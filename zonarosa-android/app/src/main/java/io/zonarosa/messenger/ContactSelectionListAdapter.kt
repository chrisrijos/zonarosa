package io.zonarosa.messenger

import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import io.zonarosa.messenger.contacts.paged.ContactSearchAdapter
import io.zonarosa.messenger.contacts.paged.ContactSearchConfiguration
import io.zonarosa.messenger.contacts.paged.ContactSearchData
import io.zonarosa.messenger.contacts.paged.ContactSearchKey
import io.zonarosa.messenger.util.adapter.mapping.LayoutFactory
import io.zonarosa.messenger.util.adapter.mapping.MappingModel
import io.zonarosa.messenger.util.adapter.mapping.MappingViewHolder

class ContactSelectionListAdapter(
  context: Context,
  fixedContacts: Set<ContactSearchKey>,
  displayOptions: DisplayOptions,
  onClickCallbacks: OnContactSelectionClick,
  longClickCallbacks: LongClickCallbacks,
  storyContextMenuCallbacks: StoryContextMenuCallbacks,
  callButtonClickCallbacks: CallButtonClickCallbacks
) : ContactSearchAdapter(context, fixedContacts, displayOptions, onClickCallbacks, longClickCallbacks, storyContextMenuCallbacks, callButtonClickCallbacks) {

  init {
    registerFactory(NewGroupModel::class.java, LayoutFactory({ NewGroupViewHolder(it, onClickCallbacks::onNewGroupClicked) }, R.layout.contact_selection_new_group_item))
    registerFactory(InviteToZonaRosaModel::class.java, LayoutFactory({ InviteToZonaRosaViewHolder(it, onClickCallbacks::onInviteToZonaRosaClicked) }, R.layout.contact_selection_invite_action_item))
    registerFactory(FindContactsModel::class.java, LayoutFactory({ FindContactsViewHolder(it, onClickCallbacks::onFindContactsClicked) }, R.layout.contact_selection_find_contacts_item))
    registerFactory(FindContactsBannerModel::class.java, LayoutFactory({ FindContactsBannerViewHolder(it, onClickCallbacks::onDismissFindContactsBannerClicked, onClickCallbacks::onFindContactsClicked) }, R.layout.contact_selection_find_contacts_banner_item))
    registerFactory(RefreshContactsModel::class.java, LayoutFactory({ RefreshContactsViewHolder(it, onClickCallbacks::onRefreshContactsClicked) }, R.layout.contact_selection_refresh_action_item))
    registerFactory(MoreHeaderModel::class.java, LayoutFactory({ MoreHeaderViewHolder(it) }, R.layout.contact_search_section_header))
    registerFactory(EmptyModel::class.java, LayoutFactory({ EmptyViewHolder(it) }, R.layout.contact_selection_empty_state))
    registerFactory(FindByUsernameModel::class.java, LayoutFactory({ FindByUsernameViewHolder(it, onClickCallbacks::onFindByUsernameClicked) }, R.layout.contact_selection_find_by_username_item))
    registerFactory(FindByPhoneNumberModel::class.java, LayoutFactory({ FindByPhoneNumberViewHolder(it, onClickCallbacks::onFindByPhoneNumberClicked) }, R.layout.contact_selection_find_by_phone_number_item))
  }

  class NewGroupModel : MappingModel<NewGroupModel> {
    override fun areItemsTheSame(newItem: NewGroupModel): Boolean = true
    override fun areContentsTheSame(newItem: NewGroupModel): Boolean = true
  }

  class InviteToZonaRosaModel : MappingModel<InviteToZonaRosaModel> {
    override fun areItemsTheSame(newItem: InviteToZonaRosaModel): Boolean = true
    override fun areContentsTheSame(newItem: InviteToZonaRosaModel): Boolean = true
  }

  class RefreshContactsModel : MappingModel<RefreshContactsModel> {
    override fun areItemsTheSame(newItem: RefreshContactsModel): Boolean = true
    override fun areContentsTheSame(newItem: RefreshContactsModel): Boolean = true
  }

  class FindContactsModel : MappingModel<FindContactsModel> {
    override fun areItemsTheSame(newItem: FindContactsModel): Boolean = true
    override fun areContentsTheSame(newItem: FindContactsModel): Boolean = true
  }

  class FindContactsBannerModel : MappingModel<FindContactsBannerModel> {
    override fun areItemsTheSame(newItem: FindContactsBannerModel): Boolean = true
    override fun areContentsTheSame(newItem: FindContactsBannerModel): Boolean = true
  }

  class FindByUsernameModel : MappingModel<FindByUsernameModel> {
    override fun areItemsTheSame(newItem: FindByUsernameModel): Boolean = true
    override fun areContentsTheSame(newItem: FindByUsernameModel): Boolean = true
  }

  class FindByPhoneNumberModel : MappingModel<FindByPhoneNumberModel> {
    override fun areItemsTheSame(newItem: FindByPhoneNumberModel): Boolean = true
    override fun areContentsTheSame(newItem: FindByPhoneNumberModel): Boolean = true
  }

  class MoreHeaderModel : MappingModel<MoreHeaderModel> {
    override fun areItemsTheSame(newItem: MoreHeaderModel): Boolean = true

    override fun areContentsTheSame(newItem: MoreHeaderModel): Boolean = true
  }

  private class InviteToZonaRosaViewHolder(itemView: View, onClickListener: () -> Unit) : MappingViewHolder<InviteToZonaRosaModel>(itemView) {
    init {
      itemView.setOnClickListener { onClickListener() }
    }

    override fun bind(model: InviteToZonaRosaModel) = Unit
  }

  private class NewGroupViewHolder(itemView: View, onClickListener: () -> Unit) : MappingViewHolder<NewGroupModel>(itemView) {
    init {
      itemView.setOnClickListener { onClickListener() }
    }

    override fun bind(model: NewGroupModel) = Unit
  }

  private class RefreshContactsViewHolder(itemView: View, onClickListener: () -> Unit) : MappingViewHolder<RefreshContactsModel>(itemView) {
    init {
      itemView.setOnClickListener { onClickListener() }
    }

    override fun bind(model: RefreshContactsModel) = Unit
  }

  private class FindContactsViewHolder(itemView: View, onClickListener: () -> Unit) : MappingViewHolder<FindContactsModel>(itemView) {
    init {
      itemView.setOnClickListener { onClickListener() }
    }

    override fun bind(model: FindContactsModel) = Unit
  }

  private class FindContactsBannerViewHolder(itemView: View, onDismissListener: () -> Unit, onClickListener: () -> Unit) : MappingViewHolder<FindContactsBannerModel>(itemView) {
    init {
      itemView.findViewById<MaterialButton>(R.id.no_thanks_button).setOnClickListener { onDismissListener() }
      itemView.findViewById<MaterialButton>(R.id.allow_contacts_button).setOnClickListener { onClickListener() }
    }

    override fun bind(model: FindContactsBannerModel) = Unit
  }

  private class MoreHeaderViewHolder(itemView: View) : MappingViewHolder<MoreHeaderModel>(itemView) {

    private val headerTextView: TextView = itemView.findViewById(R.id.section_header)

    override fun bind(model: MoreHeaderModel) {
      headerTextView.setText(R.string.contact_selection_activity__more)
    }
  }

  private class EmptyViewHolder(itemView: View) : MappingViewHolder<EmptyModel>(itemView) {

    private val emptyText: TextView = itemView.findViewById(R.id.search_no_results)

    override fun bind(model: EmptyModel) {
      emptyText.text = context.getString(R.string.SearchFragment_no_results, model.empty.query ?: "")
    }
  }

  private class FindByPhoneNumberViewHolder(itemView: View, onClickListener: () -> Unit) : MappingViewHolder<FindByPhoneNumberModel>(itemView) {

    init {
      itemView.setOnClickListener { onClickListener() }
    }

    override fun bind(model: FindByPhoneNumberModel) = Unit
  }

  private class FindByUsernameViewHolder(itemView: View, onClickListener: () -> Unit) : MappingViewHolder<FindByUsernameModel>(itemView) {

    init {
      itemView.setOnClickListener { onClickListener() }
    }

    override fun bind(model: FindByUsernameModel) = Unit
  }

  class ArbitraryRepository : io.zonarosa.messenger.contacts.paged.ArbitraryRepository {

    enum class ArbitraryRow(val code: String) {
      NEW_GROUP("new-group"),
      INVITE_TO_ZONAROSA("invite-to-zonarosa"),
      MORE_HEADING("more-heading"),
      REFRESH_CONTACTS("refresh-contacts"),
      FIND_CONTACTS("find-contacts"),
      FIND_CONTACTS_BANNER("find-contacts-banner"),
      FIND_BY_USERNAME("find-by-username"),
      FIND_BY_PHONE_NUMBER("find-by-phone-number");

      companion object {
        fun fromCode(code: String) = entries.first { it.code == code }
      }
    }

    override fun getSize(section: ContactSearchConfiguration.Section.Arbitrary, query: String?): Int {
      return section.types.size
    }

    override fun getData(section: ContactSearchConfiguration.Section.Arbitrary, query: String?, startIndex: Int, endIndex: Int, totalSearchSize: Int): List<ContactSearchData.Arbitrary> {
      check(section.types.size == 1)
      return listOf(ContactSearchData.Arbitrary(section.types.first()))
    }

    override fun getMappingModel(arbitrary: ContactSearchData.Arbitrary): MappingModel<*> {
      return when (ArbitraryRow.fromCode(arbitrary.type)) {
        ArbitraryRow.NEW_GROUP -> NewGroupModel()
        ArbitraryRow.INVITE_TO_ZONAROSA -> InviteToZonaRosaModel()
        ArbitraryRow.MORE_HEADING -> MoreHeaderModel()
        ArbitraryRow.REFRESH_CONTACTS -> RefreshContactsModel()
        ArbitraryRow.FIND_CONTACTS -> FindContactsModel()
        ArbitraryRow.FIND_CONTACTS_BANNER -> FindContactsBannerModel()
        ArbitraryRow.FIND_BY_PHONE_NUMBER -> FindByPhoneNumberModel()
        ArbitraryRow.FIND_BY_USERNAME -> FindByUsernameModel()
      }
    }
  }

  interface OnContactSelectionClick : ClickCallbacks {
    fun onNewGroupClicked()
    fun onInviteToZonaRosaClicked()
    fun onRefreshContactsClicked()
    fun onFindContactsClicked()
    fun onDismissFindContactsBannerClicked()
    fun onFindByPhoneNumberClicked()
    fun onFindByUsernameClicked()
  }
}
