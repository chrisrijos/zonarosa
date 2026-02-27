/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

public record AttachmentDescriptorV3(
    @Schema(description = """
        Indicates the CDN type. 2 in the v3 API, 2 or 3 in the v4 API.
        2 indicates resumable uploads using GCS,
        3 indicates resumable uploads using TUS
        """)
    int cdn,
    @Schema(description = "The location within the specified cdn where the finished upload can be found")
    String key,
    @Schema(description = "A map of headers to include with all upload requests. Potentially contains time-limited upload credentials")
    Map<String, String> headers,

    @Schema(description = "The URL to upload to with the appropriate protocol")
    String signedUploadLocation) {

}
