/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.recipients.ui.about

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.rxSingle
import io.zonarosa.messenger.database.IdentityTable
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.groups.GroupsInCommonRepository
import io.zonarosa.messenger.groups.memberlabel.MemberLabel
import io.zonarosa.messenger.groups.memberlabel.MemberLabelRepository
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import java.util.Optional

class AboutSheetRepository {

  fun getGroupsInCommonCount(recipientId: RecipientId): Single<Int> {
    return rxSingle { GroupsInCommonRepository.getGroupsInCommonCount(recipientId) }
  }

  fun getVerified(recipientId: RecipientId): Single<Boolean> {
    return Single.fromCallable {
      val identityRecord = AppDependencies.protocolStore.aci().identities().getIdentityRecord(recipientId)
      identityRecord.isPresent && identityRecord.get().verifiedStatus == IdentityTable.VerifiedStatus.VERIFIED
    }.subscribeOn(Schedulers.io())
  }

  fun getMemberLabel(groupId: GroupId.V2): Single<Optional<MemberLabel>> = rxSingle {
    Optional.ofNullable(MemberLabelRepository.instance.getLabel(groupId, Recipient.self()))
  }

  fun canEditMemberLabel(groupId: GroupId.V2): Single<Boolean> = rxSingle {
    MemberLabelRepository.instance.canSetLabel(groupId, Recipient.self())
  }
}
