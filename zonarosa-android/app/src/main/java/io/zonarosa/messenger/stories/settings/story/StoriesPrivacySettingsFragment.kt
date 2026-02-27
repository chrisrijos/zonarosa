package io.zonarosa.messenger.stories.settings.story

import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import io.zonarosa.core.ui.BottomSheetUtil
import io.zonarosa.core.util.concurrent.LifecycleDisposable
import io.zonarosa.core.util.dp
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.DialogFragmentDisplayManager
import io.zonarosa.messenger.components.ProgressCardDialogFragment
import io.zonarosa.messenger.components.settings.DSLConfiguration
import io.zonarosa.messenger.components.settings.DSLSettingsAdapter
import io.zonarosa.messenger.components.settings.DSLSettingsFragment
import io.zonarosa.messenger.components.settings.DSLSettingsText
import io.zonarosa.messenger.components.settings.configure
import io.zonarosa.messenger.contacts.paged.ContactSearchAdapter
import io.zonarosa.messenger.contacts.paged.ContactSearchKey
import io.zonarosa.messenger.contacts.paged.ContactSearchPagedDataSourceRepository
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.mediasend.v2.stories.ChooseGroupStoryBottomSheet
import io.zonarosa.messenger.mediasend.v2.stories.ChooseStoryTypeBottomSheet
import io.zonarosa.messenger.stories.GroupStoryEducationSheet
import io.zonarosa.messenger.stories.dialogs.StoryDialogs
import io.zonarosa.messenger.stories.settings.create.CreateStoryFlowDialogFragment
import io.zonarosa.messenger.stories.settings.create.CreateStoryWithViewersFragment
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter
import io.zonarosa.messenger.util.adapter.mapping.PagingMappingAdapter
import io.zonarosa.messenger.util.navigation.safeNavigate
import io.zonarosa.core.ui.R as CoreUiR

/**
 * Allows the user to view their stories they can send to and modify settings.
 */
class StoriesPrivacySettingsFragment :
  DSLSettingsFragment(
    titleId = R.string.preferences__stories
  ),
  ChooseStoryTypeBottomSheet.Callback,
  GroupStoryEducationSheet.Callback {

  private val viewModel: StoriesPrivacySettingsViewModel by viewModels(factoryProducer = {
    StoriesPrivacySettingsViewModel.Factory(ContactSearchPagedDataSourceRepository(requireContext()))
  })

  private val lifecycleDisposable = LifecycleDisposable()
  private val progressDisplayManager = DialogFragmentDisplayManager { ProgressCardDialogFragment.create() }

  override fun createAdapters(): Array<MappingAdapter> {
    return arrayOf(DSLSettingsAdapter(), PagingMappingAdapter<ContactSearchKey>(), DSLSettingsAdapter())
  }

  override fun bindAdapters(adapter: ConcatAdapter) {
    lifecycleDisposable.bindTo(viewLifecycleOwner)

    val titleId = StoriesPrivacySettingsFragmentArgs.fromBundle(requireArguments()).titleId
    setTitle(titleId)

    val (top, middle, bottom) = adapter.adapters

    findNavController().addOnDestinationChangedListener { _, destination, _ ->
      if (destination.id == R.id.storiesPrivacySettingsFragment) {
        viewModel.pagingController.onDataInvalidated()
      }
    }

    @Suppress("UNCHECKED_CAST")
    ContactSearchAdapter.registerStoryItems(
      mappingAdapter = middle as PagingMappingAdapter<ContactSearchKey>,
      storyListener = { _, story, _ ->
        when {
          story.recipient.isMyStory -> findNavController().safeNavigate(StoriesPrivacySettingsFragmentDirections.actionStoryPrivacySettingsToMyStorySettings())
          story.recipient.isGroup -> findNavController().safeNavigate(StoriesPrivacySettingsFragmentDirections.actionStoryPrivacySettingsToGroupStorySettings(story.recipient.requireGroupId()))
          else -> findNavController().safeNavigate(StoriesPrivacySettingsFragmentDirections.actionStoryPrivacySettingsToPrivateStorySettings(story.recipient.requireDistributionListId()))
        }
      }
    )

    NewStoryItem.register(top as MappingAdapter)

    middle.setPagingController(viewModel.pagingController)

    parentFragmentManager.setFragmentResultListener(ChooseGroupStoryBottomSheet.GROUP_STORY, viewLifecycleOwner) { _, bundle ->
      val results = ChooseGroupStoryBottomSheet.ResultContract.getRecipientIds(bundle)
      viewModel.displayGroupsAsStories(results)
    }

    parentFragmentManager.setFragmentResultListener(CreateStoryWithViewersFragment.REQUEST_KEY, viewLifecycleOwner) { _, _ ->
      viewModel.pagingController.onDataInvalidated()
    }

    lifecycleDisposable += viewModel.state.subscribe { state ->
      if (state.isUpdatingEnabledState) {
        progressDisplayManager.show(viewLifecycleOwner, childFragmentManager)
      } else {
        progressDisplayManager.hide()
      }

      top.submitList(getTopConfiguration(state).toMappingModelList())
      middle.submitList(getMiddleConfiguration(state).toMappingModelList())
      (bottom as MappingAdapter).submitList(getBottomConfiguration(state).toMappingModelList())
    }
  }

  private fun getTopConfiguration(state: StoriesPrivacySettingsState): DSLConfiguration {
    return configure {
      if (state.areStoriesEnabled) {
        space(16.dp)

        noPadTextPref(
          title = DSLSettingsText.from(
            R.string.StoriesPrivacySettingsFragment__story_updates_automatically_disappear,
            DSLSettingsText.TextAppearanceModifier(CoreUiR.style.ZonaRosa_Text_BodyMedium),
            DSLSettingsText.ColorModifier(ContextCompat.getColor(requireContext(), CoreUiR.color.zonarosa_colorOnSurfaceVariant))
          )
        )

        space(20.dp)

        sectionHeaderPref(R.string.StoriesPrivacySettingsFragment__stories)

        customPref(
          NewStoryItem.Model {
            ChooseStoryTypeBottomSheet().show(childFragmentManager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG)
          }
        )
      } else {
        clickPref(
          title = DSLSettingsText.from(R.string.StoriesPrivacySettingsFragment__turn_on_stories),
          summary = DSLSettingsText.from(R.string.StoriesPrivacySettingsFragment__share_and_view),
          onClick = {
            viewModel.setStoriesEnabled(true)
          }
        )
      }
    }
  }

  private fun getMiddleConfiguration(state: StoriesPrivacySettingsState): DSLConfiguration {
    return if (state.areStoriesEnabled) {
      configure {
        ContactSearchAdapter.toMappingModelList(
          state.storyContactItems,
          emptySet(),
          null
        ).forEach {
          customPref(it)
        }
      }
    } else {
      configure { }
    }
  }

  private fun getBottomConfiguration(state: StoriesPrivacySettingsState): DSLConfiguration {
    return if (state.areStoriesEnabled) {
      configure {
        dividerPref()

        switchPref(
          title = DSLSettingsText.from(R.string.StoriesPrivacySettingsFragment__view_receipts),
          summary = DSLSettingsText.from(R.string.StoriesPrivacySettingsFragment__see_and_share),
          isChecked = state.areViewReceiptsEnabled,
          onClick = {
            viewModel.toggleViewReceipts()
          }
        )

        dividerPref()

        clickPref(
          title = DSLSettingsText.from(R.string.StoriesPrivacySettingsFragment__turn_off_stories),
          summary = DSLSettingsText.from(
            R.string.StoriesPrivacySettingsFragment__if_you_opt_out,
            DSLSettingsText.TextAppearanceModifier(CoreUiR.style.ZonaRosa_Text_BodyMedium),
            DSLSettingsText.ColorModifier(ContextCompat.getColor(requireContext(), CoreUiR.color.zonarosa_colorOnSurfaceVariant))
          ),
          onClick = {
            StoryDialogs.disableStories(requireContext(), viewModel.userHasActiveStories) {
              viewModel.setStoriesEnabled(false)
            }
          }
        )
      }
    } else {
      configure { }
    }
  }

  override fun onGroupStoryClicked() {
    if (ZonaRosaStore.story.userHasSeenGroupStoryEducationSheet) {
      onGroupStoryEducationSheetNext()
    } else {
      GroupStoryEducationSheet().show(childFragmentManager, GroupStoryEducationSheet.KEY)
    }
  }

  override fun onNewStoryClicked() {
    CreateStoryFlowDialogFragment().show(parentFragmentManager, CreateStoryWithViewersFragment.REQUEST_KEY)
  }

  override fun onGroupStoryEducationSheetNext() {
    ChooseGroupStoryBottomSheet().show(parentFragmentManager, ChooseGroupStoryBottomSheet.GROUP_STORY)
  }
}
