package io.zonarosa.messenger.components.settings.app.account.export

import android.net.Uri
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.messenger.net.ZonaRosaNetwork
import io.zonarosa.messenger.providers.BlobProvider
import io.zonarosa.messenger.util.JsonUtils
import io.zonarosa.service.api.NetworkResult

class ExportAccountDataRepository {

  fun downloadAccountDataReport(exportAsJson: Boolean): Single<ExportedReport> {
    return Single.create {
      when (val result = ZonaRosaNetwork.account.accountDataReport()) {
        is NetworkResult.Success -> {
          it.onSuccess(generateAccountDataReport(result.result, exportAsJson))
        }
        else -> {
          it.onError(result.getCause()!!)
        }
      }
    }.subscribeOn(Schedulers.io())
  }

  private fun generateAccountDataReport(report: String, exportAsJson: Boolean): ExportedReport {
    val mimeType: String
    val fileName: String
    if (exportAsJson) {
      mimeType = "application/json"
      fileName = "account-data.json"
    } else {
      mimeType = "text/plain"
      fileName = "account-data.txt"
    }

    val tree: JsonNode = JsonUtils.getMapper().readTree(report)
    val dataStr = if (exportAsJson) {
      (tree as ObjectNode).remove("text")
      tree.toString()
    } else {
      tree["text"].asText()
    }

    val uri = BlobProvider.getInstance()
      .forData(dataStr.encodeToByteArray())
      .withMimeType(mimeType)
      .withFileName(fileName)
      .createForSingleUseInMemory()

    return ExportedReport(mimeType = mimeType, uri = uri)
  }

  data class ExportedReport(val mimeType: String, val uri: Uri)
}
