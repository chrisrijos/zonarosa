package io.zonarosa.messenger.stickers;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.service.api.ZonaRosaServiceMessageReceiver;

import java.io.InputStream;

/**
 * Glide loader to fetch a sticker remotely.
 */
public final class StickerRemoteUriLoader implements ModelLoader<StickerRemoteUri, InputStream> {

  private final ZonaRosaServiceMessageReceiver receiver;

  public StickerRemoteUriLoader(@NonNull ZonaRosaServiceMessageReceiver receiver) {
    this.receiver = receiver;
  }


  @Override
  public @NonNull LoadData<InputStream> buildLoadData(@NonNull StickerRemoteUri sticker, int width, int height, @NonNull Options options) {
    return new LoadData<>(sticker, new StickerRemoteUriFetcher(receiver, sticker));
  }

  @Override
  public boolean handles(@NonNull StickerRemoteUri sticker) {
    return true;
  }

  public static class Factory implements ModelLoaderFactory<StickerRemoteUri, InputStream> {

    @Override
    public @NonNull ModelLoader<StickerRemoteUri, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return new StickerRemoteUriLoader(AppDependencies.getZonaRosaServiceMessageReceiver());
    }

    @Override
    public void teardown() {
    }
  }
}
