package io.zonarosa.messenger.payments.preferences.addmoney

import androidx.annotation.MainThread
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.core.util.Result as ZonaRosaResult

internal class PaymentsAddMoneyRepository {
  @MainThread
  fun getWalletAddress(): Single<ZonaRosaResult<AddressAndUri, Error>> {
    if (!ZonaRosaStore.payments.mobileCoinPaymentsEnabled()) {
      return Single.just(ZonaRosaResult.failure(Error.PAYMENTS_NOT_ENABLED))
    }

    return Single.fromCallable<ZonaRosaResult<AddressAndUri, Error>> {
      val publicAddress = AppDependencies.payments.wallet.mobileCoinPublicAddress
      val paymentAddressBase58 = publicAddress.paymentAddressBase58
      val paymentAddressUri = publicAddress.paymentAddressUri
      ZonaRosaResult.success(AddressAndUri(paymentAddressBase58, paymentAddressUri))
    }
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
  }

  internal enum class Error {
    PAYMENTS_NOT_ENABLED
  }
}
