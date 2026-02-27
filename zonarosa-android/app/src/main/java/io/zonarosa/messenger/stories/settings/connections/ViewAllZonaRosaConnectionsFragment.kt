package io.zonarosa.messenger.stories.settings.connections

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.ViewBinderDelegate
import io.zonarosa.messenger.components.WrapperDialogFragment
import io.zonarosa.messenger.contacts.LetterHeaderDecoration
import io.zonarosa.messenger.contacts.paged.ContactSearchAdapter
import io.zonarosa.messenger.contacts.paged.ContactSearchConfiguration
import io.zonarosa.messenger.contacts.paged.ContactSearchMediator
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.databinding.ViewAllZonaRosaConnectionsFragmentBinding
import io.zonarosa.messenger.groups.SelectionLimits

class ViewAllZonaRosaConnectionsFragment : Fragment(R.layout.view_all_zonarosa_connections_fragment) {

  private val binding by ViewBinderDelegate(ViewAllZonaRosaConnectionsFragmentBinding::bind)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    binding.recycler.addItemDecoration(LetterHeaderDecoration(requireContext()) { false })
    binding.toolbar.setNavigationOnClickListener {
      requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    val mediator = ContactSearchMediator(
      fragment = this,
      selectionLimits = SelectionLimits(0, 0),
      isMultiSelect = false,
      displayOptions = ContactSearchAdapter.DisplayOptions(
        displayCheckBox = false,
        displaySecondaryInformation = ContactSearchAdapter.DisplaySecondaryInformation.NEVER
      ),
      mapStateToConfiguration = { getConfiguration() },
      performSafetyNumberChecks = false
    )

    binding.recycler.adapter = mediator.adapter
  }

  private fun getConfiguration(): ContactSearchConfiguration {
    return ContactSearchConfiguration.build {
      addSection(
        ContactSearchConfiguration.Section.Individuals(
          includeHeader = false,
          includeSelfMode = RecipientTable.IncludeSelfMode.Exclude,
          includeLetterHeaders = true,
          transportType = ContactSearchConfiguration.TransportType.PUSH
        )
      )
    }
  }

  class Dialog : WrapperDialogFragment() {
    override fun getWrappedFragment(): Fragment {
      return ViewAllZonaRosaConnectionsFragment()
    }

    companion object {
      fun show(fragmentManager: FragmentManager) {
        Dialog().show(fragmentManager, null)
      }
    }
  }
}
