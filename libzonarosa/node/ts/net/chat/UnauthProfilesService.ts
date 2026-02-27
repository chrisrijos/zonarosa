//
// Copyright 2026 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import { ServiceId } from '../../Address.js';
import { RequestOptions, UnauthenticatedChatConnection } from '../Chat.js';
import * as Native from '../../Native.js';

declare module '../Chat' {
  // eslint-disable-next-line @typescript-eslint/no-empty-object-type
  interface UnauthenticatedChatConnection extends UnauthProfilesService {}
}

export interface UnauthProfilesService {
  /**
   * Does an account with the given ACI or PNI exist?
   *
   * Throws / completes with failure only if the request can't be completed.
   */
  accountExists: (
    request: {
      account: ServiceId;
    },
    options?: RequestOptions
  ) => Promise<boolean>;
}

UnauthenticatedChatConnection.prototype.accountExists = async function (
  {
    account,
  }: {
    account: ServiceId;
  },
  options?: RequestOptions
): Promise<boolean> {
  return await this._asyncContext.makeCancellable(
    options?.abortZonaRosa,
    Native.UnauthenticatedChatConnection_account_exists(
      this._asyncContext,
      this._chatService,
      account.getServiceIdFixedWidthBinary()
    )
  );
};
