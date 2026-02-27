//
// Copyright 2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import { ProtocolAddress, ServiceId } from './Address.js';
import * as Native from './Native.js';

export enum ErrorCode {
  Generic,

  DuplicatedMessage,
  SealedSenderSelfSend,
  UntrustedIdentity,
  InvalidRegistrationId,
  InvalidProtocolAddress,
  VerificationFailed,
  InvalidSession,
  InvalidSenderKeySession,

  NicknameCannotBeEmpty,
  CannotStartWithDigit,
  MissingSeparator,
  BadNicknameCharacter,
  NicknameTooShort,
  NicknameTooLong,
  DiscriminatorCannotBeEmpty,
  DiscriminatorCannotBeZero,
  DiscriminatorCannotBeSingleDigit,
  DiscriminatorCannotHaveLeadingZeros,
  BadDiscriminatorCharacter,
  DiscriminatorTooLarge,

  IoError,
  CdsiInvalidToken,
  InvalidUri,

  InvalidMediaInput,
  UnsupportedMediaInput,

  InputDataTooLong,
  InvalidEntropyDataLength,
  InvalidUsernameLinkEncryptedData,

  RateLimitedError,
  RateLimitChallengeError,

  SvrDataMissing,
  SvrRequestFailed,
  SvrRestoreFailed,
  SvrAttestationError,
  SvrInvalidData,

  ChatServiceInactive,
  AppExpired,
  DeviceDelinked,
  ConnectionInvalidated,
  ConnectedElsewhere,
  PossibleCaptiveNetwork,

  BackupValidation,

  Cancelled,

  KeyTransparencyError,
  KeyTransparencyVerificationFailed,

  IncrementalMacVerificationFailed,

  RequestUnauthorized,
  MismatchedDevices,
}

/** Called out as a separate type so it's not confused with a normal ServiceIdBinary. */
type ServiceIdFixedWidthBinary = Uint8Array;

/**
 * A failure sending to a recipient on account of not being up to date on their devices.
 *
 * An entry in {@link MismatchedDevicesError}. Each entry represents a recipient that has either
 * added, removed, or relinked some devices in their account (potentially including their primary
 * device), as represented by the {@link MismatchedDevicesEntry#missingDevices},
 * {@link MismatchedDevicesEntry#extraDevices}, and {@link MismatchedDevicesEntry#staleDevices}
 * arrays, respectively. Handling the exception involves removing the "extra" devices and
 * establishing new sessions for the "missing" and "stale" devices.
 */
export class MismatchedDevicesEntry {
  account: ServiceId;
  missingDevices: number[];
  extraDevices: number[];
  staleDevices: number[];

  constructor({
    account,
    missingDevices,
    extraDevices,
    staleDevices,
  }: {
    account: ServiceId | ServiceIdFixedWidthBinary;
    missingDevices?: number[];
    extraDevices?: number[];
    staleDevices?: number[];
  }) {
    this.account =
      account instanceof ServiceId
        ? account
        : ServiceId.parseFromServiceIdFixedWidthBinary(account);
    this.missingDevices = missingDevices ?? [];
    this.extraDevices = extraDevices ?? [];
    this.staleDevices = staleDevices ?? [];
  }
}

export class LibZonaRosaErrorBase extends Error {
  public readonly code: ErrorCode;
  public readonly operation: string;
  readonly _addr?: string | Native.ProtocolAddress;

  constructor(
    message: string,
    name: keyof typeof ErrorCode | undefined,
    operation: string,
    extraProps?: Record<string, unknown>
  ) {
    super(message);
    // Include the dynamic check for `name in ErrorCode` in case there's a bug in the Rust code.
    if (name !== undefined && name in ErrorCode) {
      this.name = name;
      this.code = ErrorCode[name];
    } else {
      this.name = 'LibZonaRosaError';
      this.code = ErrorCode.Generic;
    }
    this.operation = operation;
    if (extraProps !== undefined) {
      Object.assign(this, extraProps);
    }

    // Maintains proper stack trace, where our error was thrown (only available on V8)
    //   via https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error
    if (Error.captureStackTrace) {
      Error.captureStackTrace(this);
    }
  }

  public get addr(): ProtocolAddress | string {
    switch (this.code) {
      case ErrorCode.UntrustedIdentity:
        return this._addr as string;
      case ErrorCode.InvalidRegistrationId:
        return ProtocolAddress._fromNativeHandle(
          this._addr as Native.ProtocolAddress
        );
      default:
        throw new TypeError(`cannot get address from this error (${this})`);
    }
  }

  public toString(): string {
    return `${this.name} - ${this.operation}: ${this.message}`;
  }

  /// Like `error.code === code`, but also providing access to any additional properties.
  public is<E extends ErrorCode>(
    code: E
  ): this is Extract<LibZonaRosaError, { code: E }> {
    return this.code === code;
  }

  /// Like `error instanceof LibZonaRosaErrorBase && error.code === code`, but all in one expression,
  /// and providing access to any additional properties.
  public static is<E extends ErrorCode>(
    error: unknown,
    code: E
  ): error is Extract<LibZonaRosaError, { code: E }> {
    if (error instanceof LibZonaRosaErrorBase) {
      return error.is(code);
    }
    return false;
  }
}

export type LibZonaRosaErrorCommon = Omit<LibZonaRosaErrorBase, 'addr'>;

export type GenericError = LibZonaRosaErrorCommon & {
  code: ErrorCode.Generic;
};

export type DuplicatedMessageError = LibZonaRosaErrorCommon & {
  code: ErrorCode.DuplicatedMessage;
};

export type SealedSenderSelfSendError = LibZonaRosaErrorCommon & {
  code: ErrorCode.SealedSenderSelfSend;
};

export type UntrustedIdentityError = LibZonaRosaErrorCommon & {
  code: ErrorCode.UntrustedIdentity;
  addr: string;
};

export type InvalidRegistrationIdError = LibZonaRosaErrorCommon & {
  code: ErrorCode.InvalidRegistrationId;
  addr: ProtocolAddress;
};

export type InvalidProtocolAddress = LibZonaRosaErrorCommon & {
  code: ErrorCode.InvalidProtocolAddress;
  name: string;
  deviceId: number;
};

export type VerificationFailedError = LibZonaRosaErrorCommon & {
  code: ErrorCode.VerificationFailed;
};

export type InvalidSessionError = LibZonaRosaErrorCommon & {
  code: ErrorCode.InvalidSession;
};

export type InvalidSenderKeySessionError = LibZonaRosaErrorCommon & {
  code: ErrorCode.InvalidSenderKeySession;
  distributionId: string;
};

export type NicknameCannotBeEmptyError = LibZonaRosaErrorCommon & {
  code: ErrorCode.NicknameCannotBeEmpty;
};
export type CannotStartWithDigitError = LibZonaRosaErrorCommon & {
  code: ErrorCode.CannotStartWithDigit;
};
export type MissingSeparatorError = LibZonaRosaErrorCommon & {
  code: ErrorCode.MissingSeparator;
};

export type BadNicknameCharacterError = LibZonaRosaErrorCommon & {
  code: ErrorCode.BadNicknameCharacter;
};

export type NicknameTooShortError = LibZonaRosaErrorCommon & {
  code: ErrorCode.NicknameTooShort;
};

export type NicknameTooLongError = LibZonaRosaErrorCommon & {
  code: ErrorCode.NicknameTooLong;
};

export type DiscriminatorCannotBeEmptyError = LibZonaRosaErrorCommon & {
  code: ErrorCode.DiscriminatorCannotBeEmpty;
};
export type DiscriminatorCannotBeZeroError = LibZonaRosaErrorCommon & {
  code: ErrorCode.DiscriminatorCannotBeZero;
};
export type DiscriminatorCannotBeSingleDigitError = LibZonaRosaErrorCommon & {
  code: ErrorCode.DiscriminatorCannotBeSingleDigit;
};
export type DiscriminatorCannotHaveLeadingZerosError = LibZonaRosaErrorCommon & {
  code: ErrorCode.DiscriminatorCannotHaveLeadingZeros;
};
export type BadDiscriminatorCharacterError = LibZonaRosaErrorCommon & {
  code: ErrorCode.BadDiscriminatorCharacter;
};
export type DiscriminatorTooLargeError = LibZonaRosaErrorCommon & {
  code: ErrorCode.DiscriminatorTooLarge;
};

export type InputDataTooLong = LibZonaRosaErrorCommon & {
  code: ErrorCode.InputDataTooLong;
};

export type InvalidEntropyDataLength = LibZonaRosaErrorCommon & {
  code: ErrorCode.InvalidEntropyDataLength;
};

export type InvalidUsernameLinkEncryptedData = LibZonaRosaErrorCommon & {
  code: ErrorCode.InvalidUsernameLinkEncryptedData;
};

export type IoError = LibZonaRosaErrorCommon & {
  code: ErrorCode.IoError;
};

export type CdsiInvalidTokenError = LibZonaRosaErrorCommon & {
  code: ErrorCode.CdsiInvalidToken;
};

export type InvalidUriError = LibZonaRosaErrorCommon & {
  code: ErrorCode.InvalidUri;
};

export type InvalidMediaInputError = LibZonaRosaErrorCommon & {
  code: ErrorCode.InvalidMediaInput;
};

export type UnsupportedMediaInputError = LibZonaRosaErrorCommon & {
  code: ErrorCode.UnsupportedMediaInput;
};

export type RateLimitedError = LibZonaRosaErrorBase & {
  code: ErrorCode.RateLimitedError;
  readonly retryAfterSecs: number;
};

export type RateLimitChallengeError = LibZonaRosaErrorBase & {
  code: ErrorCode.RateLimitChallengeError;
  readonly token: string;
  readonly options: Set<'pushChallenge' | 'captcha'>;
};

export type ChatServiceInactive = LibZonaRosaErrorBase & {
  code: ErrorCode.ChatServiceInactive;
};

export type AppExpiredError = LibZonaRosaErrorBase & {
  code: ErrorCode.AppExpired;
};

export type DeviceDelinkedError = LibZonaRosaErrorBase & {
  code: ErrorCode.DeviceDelinked;
};

export type ConnectionInvalidatedError = LibZonaRosaErrorBase & {
  code: ErrorCode.ConnectionInvalidated;
};

export type ConnectedElsewhereError = LibZonaRosaErrorBase & {
  code: ErrorCode.ConnectedElsewhere;
};

export type PossibleCaptiveNetworkError = LibZonaRosaErrorBase & {
  code: ErrorCode.PossibleCaptiveNetwork;
};

export type SvrDataMissingError = LibZonaRosaErrorBase & {
  code: ErrorCode.SvrDataMissing;
};

export type SvrRequestFailedError = LibZonaRosaErrorCommon & {
  code: ErrorCode.SvrRequestFailed;
};

export type SvrRestoreFailedError = LibZonaRosaErrorCommon & {
  code: ErrorCode.SvrRestoreFailed;
  readonly triesRemaining: number;
};

export type SvrAttestationError = LibZonaRosaErrorCommon & {
  code: ErrorCode.SvrAttestationError;
};

export type SvrInvalidDataError = LibZonaRosaErrorCommon & {
  code: ErrorCode.SvrInvalidData;
};

export type BackupValidationError = LibZonaRosaErrorCommon & {
  code: ErrorCode.BackupValidation;
  readonly unknownFields: ReadonlyArray<string>;
};

export type CancellationError = LibZonaRosaErrorCommon & {
  code: ErrorCode.Cancelled;
};

export type KeyTransparencyError = LibZonaRosaErrorCommon & {
  code: ErrorCode.KeyTransparencyError;
};

export type KeyTransparencyVerificationFailed = LibZonaRosaErrorCommon & {
  code: ErrorCode.KeyTransparencyVerificationFailed;
};

export type IncrementalMacVerificationFailed = LibZonaRosaErrorCommon & {
  code: ErrorCode.IncrementalMacVerificationFailed;
};

export type RequestUnauthorizedError = LibZonaRosaErrorCommon & {
  code: ErrorCode.RequestUnauthorized;
};

export type MismatchedDevicesError = LibZonaRosaErrorCommon & {
  code: ErrorCode.MismatchedDevices;
  readonly entries: MismatchedDevicesEntry[];
};

export type LibZonaRosaError =
  | GenericError
  | DuplicatedMessageError
  | SealedSenderSelfSendError
  | UntrustedIdentityError
  | InvalidRegistrationIdError
  | InvalidProtocolAddress
  | VerificationFailedError
  | InvalidSessionError
  | InvalidSenderKeySessionError
  | NicknameCannotBeEmptyError
  | CannotStartWithDigitError
  | MissingSeparatorError
  | BadNicknameCharacterError
  | NicknameTooShortError
  | NicknameTooLongError
  | DiscriminatorCannotBeEmptyError
  | DiscriminatorCannotBeZeroError
  | DiscriminatorCannotBeSingleDigitError
  | DiscriminatorCannotHaveLeadingZerosError
  | BadDiscriminatorCharacterError
  | DiscriminatorTooLargeError
  | InputDataTooLong
  | InvalidEntropyDataLength
  | InvalidUsernameLinkEncryptedData
  | IoError
  | CdsiInvalidTokenError
  | InvalidUriError
  | InvalidMediaInputError
  | SvrDataMissingError
  | SvrRestoreFailedError
  | SvrRequestFailedError
  | SvrAttestationError
  | SvrInvalidDataError
  | UnsupportedMediaInputError
  | ChatServiceInactive
  | AppExpiredError
  | DeviceDelinkedError
  | ConnectionInvalidatedError
  | ConnectedElsewhereError
  | PossibleCaptiveNetworkError
  | RateLimitedError
  | RateLimitChallengeError
  | BackupValidationError
  | CancellationError
  | KeyTransparencyError
  | KeyTransparencyVerificationFailed
  | IncrementalMacVerificationFailed
  | RequestUnauthorizedError
  | MismatchedDevicesError;
