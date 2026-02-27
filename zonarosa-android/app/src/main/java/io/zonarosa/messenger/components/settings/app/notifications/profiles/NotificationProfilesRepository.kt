package io.zonarosa.messenger.components.settings.app.notifications.profiles

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.ObservableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.messenger.conversation.colors.AvatarColor
import io.zonarosa.messenger.database.DatabaseObserver
import io.zonarosa.messenger.database.NotificationProfileTables
import io.zonarosa.messenger.database.RxDatabaseObserver
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.notifications.profiles.NotificationProfile
import io.zonarosa.messenger.notifications.profiles.NotificationProfileSchedule
import io.zonarosa.messenger.notifications.profiles.NotificationProfiles
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.util.toLocalDateTime
import io.zonarosa.messenger.util.toMillis

/**
 * One stop shop for all your Notification Profile data needs.
 */
class NotificationProfilesRepository {
  private val database: NotificationProfileTables = ZonaRosaDatabase.notificationProfiles

  fun getProfiles(): Flowable<List<NotificationProfile>> {
    return RxDatabaseObserver
      .notificationProfiles
      .map { database.getProfiles() }
      .subscribeOn(Schedulers.io())
  }

  fun getProfile(profileId: Long): Observable<NotificationProfile> {
    return Observable.create { emitter: ObservableEmitter<NotificationProfile> ->
      val emitProfile: () -> Unit = {
        val profile: NotificationProfile? = database.getProfile(profileId)
        if (profile != null) {
          emitter.onNext(profile)
        } else {
          emitter.onError(NotificationProfileNotFoundException())
        }
      }

      val databaseObserver: DatabaseObserver = AppDependencies.databaseObserver
      val profileObserver = DatabaseObserver.Observer { emitProfile() }

      databaseObserver.registerNotificationProfileObserver(profileObserver)

      emitter.setCancellable { databaseObserver.unregisterObserver(profileObserver) }
      emitProfile()
    }.subscribeOn(Schedulers.io())
  }

  fun createProfile(name: String, selectedEmoji: String): Single<NotificationProfileTables.NotificationProfileChangeResult> {
    return Single.fromCallable { database.createProfile(name = name, emoji = selectedEmoji, color = AvatarColor.random(), createdAt = System.currentTimeMillis()) }
      .doOnSuccess { StorageSyncHelper.scheduleSyncForDataChange() }
      .subscribeOn(Schedulers.io())
  }

  fun updateProfile(profileId: Long, name: String, selectedEmoji: String): Single<NotificationProfileTables.NotificationProfileChangeResult> {
    return Single.fromCallable { database.updateProfile(profileId = profileId, name = name, emoji = selectedEmoji) }
      .doOnSuccess { scheduleNotificationProfileSync(profileId) }
      .subscribeOn(Schedulers.io())
  }

  fun updateProfile(profile: NotificationProfile): Single<NotificationProfileTables.NotificationProfileChangeResult> {
    return Single.fromCallable { database.updateProfile(profile) }
      .doOnSuccess { scheduleNotificationProfileSync(profile.id) }
      .subscribeOn(Schedulers.io())
  }

  fun updateAllowedMembers(profileId: Long, recipients: Set<RecipientId>): Single<NotificationProfile> {
    return Single.fromCallable { database.setAllowedRecipients(profileId, recipients) }
      .doOnSuccess { scheduleNotificationProfileSync(profileId) }
      .subscribeOn(Schedulers.io())
  }

  fun removeMember(profileId: Long, recipientId: RecipientId): Single<NotificationProfile> {
    return Single.fromCallable { database.removeAllowedRecipient(profileId, recipientId) }
      .doOnSuccess { scheduleNotificationProfileSync(profileId) }
      .subscribeOn(Schedulers.io())
  }

  fun addMember(profileId: Long, recipientId: RecipientId): Single<NotificationProfile> {
    return Single.fromCallable { database.addAllowedRecipient(profileId, recipientId) }
      .doOnSuccess { scheduleNotificationProfileSync(profileId) }
      .subscribeOn(Schedulers.io())
  }

  fun deleteProfile(profileId: Long): Completable {
    return Completable.fromCallable { database.deleteProfile(profileId) }
      .doOnComplete { scheduleNotificationProfileSync(profileId) }
      .subscribeOn(Schedulers.io())
  }

  fun updateSchedule(schedule: NotificationProfileSchedule): Completable {
    return Completable.fromCallable { database.updateSchedule(schedule) }
      .subscribeOn(Schedulers.io())
  }

  fun toggleAllowAllMentions(profileId: Long): Single<NotificationProfile> {
    return getProfile(profileId)
      .take(1)
      .singleOrError()
      .flatMap { updateProfile(it.copy(allowAllMentions = !it.allowAllMentions)) }
      .map { (it as NotificationProfileTables.NotificationProfileChangeResult.Success).notificationProfile }
  }

  fun toggleAllowAllCalls(profileId: Long): Single<NotificationProfile> {
    return getProfile(profileId)
      .take(1)
      .singleOrError()
      .flatMap { updateProfile(it.copy(allowAllCalls = !it.allowAllCalls)) }
      .map { (it as NotificationProfileTables.NotificationProfileChangeResult.Success).notificationProfile }
  }

  fun manuallyToggleProfile(profile: NotificationProfile, now: Long = System.currentTimeMillis()): Completable {
    return manuallyToggleProfile(profile.id, profile.schedule, now)
  }

  fun manuallyToggleProfile(profileId: Long, schedule: NotificationProfileSchedule, now: Long = System.currentTimeMillis()): Completable {
    return Completable.fromAction {
      val profiles = database.getProfiles()
      val activeProfile = NotificationProfiles.getActiveProfile(profiles, now)

      if (profileId == activeProfile?.id) {
        ZonaRosaStore.notificationProfile.manuallyEnabledProfile = 0
        ZonaRosaStore.notificationProfile.manuallyEnabledUntil = 0
        ZonaRosaStore.notificationProfile.manuallyDisabledAt = now
        ZonaRosaStore.notificationProfile.lastProfilePopup = 0
        ZonaRosaStore.notificationProfile.lastProfilePopupTime = 0
      } else {
        val inScheduledWindow = schedule.isCurrentlyActive(now)
        ZonaRosaStore.notificationProfile.manuallyEnabledProfile = profileId
        ZonaRosaStore.notificationProfile.manuallyEnabledUntil = if (inScheduledWindow) schedule.endDateTime(now.toLocalDateTime()).toMillis() else Long.MAX_VALUE
        ZonaRosaStore.notificationProfile.manuallyDisabledAt = now
      }
    }
      .doOnComplete {
        scheduleManualOverrideSync()
        AppDependencies.databaseObserver.notifyNotificationProfileObservers()
      }
      .subscribeOn(Schedulers.io())
  }

  fun manuallyEnableProfileForDuration(profileId: Long, enableUntil: Long, now: Long = System.currentTimeMillis()): Completable {
    return Completable.fromAction {
      ZonaRosaStore.notificationProfile.manuallyEnabledProfile = profileId
      ZonaRosaStore.notificationProfile.manuallyEnabledUntil = enableUntil
      ZonaRosaStore.notificationProfile.manuallyDisabledAt = now
    }
      .doOnComplete {
        scheduleManualOverrideSync()
        AppDependencies.databaseObserver.notifyNotificationProfileObservers()
      }
      .subscribeOn(Schedulers.io())
  }

  fun manuallyEnableProfileForSchedule(profileId: Long, schedule: NotificationProfileSchedule, now: Long = System.currentTimeMillis()): Completable {
    return Completable.fromAction {
      val inScheduledWindow = schedule.isCurrentlyActive(now)
      ZonaRosaStore.notificationProfile.manuallyEnabledProfile = if (inScheduledWindow) profileId else 0
      ZonaRosaStore.notificationProfile.manuallyEnabledUntil = if (inScheduledWindow) schedule.endDateTime(now.toLocalDateTime()).toMillis() else Long.MAX_VALUE
      ZonaRosaStore.notificationProfile.manuallyDisabledAt = if (inScheduledWindow) now else 0
    }
      .doOnComplete {
        scheduleManualOverrideSync()
        AppDependencies.databaseObserver.notifyNotificationProfileObservers()
      }
      .subscribeOn(Schedulers.io())
  }

  /**
   * Schedules a sync for a notification profile when it changes
   */
  fun scheduleNotificationProfileSync(profileId: Long) {
    ZonaRosaDatabase.notificationProfiles.markNeedsSync(profileId)
    StorageSyncHelper.scheduleSyncForDataChange()
  }

  /**
   * Schedules a sync for the self when the manual notification profile changes
   */
  private fun scheduleManualOverrideSync() {
    ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
    StorageSyncHelper.scheduleSyncForDataChange()
  }

  class NotificationProfileNotFoundException : Throwable()
}
