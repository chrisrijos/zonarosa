/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2.items

import io.zonarosa.messenger.util.adapter.mapping.MappingModel
import io.zonarosa.messenger.util.adapter.mapping.MappingViewHolder

/**
 * Base ViewHolder to share some common properties shared among conversation items.
 */
abstract class V2ConversationItemViewHolder<Model : MappingModel<Model>>(
  root: V2ConversationItemLayout,
  appearanceInfoProvider: V2ConversationContext
) : MappingViewHolder<Model>(root) {
  protected val shapeDelegate = V2ConversationItemShape(appearanceInfoProvider)
  protected val themeDelegate = V2ConversationItemTheme(context, appearanceInfoProvider)
}
