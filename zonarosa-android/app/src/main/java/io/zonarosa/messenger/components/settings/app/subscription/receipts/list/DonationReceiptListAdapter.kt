package io.zonarosa.messenger.components.settings.app.subscription.receipts.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.settings.DSLSettingsText
import io.zonarosa.messenger.components.settings.SectionHeaderPreference
import io.zonarosa.messenger.components.settings.SectionHeaderPreferenceViewHolder
import io.zonarosa.messenger.components.settings.TextPreference
import io.zonarosa.messenger.components.settings.TextPreferenceViewHolder
import io.zonarosa.messenger.util.StickyHeaderDecoration
import io.zonarosa.messenger.util.adapter.mapping.LayoutFactory
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter
import io.zonarosa.messenger.util.toLocalDateTime
import io.zonarosa.core.ui.R as CoreUiR

class DonationReceiptListAdapter(onModelClick: (DonationReceiptListItem.Model) -> Unit) : MappingAdapter(), StickyHeaderDecoration.StickyHeaderAdapter<SectionHeaderPreferenceViewHolder> {

  init {
    registerFactory(TextPreference::class.java, LayoutFactory({ TextPreferenceViewHolder(it) }, R.layout.dsl_preference_item))
    DonationReceiptListItem.register(this, onModelClick)
  }

  override fun getHeaderId(position: Int): Long {
    return when (val item = getItem(position)) {
      is DonationReceiptListItem.Model -> item.record.timestamp.toLocalDateTime().year.toLong()
      else -> StickyHeaderDecoration.StickyHeaderAdapter.NO_HEADER_ID
    }
  }

  override fun onCreateHeaderViewHolder(parent: ViewGroup?, position: Int, type: Int): SectionHeaderPreferenceViewHolder {
    return SectionHeaderPreferenceViewHolder(LayoutInflater.from(parent!!.context).inflate(R.layout.dsl_section_header, parent, false))
  }

  override fun onBindHeaderViewHolder(viewHolder: SectionHeaderPreferenceViewHolder?, position: Int, type: Int) {
    viewHolder?.itemView?.run {
      val color = ContextCompat.getColor(context, CoreUiR.color.zonarosa_colorBackground)
      setBackgroundColor(color)
    }

    viewHolder?.bind(SectionHeaderPreference(DSLSettingsText.from(getHeaderId(position).toString())))
  }
}
