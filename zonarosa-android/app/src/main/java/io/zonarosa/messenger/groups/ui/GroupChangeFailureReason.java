package io.zonarosa.messenger.groups.ui;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.groups.GroupChangeBusyException;
import io.zonarosa.messenger.groups.GroupInsufficientRightsException;
import io.zonarosa.messenger.groups.GroupNotAMemberException;
import io.zonarosa.messenger.groups.MembershipNotSuitableForV2Exception;

import java.io.IOException;

public enum GroupChangeFailureReason {
  NO_RIGHTS,
  NOT_GV2_CAPABLE,
  NOT_ANNOUNCEMENT_CAPABLE,
  NOT_A_MEMBER,
  BUSY,
  NETWORK,
  OTHER;

  @SuppressLint("SuspiciousIndentation")
  public static @NonNull GroupChangeFailureReason fromException(@NonNull Throwable e) {
    if (e instanceof MembershipNotSuitableForV2Exception) return GroupChangeFailureReason.NOT_GV2_CAPABLE;
    if (e instanceof IOException)                         return GroupChangeFailureReason.NETWORK;
    if (e instanceof GroupNotAMemberException)            return GroupChangeFailureReason.NOT_A_MEMBER;
    if (e instanceof GroupChangeBusyException)            return GroupChangeFailureReason.BUSY;
    if (e instanceof GroupInsufficientRightsException)    return GroupChangeFailureReason.NO_RIGHTS;
                                                          return GroupChangeFailureReason.OTHER;
  }
}
