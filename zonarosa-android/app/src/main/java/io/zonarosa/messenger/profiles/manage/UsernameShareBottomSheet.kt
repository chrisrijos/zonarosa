package io.zonarosa.messenger.profiles.manage

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.DimensionUnit
import io.zonarosa.core.util.Util
import io.zonarosa.core.util.concurrent.LifecycleDisposable
import io.zonarosa.libzonarosa.usernames.Username
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.settings.DSLConfiguration
import io.zonarosa.messenger.components.settings.DSLSettingsAdapter
import io.zonarosa.messenger.components.settings.DSLSettingsBottomSheetFragment
import io.zonarosa.messenger.components.settings.DSLSettingsText
import io.zonarosa.messenger.components.settings.configure
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.util.FragmentResultContract
import io.zonarosa.core.ui.R as CoreUiR

/**
 * Allows the user to either share their username directly or to copy it to their clipboard.
 */
class UsernameShareBottomSheet : DSLSettingsBottomSheetFragment() {

  companion object {
    private const val REQUEST_KEY = "copy_username"
  }

  private val lifecycleDisposable = LifecycleDisposable()

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    CopyButton.register(adapter)
    ShareButton.register(adapter)

    lifecycleDisposable += Recipient.observable(Recipient.self().id).subscribe {
      if (context == null) {
        return@subscribe
      }

      adapter.submitList(getConfiguration(it).toMappingModelList())
    }
  }

  private fun getConfiguration(recipient: Recipient): DSLConfiguration {
    return configure {
      noPadTextPref(
        title = DSLSettingsText.from(
          R.string.UsernameShareBottomSheet__copy_or_share_a_username_link,
          DSLSettingsText.TextAppearanceModifier(CoreUiR.style.ZonaRosa_Text_BodyMedium),
          DSLSettingsText.CenterModifier,
          DSLSettingsText.ColorModifier(
            ContextCompat.getColor(requireContext(), CoreUiR.color.zonarosa_colorOnSurfaceVariant)
          )
        )
      )

      space(DimensionUnit.DP.toPixels(32f).toInt())

      val username = recipient.username.get()
      customPref(
        CopyButton.Model(
          text = username,
          onClick = {
            copyToClipboard(it)
          }
        )
      )

      space(DimensionUnit.DP.toPixels(20f).toInt())

      customPref(
        CopyButton.Model(
          text = getString(R.string.zonarosa_me_username_url, Base64.encodeUrlSafeWithoutPadding(Username(username).hash)),
          onClick = {
            copyToClipboard(it)
          }
        )
      )

      space(DimensionUnit.DP.toPixels(24f).toInt())

      customPref(
        ShareButton.Model(
          text = getString(R.string.zonarosa_me_username_url, Base64.encodeUrlSafeWithoutPadding(Username(username).hash)),
          onClick = {
            openShareSheet(it.text)
          }
        )
      )

      space(DimensionUnit.DP.toPixels(18f).toInt())
    }
  }

  private fun copyToClipboard(model: CopyButton.Model) {
    Util.copyToClipboard(requireContext(), model.text)
    setFragmentResult(REQUEST_KEY, Bundle().apply { putBoolean(REQUEST_KEY, true) })
    findNavController().popBackStack()
  }

  private fun openShareSheet(charSequence: CharSequence) {
    val mimeType = Intent.normalizeMimeType("text/plain")
    val shareIntent = ShareCompat.IntentBuilder(requireContext())
      .setText(charSequence)
      .setType(mimeType)
      .createChooserIntent()
      .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    try {
      startActivity(shareIntent)
    } catch (e: ActivityNotFoundException) {
      Toast.makeText(requireContext(), R.string.MediaPreviewActivity_cant_find_an_app_able_to_share_this_media, Toast.LENGTH_LONG).show()
    }
  }

  object ResultContract : FragmentResultContract<Boolean>(REQUEST_KEY) {
    override fun getResult(bundle: Bundle): Boolean {
      return bundle.getBoolean(REQUEST_KEY, false)
    }
  }
}
