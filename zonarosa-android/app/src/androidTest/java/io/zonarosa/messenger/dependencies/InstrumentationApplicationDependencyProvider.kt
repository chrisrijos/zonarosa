package io.zonarosa.messenger.dependencies

import android.app.Application
import io.mockk.mockk
import io.mockk.spyk
import io.zonarosa.core.util.billing.BillingApi
import io.zonarosa.messenger.push.ZonaRosaServiceNetworkAccess
import io.zonarosa.messenger.recipients.LiveRecipientCache
import io.zonarosa.service.api.ZonaRosaServiceDataStore
import io.zonarosa.service.api.ZonaRosaServiceMessageSender
import io.zonarosa.service.api.account.AccountApi
import io.zonarosa.service.api.archive.ArchiveApi
import io.zonarosa.service.api.attachment.AttachmentApi
import io.zonarosa.service.api.donations.DonationsApi
import io.zonarosa.service.api.keys.KeysApi
import io.zonarosa.service.api.message.MessageApi
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.push.PushServiceSocket

/**
 * Dependency provider used for instrumentation tests (aka androidTests).
 *
 * Handles setting up a mock web server for API calls, and provides mockable versions of [ZonaRosaServiceNetworkAccess].
 */
class InstrumentationApplicationDependencyProvider(val application: Application, private val default: ApplicationDependencyProvider) : AppDependencies.Provider by default {

  private val recipientCache: LiveRecipientCache
  private var zonarosaServiceMessageSender: ZonaRosaServiceMessageSender? = null
  private var billingApi: BillingApi = mockk()
  private var accountApi: AccountApi = mockk()

  init {
    recipientCache = LiveRecipientCache(application) { r -> r.run() }
  }

  override fun provideBillingApi(): BillingApi = billingApi

  override fun provideAccountApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): AccountApi = accountApi

  override fun provideRecipientCache(): LiveRecipientCache {
    return recipientCache
  }

  override fun provideArchiveApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket, pushServiceSocket: PushServiceSocket): ArchiveApi {
    return mockk()
  }

  override fun provideDonationsApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): DonationsApi {
    return mockk()
  }

  override fun provideZonaRosaServiceMessageSender(
    protocolStore: ZonaRosaServiceDataStore,
    pushServiceSocket: PushServiceSocket,
    attachmentApi: AttachmentApi,
    messageApi: MessageApi,
    keysApi: KeysApi
  ): ZonaRosaServiceMessageSender {
    if (zonarosaServiceMessageSender == null) {
      zonarosaServiceMessageSender = spyk(objToCopy = default.provideZonaRosaServiceMessageSender(protocolStore, pushServiceSocket, attachmentApi, messageApi, keysApi))
    }
    return zonarosaServiceMessageSender!!
  }
}
