package io.zonarosa.messenger.sharing.interstitial;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.annimon.stream.Stream;

import io.zonarosa.messenger.linkpreview.LinkPreview;
import io.zonarosa.messenger.sharing.MultiShareArgs;
import io.zonarosa.messenger.sharing.MultiShareSender;
import io.zonarosa.messenger.util.DefaultValueLiveData;
import io.zonarosa.core.util.Util;
import io.zonarosa.messenger.util.adapter.mapping.MappingModelList;

class ShareInterstitialViewModel extends ViewModel {

private final MultiShareArgs                      args;
  private final MutableLiveData<MappingModelList> recipients;
  private final MutableLiveData<String>           draftText;

  private LinkPreview linkPreview;

  ShareInterstitialViewModel(@NonNull MultiShareArgs args, @NonNull ShareInterstitialRepository repository) {
    this.args        = args;
    this.recipients  = new MutableLiveData<>();
    this.draftText   = new DefaultValueLiveData<>(Util.firstNonNull(args.getDraftText(), ""));

    repository.loadRecipients(args.getRecipientSearchKeys(),
                              list -> recipients.postValue(Stream.of(list)
                                                                 .mapIndexed((i, r) -> new ShareInterstitialMappingModel(r, i == 0))
                                                                 .collect(MappingModelList.toMappingModelList())));

  }

  LiveData<MappingModelList> getRecipients() {
    return recipients;
  }

  LiveData<Boolean> hasDraftText() {
    return Transformations.map(draftText, text -> !TextUtils.isEmpty(text));
  }

  void onDraftTextChanged(@NonNull String change) {
    draftText.setValue(change);
  }

  void onLinkPreviewChanged(@Nullable LinkPreview linkPreview) {
    this.linkPreview = linkPreview;
  }

  void send(@NonNull Consumer<MultiShareSender.MultiShareSendResultCollection> resultsConsumer) {
    LinkPreview linkPreview = this.linkPreview;
    String      draftText   = this.draftText.getValue();

    MultiShareArgs.Builder builder = args.buildUpon()
                                         .withDraftText(draftText)
                                         .withLinkPreview(linkPreview);

    MultiShareSender.send(builder.build(), resultsConsumer);
  }

  static class Factory implements ViewModelProvider.Factory {

    private final MultiShareArgs args;
    private final ShareInterstitialRepository repository;

    Factory(@NonNull MultiShareArgs args, @NonNull ShareInterstitialRepository repository) {
      this.args       = args;
      this.repository = repository;
    }

    @Override
    public @NonNull <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      return modelClass.cast(new ShareInterstitialViewModel(args, repository));
    }
  }
}
