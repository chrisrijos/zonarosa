//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//
package io.zonarosa.libzonarosa.protocol.incrementalmac

import io.zonarosa.libzonarosa.internal.Native

public abstract class ChunkSizeChoice {
  public abstract val sizeInBytes: Int

  public companion object {
    @JvmStatic
    public fun everyNthByte(n: Int): ChunkSizeChoice = EveryN(n)

    @JvmStatic
    public fun inferChunkSize(dataSize: Int): ChunkSizeChoice = ChunksOf(dataSize)
  }
}

internal final data class EveryN(
  val n: Int,
) : ChunkSizeChoice() {
  override val sizeInBytes: Int = n
}

internal final data class ChunksOf(
  val dataSize: Int,
) : ChunkSizeChoice() {
  override val sizeInBytes: Int = Native.IncrementalMac_CalculateChunkSize(this.dataSize)
}
