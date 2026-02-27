/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.glide.cache

import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.load.ImageHeaderParser
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import io.zonarosa.core.util.StreamUtil
import io.zonarosa.glide.apng.ApngOptions
import io.zonarosa.glide.common.io.InputStreamFactory
import io.zonarosa.glide.load.ImageHeaderParserUtils
import io.zonarosa.glide.load.resource.apng.decode.APNGDecoder
import java.nio.ByteBuffer

/**
 * A variant of [StreamApngDecoder] that decodes animated PNGs from [InputStreamFactory] sources.
 */
class StreamFactoryApngDecoder(
  private val byteBufferDecoder: ResourceDecoder<ByteBuffer, APNGDecoder>,
  private val glide: Glide,
  private val registry: Registry
) : ResourceDecoder<InputStreamFactory, APNGDecoder> {

  override fun handles(source: InputStreamFactory, options: Options): Boolean {
    return if (options.get(ApngOptions.ANIMATE) == true) {
      val imageType = ImageHeaderParserUtils.getType(registry.imageHeaderParsers, source.create(), glide.arrayPool)
      imageType == ImageHeaderParser.ImageType.PNG || imageType == ImageHeaderParser.ImageType.PNG_A
    } else {
      false
    }
  }

  override fun decode(
    source: InputStreamFactory,
    width: Int,
    height: Int,
    options: Options
  ): Resource<APNGDecoder>? {
    val data = StreamUtil.readFully(source.create())
    val byteBuffer = ByteBuffer.wrap(data)
    return byteBufferDecoder.decode(byteBuffer, width, height, options)
  }
}
