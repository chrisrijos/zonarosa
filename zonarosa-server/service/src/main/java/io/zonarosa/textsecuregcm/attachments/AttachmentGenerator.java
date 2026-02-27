/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.attachments;
import java.util.Map;

public interface AttachmentGenerator {

  record Descriptor(Map<String, String> headers, String signedUploadLocation) {}

  Descriptor generateAttachment(final String key);

}
