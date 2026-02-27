//
// Copyright 2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import ByteArray from '../internal/ByteArray.js';
import * as Native from '../../Native.js';

export default class ExpiringProfileKeyCredentialResponse extends ByteArray {
  private readonly __type?: never;

  constructor(contents: Uint8Array) {
    super(
      contents,
      Native.ExpiringProfileKeyCredentialResponse_CheckValidContents
    );
  }
}
