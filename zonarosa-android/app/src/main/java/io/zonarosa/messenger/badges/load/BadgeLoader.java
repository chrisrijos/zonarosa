/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.badges.load;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import io.zonarosa.messenger.badges.models.Badge;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.glide.OkHttpStreamFetcher;

import java.io.InputStream;

import okhttp3.OkHttpClient;

/**
 * A loader which will load a sprite sheet for a particular badge at the correct dpi for this device.
 */
public class BadgeLoader implements ModelLoader<Badge, InputStream> {

  private final OkHttpClient client;

  private BadgeLoader(OkHttpClient client) {
    this.client = client;
  }

  @Override
  public @Nullable LoadData<InputStream> buildLoadData(@NonNull Badge request, int width, int height, @NonNull Options options) {
    return new LoadData<>(request, new OkHttpStreamFetcher(client, new GlideUrl(request.getImageUrl().toString())));
  }

  @Override
  public boolean handles(@NonNull Badge badgeSpriteSheetRequest) {
    return true;
  }

  public static Factory createFactory() {
    return new Factory(AppDependencies.getZonaRosaOkHttpClient());
  }

  public static class Factory implements ModelLoaderFactory<Badge, InputStream> {

    private final OkHttpClient client;

    private Factory(@NonNull OkHttpClient client) {
      this.client = client;
    }

    @Override
    public @NonNull ModelLoader<Badge, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
      return new BadgeLoader(client);
    }

    @Override
    public void teardown() {
    }
  }
}