package io.zonarosa.messenger.components.settings.conversation.preferences

import android.view.View
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.settings.PreferenceModel
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.util.adapter.mapping.LayoutFactory
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter
import io.zonarosa.messenger.util.adapter.mapping.MappingViewHolder

object InternalPreference {

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.conversation_settings_internal_preference))
  }

  class Model(
    private val recipient: Recipient,
    val onInternalDetailsClicked: () -> Unit
  ) : PreferenceModel<Model>() {

    override fun areItemsTheSame(newItem: Model): Boolean {
      return recipient == newItem.recipient
    }
  }

  private class ViewHolder(itemView: View) : MappingViewHolder<Model>(itemView) {

    private val internalDetails: View = itemView.findViewById(R.id.internal_details)

    override fun bind(model: Model) {
      internalDetails.setOnClickListener { model.onInternalDetailsClicked() }
    }
  }
}
