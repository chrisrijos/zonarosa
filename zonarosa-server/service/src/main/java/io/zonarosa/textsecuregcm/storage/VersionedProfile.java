/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import io.zonarosa.server.util.ByteArrayAdapter;
import io.zonarosa.server.util.ByteArrayBase64WithPaddingAdapter;

public record VersionedProfile (String version,
                                @JsonSerialize(using = ByteArrayBase64WithPaddingAdapter.Serializing.class)
                                @JsonDeserialize(using = ByteArrayBase64WithPaddingAdapter.Deserializing.class)
                                byte[] name,

                                @Nullable
                                String avatar,

                                @JsonSerialize(using = ByteArrayBase64WithPaddingAdapter.Serializing.class)
                                @JsonDeserialize(using = ByteArrayBase64WithPaddingAdapter.Deserializing.class)
                                byte[] aboutEmoji,

                                @JsonSerialize(using = ByteArrayBase64WithPaddingAdapter.Serializing.class)
                                @JsonDeserialize(using = ByteArrayBase64WithPaddingAdapter.Deserializing.class)
                                byte[] about,

                                @JsonSerialize(using = ByteArrayBase64WithPaddingAdapter.Serializing.class)
                                @JsonDeserialize(using = ByteArrayBase64WithPaddingAdapter.Deserializing.class)
                                byte[] paymentAddress,

                                @JsonSerialize(using = ByteArrayAdapter.Serializing.class)
                                @JsonDeserialize(using = ByteArrayAdapter.Deserializing.class)
                                byte[] phoneNumberSharing,

                                @JsonSerialize(using = ByteArrayAdapter.Serializing.class)
                                @JsonDeserialize(using = ByteArrayAdapter.Deserializing.class)
                                byte[] commitment) {}
