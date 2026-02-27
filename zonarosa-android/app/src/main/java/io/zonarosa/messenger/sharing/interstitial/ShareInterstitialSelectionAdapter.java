package io.zonarosa.messenger.sharing.interstitial;

import io.zonarosa.messenger.R;
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter;
import io.zonarosa.messenger.util.viewholders.RecipientViewHolder;

class ShareInterstitialSelectionAdapter extends MappingAdapter {
  ShareInterstitialSelectionAdapter() {
    registerFactory(ShareInterstitialMappingModel.class, RecipientViewHolder.createFactory(R.layout.share_contact_selection_item, null));
  }
}
