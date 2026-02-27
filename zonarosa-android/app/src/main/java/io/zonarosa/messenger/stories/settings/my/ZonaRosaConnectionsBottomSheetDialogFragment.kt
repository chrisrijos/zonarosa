package io.zonarosa.messenger.stories.settings.my

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.zonarosa.core.ui.FixedRoundedCornerBottomSheetDialogFragment
import io.zonarosa.messenger.R
import io.zonarosa.messenger.util.SpanUtil

class ZonaRosaConnectionsBottomSheetDialogFragment : FixedRoundedCornerBottomSheetDialogFragment() {

  override val peekHeightPercentage: Float = 1f

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val view = inflater.inflate(R.layout.stories_zonarosa_connection_bottom_sheet, container, false)
    view.findViewById<TextView>(R.id.text_1).text = SpanUtil.boldSubstring(getString(R.string.ZonaRosaConnectionsBottomSheet__zonarosa_connections_are_people), getString(R.string.ZonaRosaConnectionsBottomSheet___zonarosa_connections))
    return view
  }
}
