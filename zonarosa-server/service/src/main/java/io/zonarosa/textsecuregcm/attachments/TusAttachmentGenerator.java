/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.attachments;

import org.apache.http.HttpHeaders;
import io.zonarosa.server.auth.ExternalServiceCredentials;
import io.zonarosa.server.auth.ExternalServiceCredentialsGenerator;
import io.zonarosa.server.util.HeaderUtils;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;

public class TusAttachmentGenerator implements AttachmentGenerator {

  private static final String ATTACHMENTS = "attachments";

  final ExternalServiceCredentialsGenerator credentialsGenerator;
  final String tusUri;

  public TusAttachmentGenerator(final TusConfiguration cfg) {
    this.tusUri = cfg.uploadUri();
    this.credentialsGenerator = credentialsGenerator(Clock.systemUTC(), cfg);
  }

  private static ExternalServiceCredentialsGenerator credentialsGenerator(final Clock clock, final TusConfiguration cfg) {
    return ExternalServiceCredentialsGenerator
        .builder(cfg.userAuthenticationTokenSharedSecret())
        .prependUsername(false)
        .withClock(clock)
        .build();
  }

  @Override
  public Descriptor generateAttachment(final String key) {
    final ExternalServiceCredentials credentials = credentialsGenerator.generateFor(ATTACHMENTS + "/" + key);
    final String b64Key = Base64.getEncoder().encodeToString(key.getBytes(StandardCharsets.UTF_8));
    final Map<String, String> headers = Map.of(
        HttpHeaders.AUTHORIZATION, HeaderUtils.basicAuthHeader(credentials),
        "Upload-Metadata", String.format("filename %s", b64Key)
    );
    return new Descriptor(headers, tusUri + "/" +  ATTACHMENTS);
  }
}
