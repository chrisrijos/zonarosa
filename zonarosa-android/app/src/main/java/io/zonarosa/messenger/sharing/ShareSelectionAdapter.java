package io.zonarosa.messenger.sharing;

import io.zonarosa.messenger.R;
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter;

public class ShareSelectionAdapter extends MappingAdapter {
  public ShareSelectionAdapter() {
    registerFactory(ShareSelectionMappingModel.class,
                    ShareSelectionViewHolder.createFactory(R.layout.share_contact_selection_item));
  }
}
