package io.zonarosa.messenger.components.settings.app.subscription.receipts.detail

import android.content.Intent
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.ZonaRosaProgressDialog
import io.zonarosa.messenger.components.settings.DSLConfiguration
import io.zonarosa.messenger.components.settings.DSLSettingsFragment
import io.zonarosa.messenger.components.settings.DSLSettingsText
import io.zonarosa.messenger.components.settings.app.subscription.InAppDonations
import io.zonarosa.messenger.components.settings.app.subscription.receipts.ReceiptImageRenderer
import io.zonarosa.messenger.components.settings.configure
import io.zonarosa.messenger.components.settings.models.SplashImage
import io.zonarosa.messenger.database.model.InAppPaymentReceiptRecord
import io.zonarosa.messenger.payments.FiatMoneyUtil
import io.zonarosa.messenger.util.DateUtils
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter
import java.util.Locale
import io.zonarosa.core.ui.R as CoreUiR

class DonationReceiptDetailFragment : DSLSettingsFragment(layoutId = R.layout.donation_receipt_detail_fragment) {

  private lateinit var progressDialog: ZonaRosaProgressDialog

  private val viewModel: DonationReceiptDetailViewModel by viewModels(
    factoryProducer = {
      DonationReceiptDetailViewModel.Factory(
        DonationReceiptDetailFragmentArgs.fromBundle(requireArguments()).id,
        DonationReceiptDetailRepository()
      )
    }
  )

  override fun bindAdapter(adapter: MappingAdapter) {
    SplashImage.register(adapter)

    val sharePngButton: MaterialButton = requireView().findViewById(R.id.share_png)
    sharePngButton.isEnabled = false

    viewModel.state.observe(viewLifecycleOwner) { state ->
      if (state.inAppPaymentReceiptRecord != null) {
        val subscriptionName = InAppDonations.resolveLabel(requireContext(), state.inAppPaymentReceiptRecord)
        adapter.submitList(getConfiguration(state.inAppPaymentReceiptRecord, subscriptionName).toMappingModelList())

        sharePngButton.isEnabled = true
        sharePngButton.setOnClickListener {
          progressDialog = ZonaRosaProgressDialog.show(requireContext())
          ReceiptImageRenderer.renderPng(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            record = state.inAppPaymentReceiptRecord,
            subscriptionName = InAppDonations.resolveLabel(requireContext(), state.inAppPaymentReceiptRecord),
            callback = object : ReceiptImageRenderer.Callback {
              override fun onBitmapRendered() {
                progressDialog.dismiss()
              }

              override fun onStartActivity(intent: Intent) {
                startActivity(intent)
              }
            }
          )
        }
      }
    }
  }

  private fun getConfiguration(record: InAppPaymentReceiptRecord, subscriptionName: String?): DSLConfiguration {
    return configure {
      customPref(
        SplashImage.Model(
          splashImageResId = R.drawable.ic_zonarosa_logo_type
        )
      )

      textPref(
        title = DSLSettingsText.from(
          charSequence = FiatMoneyUtil.format(resources, record.amount),
          DSLSettingsText.TextAppearanceModifier(CoreUiR.style.ZonaRosa_Text_Giant),
          DSLSettingsText.CenterModifier
        )
      )

      dividerPref()

      textPref(
        title = DSLSettingsText.from(R.string.DonationReceiptDetailsFragment__donation_type),
        summary = DSLSettingsText.from(
          when (record.type) {
            InAppPaymentReceiptRecord.Type.RECURRING_DONATION -> getString(R.string.DonationReceiptListFragment__recurring)
            InAppPaymentReceiptRecord.Type.ONE_TIME_DONATION -> getString(R.string.DonationReceiptListFragment__one_time)
            InAppPaymentReceiptRecord.Type.ONE_TIME_GIFT -> getString(R.string.DonationReceiptListFragment__donation_for_a_friend)
            InAppPaymentReceiptRecord.Type.RECURRING_BACKUP -> error("Not supported in this fragment.")
          }
        )
      )

      textPref(
        title = DSLSettingsText.from(R.string.DonationReceiptDetailsFragment__date_paid),
        summary = record.let { DSLSettingsText.from(DateUtils.formatDateWithYear(Locale.getDefault(), it.timestamp)) }
      )
    }
  }
}
