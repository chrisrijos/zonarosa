package io.zonarosa.messenger.calls.log

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.withinTransaction
import io.zonarosa.messenger.calls.links.UpdateCallLinkRepository
import io.zonarosa.messenger.database.CallLinkTable
import io.zonarosa.messenger.database.DatabaseObserver
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.CallLogEventSendJob
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.messenger.service.webrtc.links.UpdateCallLinkResult

class CallLogRepository(
  private val updateCallLinkRepository: UpdateCallLinkRepository = UpdateCallLinkRepository(),
  private val callLogPeekHelper: CallLogPeekHelper,
  private val callEventCache: CallEventCache
) : CallLogPagedDataSource.CallRepository {

  companion object {
    fun listenForCallTableChanges(): Observable<Unit> {
      return Observable.create { emitter ->
        fun refresh() {
          emitter.onNext(Unit)
        }

        val databaseObserver = DatabaseObserver.Observer {
          refresh()
        }

        AppDependencies.databaseObserver.registerCallUpdateObserver(databaseObserver)

        emitter.setCancellable {
          AppDependencies.databaseObserver.unregisterObserver(databaseObserver)
        }
      }
    }
  }

  override fun getCallsCount(query: String?, filter: CallLogFilter): Int {
    return callEventCache.getCallEventsCount(CallEventCache.FilterState(query ?: "", filter))
  }

  override fun getCalls(query: String?, filter: CallLogFilter, start: Int, length: Int): List<CallLogRow> {
    return callEventCache.getCallEvents(CallEventCache.FilterState(query ?: "", filter), length, start)
  }

  override fun getCallLinksCount(query: String?, filter: CallLogFilter): Int {
    return when (filter) {
      CallLogFilter.MISSED -> 0
      CallLogFilter.ALL, CallLogFilter.AD_HOC -> ZonaRosaDatabase.callLinks.getCallLinksCount(query)
    }
  }

  override fun getCallLinks(query: String?, filter: CallLogFilter, start: Int, length: Int): List<CallLogRow> {
    return when (filter) {
      CallLogFilter.MISSED -> emptyList()
      CallLogFilter.ALL, CallLogFilter.AD_HOC -> ZonaRosaDatabase.callLinks.getCallLinks(query, start, length)
    }
  }

  override fun onCallTabPageLoaded(pageData: List<CallLogRow>) {
    ZonaRosaExecutors.BOUNDED_IO.execute {
      callLogPeekHelper.onPageLoaded(pageData)
    }
  }

  fun listenForChanges(): Observable<Unit> {
    return callEventCache.listenForChanges()
  }

  fun markAllCallEventsRead() {
    ZonaRosaExecutors.BOUNDED_IO.execute {
      val latestCall = ZonaRosaDatabase.calls.getLatestCall() ?: return@execute
      ZonaRosaDatabase.calls.markAllCallEventsRead()
      AppDependencies.jobManager.add(CallLogEventSendJob.forMarkedAsRead(latestCall))
    }
  }

  fun deleteSelectedCallLogs(
    selectedCallRowIds: Set<Long>
  ): Completable {
    return Completable.fromAction {
      ZonaRosaDatabase.calls.deleteNonAdHocCallEvents(selectedCallRowIds)
    }.subscribeOn(Schedulers.io())
  }

  fun deleteAllCallLogsExcept(
    selectedCallRowIds: Set<Long>,
    missedOnly: Boolean
  ): Completable {
    return Completable.fromAction {
      ZonaRosaDatabase.calls.deleteAllNonAdHocCallEventsExcept(selectedCallRowIds, missedOnly)
    }.subscribeOn(Schedulers.io())
  }

  /**
   * Delete all call events / unowned links and enqueue clear history job, and then
   * emit a clear history message.
   *
   * This explicitly drops failed call link revocations of call links, and those call links
   * will remain visible to the user. This is safe because the clear history sync message should
   * only clear local history and then poll link status from the server.
   */
  fun deleteAllCallLogsOnOrBeforeNow(): Single<Int> {
    return Single.fromCallable {
      ZonaRosaDatabase.rawDatabase.withinTransaction {
        val latestCall = ZonaRosaDatabase.calls.getLatestCall() ?: return@withinTransaction
        ZonaRosaDatabase.calls.deleteNonAdHocCallEventsOnOrBefore(latestCall.timestamp)
        ZonaRosaDatabase.callLinks.deleteNonAdminCallLinksOnOrBefore(latestCall.timestamp)
        AppDependencies.jobManager.add(CallLogEventSendJob.forClearHistory(latestCall))
      }

      ZonaRosaDatabase.callLinks.getAllAdminCallLinksExcept(emptySet())
    }.flatMap(this::deleteAndCollectResults).map { 0 }.subscribeOn(Schedulers.io())
  }

  /**
   * Deletes the selected call links. We DELETE those links we don't have admin keys for,
   * and revoke the ones we *do* have admin keys for. We then perform a cleanup step on
   * terminate to clean up call events.
   */
  fun deleteSelectedCallLinks(
    selectedCallRowIds: Set<Long>,
    selectedRoomIds: Set<CallLinkRoomId>
  ): Single<Int> {
    return Single.fromCallable {
      val allCallLinkIds = ZonaRosaDatabase.calls.getCallLinkRoomIdsFromCallRowIds(selectedCallRowIds) + selectedRoomIds
      ZonaRosaDatabase.callLinks.deleteNonAdminCallLinks(allCallLinkIds)
      ZonaRosaDatabase.callLinks.getAdminCallLinks(allCallLinkIds)
    }.flatMap(this::deleteAndCollectResults).subscribeOn(Schedulers.io())
  }

  /**
   * Deletes all but the selected call links. We DELETE those links we don't have admin keys for,
   * and revoke the ones we *do* have admin keys for. We then perform a cleanup step on
   * terminate to clean up call events.
   */
  fun deleteAllCallLinksExcept(
    selectedCallRowIds: Set<Long>,
    selectedRoomIds: Set<CallLinkRoomId>
  ): Single<Int> {
    return Single.fromCallable {
      val allCallLinkIds = ZonaRosaDatabase.calls.getCallLinkRoomIdsFromCallRowIds(selectedCallRowIds) + selectedRoomIds
      ZonaRosaDatabase.callLinks.deleteAllNonAdminCallLinksExcept(allCallLinkIds)
      ZonaRosaDatabase.callLinks.getAllAdminCallLinksExcept(allCallLinkIds)
    }.flatMap(this::deleteAndCollectResults).subscribeOn(Schedulers.io())
  }

  private fun deleteAndCollectResults(callLinksToRevoke: Set<CallLinkTable.CallLink>): Single<Int> {
    return Single.merge(
      callLinksToRevoke.map {
        updateCallLinkRepository.deleteCallLink(it.credentials!!)
      }
    ).reduce(0) { acc, current ->
      acc + (if (current is UpdateCallLinkResult.Delete) 0 else 1)
    }.doOnTerminate {
      ZonaRosaDatabase.calls.updateAdHocCallEventDeletionTimestamps()
    }.doOnDispose {
      ZonaRosaDatabase.calls.updateAdHocCallEventDeletionTimestamps()
    }
  }
}
