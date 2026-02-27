/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util.androidx

import androidx.documentfile.provider.DocumentFile

/**
 * Information about a file within the storage. Useful because default [DocumentFile] implementations
 * re-query info on each access.
 */
data class DocumentFileInfo(val documentFile: DocumentFile, val name: String, val size: Long)
