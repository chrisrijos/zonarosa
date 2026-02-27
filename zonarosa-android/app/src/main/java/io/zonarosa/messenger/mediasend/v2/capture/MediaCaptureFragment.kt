package io.zonarosa.messenger.mediasend.v2.capture

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.core.Flowable
import io.zonarosa.core.models.media.Media
import io.zonarosa.core.ui.permissions.Permissions
import io.zonarosa.core.util.concurrent.LifecycleDisposable
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.settings.app.AppSettingsActivity
import io.zonarosa.messenger.mediasend.CameraFragment
import io.zonarosa.messenger.mediasend.v2.HudCommand
import io.zonarosa.messenger.mediasend.v2.MediaSelectionNavigator
import io.zonarosa.messenger.mediasend.v2.MediaSelectionViewModel
import io.zonarosa.messenger.mms.MediaConstraints
import io.zonarosa.messenger.registration.olddevice.QuickTransferOldDeviceActivity
import io.zonarosa.messenger.stories.Stories
import io.zonarosa.messenger.util.CommunicationActions
import io.zonarosa.messenger.util.navigation.safeNavigate
import java.io.FileDescriptor
import java.util.Optional
import java.util.concurrent.TimeUnit

private val TAG = Log.tag(MediaCaptureFragment::class.java)

/**
 * Fragment which displays the proper camera fragment.
 */
class MediaCaptureFragment : Fragment(R.layout.fragment_container), CameraFragment.Controller {

  private val sharedViewModel: MediaSelectionViewModel by viewModels(
    ownerProducer = { requireActivity() }
  )

  private val viewModel: MediaCaptureViewModel by viewModels(
    factoryProducer = { MediaCaptureViewModel.Factory(MediaCaptureRepository(requireContext())) }
  )

  private lateinit var captureChildFragment: CameraFragment
  private lateinit var navigator: MediaSelectionNavigator

  private val lifecycleDisposable = LifecycleDisposable()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    captureChildFragment = CameraFragment.newInstance(sharedViewModel.isContactSelectionRequired) as CameraFragment

    navigator = MediaSelectionNavigator(
      toGallery = R.id.action_mediaCaptureFragment_to_mediaGalleryFragment
    )

    childFragmentManager
      .beginTransaction()
      .replace(R.id.fragment_container, captureChildFragment as Fragment)
      .commitNowAllowingStateLoss()

    lifecycleDisposable += viewModel.events.subscribe { event ->
      when (event) {
        MediaCaptureEvent.MediaCaptureRenderFailed -> {
          Log.w(TAG, "Failed to render captured media.")
          Toast.makeText(requireContext(), R.string.MediaSendActivity_camera_unavailable, Toast.LENGTH_SHORT).show()
        }

        is MediaCaptureEvent.MediaCaptureRendered -> {
          if (isFirst()) {
            sharedViewModel.addCameraFirstCapture(event.media)
          } else {
            sharedViewModel.addMedia(event.media)
          }

          navigator.goToReview(findNavController())
        }

        is MediaCaptureEvent.UsernameScannedFromQrCode -> {
          MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.MediaCaptureFragment_username_dialog_title, event.username))
            .setMessage(getString(R.string.MediaCaptureFragment_username_dialog_body, event.username))
            .setPositiveButton(R.string.MediaCaptureFragment_username_dialog_go_to_chat_button) { d, _ ->
              CommunicationActions.startConversation(requireContext(), event.recipient, "")
              requireActivity().finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        }

        is MediaCaptureEvent.DeviceLinkScannedFromQrCode -> {
          MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.MediaCaptureFragment_device_link_dialog_title)
            .setMessage(R.string.MediaCaptureFragment_it_looks_like_youre_trying)
            .setPositiveButton(R.string.MediaCaptureFragment_device_link_dialog_continue) { d, _ ->
              startActivity(AppSettingsActivity.linkedDevices(requireContext()))
              requireActivity().finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        }

        is MediaCaptureEvent.ReregistrationScannedFromQrCode -> {
          startActivity(QuickTransferOldDeviceActivity.intent(requireContext(), event.data))
          requireActivity().finish()
        }
      }
    }

    sharedViewModel.state.observe(viewLifecycleOwner) { state ->
      captureChildFragment.presentHud(state.selectedMedia.size)
    }

    lifecycleDisposable.bindTo(viewLifecycleOwner)
    lifecycleDisposable += sharedViewModel.hudCommands.subscribe { command ->
      if (command == HudCommand.GoToText) {
        findNavController().safeNavigate(R.id.action_mediaCaptureFragment_to_textStoryPostCreationFragment)
      }
    }

    if (isFirst() || sharedViewModel.isSelectedMediaEmpty()) {
      requireActivity().onBackPressedDispatcher.addCallback(
        viewLifecycleOwner,
        object : OnBackPressedCallback(true) {
          override fun handleOnBackPressed() {
            requireActivity().finish()
          }
        }
      )
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
  }

  override fun onResume() {
    super.onResume()
    captureChildFragment.fadeInControls()
  }

  override fun onCameraError() {
    Log.w(TAG, "Camera Error.")

    val context = this.context
    if (context != null) {
      Toast.makeText(context, R.string.MediaSendActivity_camera_unavailable, Toast.LENGTH_SHORT).show()
    } else {
      Log.w(TAG, "Could not post toast, fragment not attached to a context.")
    }
  }

  override fun onImageCaptured(data: ByteArray, width: Int, height: Int) {
    viewModel.onImageCaptured(data, width, height)
  }

  override fun onVideoCaptured(fd: FileDescriptor) {
    viewModel.onVideoCaptured(fd)
  }

  override fun onVideoCaptureError() {
    Log.w(TAG, "Video capture error.")
    context?.let { context ->
      Toast.makeText(context, R.string.MediaSendActivity_camera_unavailable, Toast.LENGTH_SHORT).show()
    }
  }

  override fun onGalleryClicked() {
    val controller = findNavController()
    captureChildFragment.fadeOutControls {
      navigator.goToGallery(controller)
    }
  }

  override fun onCameraCountButtonClicked() {
    val controller = findNavController()
    captureChildFragment.fadeOutControls {
      navigator.goToReview(controller)
    }
  }

  override fun onQrCodeFound(data: String) {
    viewModel.onQrCodeFound(data)
  }

  override fun getMostRecentMediaItem(): Flowable<Optional<Media>> {
    return viewModel.getMostRecentMedia()
  }

  override fun getMediaConstraints(): MediaConstraints {
    return sharedViewModel.getMediaConstraints()
  }

  override fun getMaxVideoDuration(): Int {
    return if (sharedViewModel.isStory()) TimeUnit.MILLISECONDS.toSeconds(Stories.MAX_VIDEO_DURATION_MILLIS).toInt() else -1
  }

  private fun isFirst(): Boolean {
    return arguments?.getBoolean("first") == true
  }

  companion object {
    const val CAPTURE_RESULT = "capture_result"
    const val CAPTURE_RESULT_OK = "capture_result_ok"
  }
}
