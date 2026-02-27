package io.zonarosa.messenger.mediasend.v2

import android.view.animation.Interpolator
import io.zonarosa.messenger.util.createDefaultCubicBezierInterpolator

object MediaAnimations {
  /**
   * Fast-In-Extra-Slow-Out Interpolator
   */
  @JvmStatic
  val interpolator: Interpolator = createDefaultCubicBezierInterpolator()
}
