package io.zonarosa.messenger.avatar.photo

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResult
import io.zonarosa.core.util.ThreadUtil
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.R
import io.zonarosa.messenger.avatar.AvatarBundler
import io.zonarosa.messenger.avatar.AvatarPickerStorage
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.providers.BlobProvider
import io.zonarosa.messenger.scribbles.ImageEditorFragment

class PhotoEditorFragment : Fragment(R.layout.avatar_photo_editor_fragment), ImageEditorFragment.Controller {

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val args = PhotoEditorActivityArgs.fromBundle(requireArguments())
    val photo = AvatarBundler.extractPhoto(args.photoAvatar)
    val imageEditorFragment = ImageEditorFragment.newInstanceForAvatarEdit(photo.uri)

    childFragmentManager.commit {
      add(R.id.fragment_container, imageEditorFragment, IMAGE_EDITOR)
    }
  }

  override fun onTouchEventsNeeded(needed: Boolean) {
  }

  override fun onRequestFullScreen(fullScreen: Boolean, hideKeyboard: Boolean) {
  }

  override fun onDoneEditing() {
    val args = PhotoEditorActivityArgs.fromBundle(requireArguments())
    val applicationContext = requireContext().applicationContext
    val imageEditorFragment: ImageEditorFragment = childFragmentManager.findFragmentByTag(IMAGE_EDITOR) as ImageEditorFragment

    ZonaRosaExecutors.BOUNDED.execute {
      val editedImageUri = imageEditorFragment.renderToSingleUseBlob()
      val size = BlobProvider.getFileSize(editedImageUri) ?: 0
      val inputStream = BlobProvider.getInstance().getStream(applicationContext, editedImageUri)
      val onDiskUri = AvatarPickerStorage.save(applicationContext, inputStream)
      val photo = AvatarBundler.extractPhoto(args.photoAvatar)
      val database = ZonaRosaDatabase.avatarPicker
      val newPhoto = photo.copy(uri = onDiskUri, size = size)

      database.update(newPhoto)
      BlobProvider.getInstance().delete(requireContext(), photo.uri)

      ThreadUtil.runOnMain {
        setFragmentResult(REQUEST_KEY_EDIT, AvatarBundler.bundlePhoto(newPhoto))
      }
    }
  }

  override fun onCancelEditing() {
    requireActivity().finishAfterTransition()
  }

  override fun restoreState() {
  }

  override fun onMainImageLoaded() {
  }

  override fun onMainImageFailedToLoad() {
  }

  companion object {
    const val REQUEST_KEY_EDIT = "io.zonarosa.messenger.avatar.photo.EDIT"

    private const val IMAGE_EDITOR = "image_editor"
  }
}
