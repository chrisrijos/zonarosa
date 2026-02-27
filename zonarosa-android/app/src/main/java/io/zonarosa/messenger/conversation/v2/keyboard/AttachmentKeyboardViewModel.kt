/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2.keyboard

import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.zonarosa.core.models.media.Media
import io.zonarosa.messenger.mediasend.MediaRepository

class AttachmentKeyboardViewModel(
  private val mediaRepository: MediaRepository = MediaRepository()
) : ViewModel() {

  private val refreshRecentMedia = BehaviorSubject.createDefault(Unit)

  fun getRecentMedia(): Observable<MutableList<Media>> {
    return refreshRecentMedia
      .flatMapSingle {
        mediaRepository
          .recentMedia
      }
      .observeOn(AndroidSchedulers.mainThread())
  }

  fun refreshRecentMedia() {
    refreshRecentMedia.onNext(Unit)
  }
}
