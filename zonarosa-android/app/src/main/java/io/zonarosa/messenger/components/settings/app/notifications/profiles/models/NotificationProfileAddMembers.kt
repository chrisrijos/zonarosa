package io.zonarosa.messenger.components.settings.app.notifications.profiles.models

import android.view.View
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.settings.DSLSettingsIcon
import io.zonarosa.messenger.components.settings.DSLSettingsText
import io.zonarosa.messenger.components.settings.NO_TINT
import io.zonarosa.messenger.components.settings.PreferenceModel
import io.zonarosa.messenger.components.settings.PreferenceViewHolder
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.util.adapter.mapping.LayoutFactory
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter

/**
 * Custom DSL preference for adding members to a profile.
 */
object NotificationProfileAddMembers {

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, LayoutFactory(::ViewHolder, R.layout.large_icon_preference_item))
  }

  class Model(
    override val title: DSLSettingsText = DSLSettingsText.from(R.string.AddAllowedMembers__add_people_or_groups),
    override val icon: DSLSettingsIcon = DSLSettingsIcon.from(R.drawable.add_to_a_group, NO_TINT),
    val onClick: (Long, Set<RecipientId>) -> Unit,
    val profileId: Long,
    val currentSelection: Set<RecipientId>
  ) : PreferenceModel<Model>() {
    override fun areContentsTheSame(newItem: Model): Boolean {
      return super.areContentsTheSame(newItem) && profileId == newItem.profileId && currentSelection == newItem.currentSelection
    }
  }

  private class ViewHolder(itemView: View) : PreferenceViewHolder<Model>(itemView) {
    override fun bind(model: Model) {
      super.bind(model)
      itemView.setOnClickListener { model.onClick(model.profileId, model.currentSelection) }
    }
  }
}
