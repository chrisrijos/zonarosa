package io.zonarosa.messenger.contacts.management

import android.content.Context
import androidx.annotation.CheckResult
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.RotateProfileKeyJob
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientUtil

class ContactsManagementRepository(context: Context) {
  private val context = context.applicationContext

  @CheckResult
  fun blockContact(recipient: Recipient): Completable {
    return Completable.fromAction {
      if (recipient.isDistributionList) {
        error("Blocking a distribution list makes no sense")
      } else if (recipient.isGroup) {
        RecipientUtil.block(context, recipient)
      } else {
        RecipientUtil.blockNonGroup(context, recipient)
      }
    }.subscribeOn(Schedulers.io())
  }

  @CheckResult
  fun hideContact(recipient: Recipient): Completable {
    return Completable.fromAction {
      if (recipient.isGroup || recipient.isDistributionList || recipient.isSelf) {
        error("Cannot hide groups, self, or distribution lists.")
      }

      val rotateProfileKey = !recipient.hasGroupsInCommon
      ZonaRosaDatabase.recipients.markHidden(recipient.id, rotateProfileKey, false)
      if (rotateProfileKey) {
        AppDependencies.jobManager.add(RotateProfileKeyJob())
      }
    }.subscribeOn(Schedulers.io())
  }
}
