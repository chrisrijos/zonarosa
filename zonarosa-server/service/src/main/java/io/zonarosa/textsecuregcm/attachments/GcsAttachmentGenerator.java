/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.attachments;

import io.zonarosa.server.gcp.CanonicalRequest;
import io.zonarosa.server.gcp.CanonicalRequestGenerator;
import io.zonarosa.server.gcp.CanonicalRequestSigner;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

public class GcsAttachmentGenerator implements AttachmentGenerator {
  @Nonnull
  private final CanonicalRequestGenerator canonicalRequestGenerator;

  @Nonnull
  private final CanonicalRequestSigner canonicalRequestSigner;

  public GcsAttachmentGenerator(@Nonnull String domain, @Nonnull String email,
      int maxSizeInBytes, @Nonnull String pathPrefix, @Nonnull String rsaSigningKey)
      throws IOException, InvalidKeyException, InvalidKeySpecException {
    this.canonicalRequestGenerator = new CanonicalRequestGenerator(domain, email, maxSizeInBytes, pathPrefix);
    this.canonicalRequestSigner = new CanonicalRequestSigner(rsaSigningKey);
  }

  @Override
  public Descriptor generateAttachment(final String key) {
    final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
    final CanonicalRequest canonicalRequest = canonicalRequestGenerator.createFor(key, now);
    return new Descriptor(getHeaderMap(canonicalRequest), getSignedUploadLocation(canonicalRequest));
  }

  private String getSignedUploadLocation(@Nonnull CanonicalRequest canonicalRequest) {
    return "https://" + canonicalRequest.getDomain() + canonicalRequest.getResourcePath()
        + '?' + canonicalRequest.getCanonicalQuery()
        + "&X-Goog-Signature=" + canonicalRequestSigner.sign(canonicalRequest);
  }

  private static Map<String, String> getHeaderMap(@Nonnull CanonicalRequest canonicalRequest) {
    return Map.of(
        "host", canonicalRequest.getDomain(),
        "x-goog-content-length-range", "1," + canonicalRequest.getMaxSizeInBytes(),
        "x-goog-resumable", "start");
  }


}
