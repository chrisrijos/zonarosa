package io.zonarosa.messenger.linkpreview

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Link(@JvmField val url: String, @JvmField val position: Int) : Parcelable
