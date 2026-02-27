package io.zonarosa.messenger.delete

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import io.zonarosa.core.ui.compose.ComposeFragment
import io.zonarosa.messenger.R
import io.zonarosa.messenger.registration.ui.countrycode.CountryCodeSelectScreen
import io.zonarosa.messenger.registration.ui.countrycode.CountryCodeViewModel

/**
 * Country code picker specific to deleting an account.
 */
class DeleteAccountCountryCodeFragment : ComposeFragment() {

  companion object {
    const val RESULT_KEY = "result_key"
    const val RESULT_COUNTRY = "result_country"
  }

  private val viewModel: CountryCodeViewModel by viewModels()

  @Composable
  override fun FragmentContent() {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CountryCodeSelectScreen(
      state = state,
      title = stringResource(R.string.CountryCodeFragment__your_country),
      onSearch = { search -> viewModel.filterCountries(search) },
      onDismissed = { findNavController().popBackStack() },
      onClick = { country ->
        setFragmentResult(RESULT_KEY, bundleOf(RESULT_COUNTRY to country))
        findNavController().popBackStack()
      }
    )
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.loadCountries()
  }
}
