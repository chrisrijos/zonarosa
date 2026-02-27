package io.zonarosa.camera.demo

import android.app.Application
import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import io.zonarosa.core.util.logging.AndroidLogger
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.mms.RegisterGlideComponents
import io.zonarosa.messenger.mms.ZonaRosaGlideModule

/**
 * Application class for the camera demo.
 */
class CameraDemoApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    Log.initialize(AndroidLogger)
    ZonaRosaGlideModule.registerGlideComponents = object : RegisterGlideComponents {
      override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
      }
    }
  }
}
