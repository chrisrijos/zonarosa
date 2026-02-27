//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import * as Native from './Native.js';

export function toJSONString(buffer: Uint8Array): string {
  return Native.MinidumpToJSONString(buffer);
}
