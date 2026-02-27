/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.net

import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.dependencies.KeyTransparencyApi
import io.zonarosa.service.api.account.AccountApi
import io.zonarosa.service.api.archive.ArchiveApi
import io.zonarosa.service.api.attachment.AttachmentApi
import io.zonarosa.service.api.calling.CallingApi
import io.zonarosa.service.api.cds.CdsApi
import io.zonarosa.service.api.certificate.CertificateApi
import io.zonarosa.service.api.keys.KeysApi
import io.zonarosa.service.api.link.LinkDeviceApi
import io.zonarosa.service.api.message.MessageApi
import io.zonarosa.service.api.payments.PaymentsApi
import io.zonarosa.service.api.profiles.ProfileApi
import io.zonarosa.service.api.provisioning.ProvisioningApi
import io.zonarosa.service.api.ratelimit.RateLimitChallengeApi
import io.zonarosa.service.api.remoteconfig.RemoteConfigApi
import io.zonarosa.service.api.storage.StorageServiceApi
import io.zonarosa.service.api.svr.SvrBApi
import io.zonarosa.service.api.username.UsernameApi

/**
 * A convenient way to access network operations, similar to [io.zonarosa.messenger.database.ZonaRosaDatabase] and [io.zonarosa.messenger.keyvalue.ZonaRosaStore].
 */
object ZonaRosaNetwork {
  @JvmStatic
  @get:JvmName("account")
  val account: AccountApi
    get() = AppDependencies.accountApi

  val archive: ArchiveApi
    get() = AppDependencies.archiveApi

  val attachments: AttachmentApi
    get() = AppDependencies.attachmentApi

  @JvmStatic
  @get:JvmName("calling")
  val calling: CallingApi
    get() = AppDependencies.callingApi

  val cdsApi: CdsApi
    get() = AppDependencies.cdsApi

  @JvmStatic
  @get:JvmName("certificate")
  val certificate: CertificateApi
    get() = AppDependencies.certificateApi

  @JvmStatic
  @get:JvmName("keys")
  val keys: KeysApi
    get() = AppDependencies.keysApi

  val linkDevice: LinkDeviceApi
    get() = AppDependencies.linkDeviceApi

  @JvmStatic
  @get:JvmName("message")
  val message: MessageApi
    get() = AppDependencies.messageApi

  @JvmStatic
  @get:JvmName("payments")
  val payments: PaymentsApi
    get() = AppDependencies.paymentsApi

  @JvmStatic
  @get:JvmName("profile")
  val profile: ProfileApi
    get() = AppDependencies.profileApi

  val provisioning: ProvisioningApi
    get() = AppDependencies.provisioningApi

  @JvmStatic
  @get:JvmName("rateLimitChallenge")
  val rateLimitChallenge: RateLimitChallengeApi
    get() = AppDependencies.rateLimitChallengeApi

  @JvmStatic
  @get:JvmName("remoteConfig")
  val remoteConfig: RemoteConfigApi
    get() = AppDependencies.remoteConfigApi

  val storageService: StorageServiceApi
    get() = AppDependencies.storageServiceApi

  @JvmStatic
  @get:JvmName("username")
  val username: UsernameApi
    get() = AppDependencies.usernameApi

  val svrB: SvrBApi
    get() = AppDependencies.svrBApi

  val keyTransparency: KeyTransparencyApi
    get() = AppDependencies.keyTransparencyApi
}
