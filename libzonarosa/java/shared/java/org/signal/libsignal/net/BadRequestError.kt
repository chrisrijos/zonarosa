//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

// This file exists so that BadRequestError and its sub-interfaces can be applied to exception types
// shared between the client and server, even though typed request APIs are a client-only thing.

package io.zonarosa.libzonarosa.net

/**
 * Marker interface for business logic errors returned by typed request APIs.
 *
 * All API-specific error types must implement this interface. Errors can
 * implement multiple specific error interfaces to indicate they may be
 * returned by multiple APIs.
 *
 * Example:
 * ```kotlin
 * sealed interface AciByUsernameFetchError : BadRequestError
 * object UserNotFound : AciByUsernameFetchError
 * ```
 */
public interface BadRequestError

/**
 * [io.zonarosa.libzonarosa.usernames.UsernameLinkInvalidEntropyDataLength] and
 * [io.zonarosa.libzonarosa.usernames.UsernameLinkInvalidLinkData]
 */
public sealed interface LookUpUsernameLinkFailure : BadRequestError
