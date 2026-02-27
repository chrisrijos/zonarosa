//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

use std::borrow::Cow;
use std::fmt;

use assert_matches::assert_matches;
use attest::enclave::Error as EnclaveError;
use attest::hsm_enclave::Error as HsmEnclaveError;
use device_transfer::Error as DeviceTransferError;
use libzonarosa_account_keys::Error as PinError;
use libzonarosa_net::infra::errors::{LogSafeDisplay, TransportConnectError};
use libzonarosa_net::infra::ws::WebSocketConnectError;
use libzonarosa_net_chat::api::RateLimitChallenge;
use libzonarosa_net_chat::api::keytrans::Error as KeyTransError;
use libzonarosa_net_chat::api::messages::MismatchedDeviceError;
use libzonarosa_net_chat::api::registration::{RegistrationLock, VerificationCodeNotDeliverable};
use libzonarosa_protocol::*;
use zonarosa_crypto::Error as ZonaRosaCryptoError;
use usernames::{UsernameError, UsernameLinkError};
use zkgroup::{ZkGroupDeserializationFailure, ZkGroupVerificationFailure};

use super::{FutureCancelled, NullPointerError, UnexpectedPanic};
use crate::support::{IllegalArgumentError, WithContext, describe_panic};

#[derive(Debug, Clone, Copy)]
#[repr(C)]
pub enum ZonaRosaErrorCode {
    #[allow(dead_code)]
    UnknownError = 1,
    InvalidState = 2,
    InternalError = 3,
    NullParameter = 4,
    InvalidArgument = 5,
    InvalidType = 6,
    InvalidUtf8String = 7,
    Cancelled = 8,

    ProtobufError = 10,

    LegacyCiphertextVersion = 21,
    UnknownCiphertextVersion = 22,
    UnrecognizedMessageVersion = 23,

    InvalidMessage = 30,
    SealedSenderSelfSend = 31,

    InvalidKey = 40,
    InvalidSignature = 41,
    InvalidAttestationData = 42,

    FingerprintVersionMismatch = 51,
    FingerprintParsingError = 52,

    UntrustedIdentity = 60,

    InvalidKeyIdentifier = 70,

    SessionNotFound = 80,
    InvalidRegistrationId = 81,
    InvalidSession = 82,
    InvalidSenderKeySession = 83,
    InvalidProtocolAddress = 84,

    DuplicatedMessage = 90,

    CallbackError = 100,

    VerificationFailure = 110,

    UsernameCannotBeEmpty = 120,
    UsernameCannotStartWithDigit = 121,
    UsernameMissingSeparator = 122,
    UsernameBadDiscriminatorCharacter = 123,
    UsernameBadNicknameCharacter = 124,
    UsernameTooShort = 125,
    UsernameTooLong = 126,
    UsernameLinkInvalidEntropyDataLength = 127,
    UsernameLinkInvalid = 128,

    UsernameDiscriminatorCannotBeEmpty = 130,
    UsernameDiscriminatorCannotBeZero = 131,
    UsernameDiscriminatorCannotBeSingleDigit = 132,
    UsernameDiscriminatorCannotHaveLeadingZeros = 133,
    UsernameDiscriminatorTooLarge = 134,

    IoError = 140,
    #[allow(dead_code)]
    InvalidMediaInput = 141,
    #[allow(dead_code)]
    UnsupportedMediaInput = 142,

    ConnectionTimedOut = 143,
    NetworkProtocol = 144,
    RateLimited = 145,
    WebSocket = 146,
    CdsiInvalidToken = 147,
    ConnectionFailed = 148,
    ChatServiceInactive = 149,
    RequestTimedOut = 150,
    RateLimitChallenge = 151,
    PossibleCaptiveNetwork = 152,

    SvrDataMissing = 160,
    SvrRestoreFailed = 161,
    SvrRotationMachineTooManySteps = 162,
    SvrRequestFailed = 163,

    AppExpired = 170,
    DeviceDeregistered = 171,
    ConnectionInvalidated = 172,
    ConnectedElsewhere = 173,

    BackupValidation = 180,

    RegistrationInvalidSessionId = 190,
    RegistrationUnknown = 192,
    RegistrationSessionNotFound = 193,
    RegistrationNotReadyForVerification = 194,
    RegistrationSendVerificationCodeFailed = 195,
    RegistrationCodeNotDeliverable = 196,
    RegistrationSessionUpdateRejected = 197,
    RegistrationCredentialsCouldNotBeParsed = 198,
    RegistrationDeviceTransferPossible = 199,
    RegistrationRecoveryVerificationFailed = 200,
    RegistrationLock = 201,

    KeyTransparencyError = 210,
    KeyTransparencyVerificationFailed = 211,

    RequestUnauthorized = 220,
    MismatchedDevices = 221,
}

pub trait UpcastAsAny {
    fn upcast_as_any(&self) -> &dyn std::any::Any;
}
impl<T: std::any::Any> UpcastAsAny for T {
    fn upcast_as_any(&self) -> &dyn std::any::Any {
        self
    }
}

/// Error returned when asking for an attribute of an error that doesn't support that attribute.
pub struct WrongErrorKind;

pub struct FingerprintVersions {
    pub theirs: u32,
    pub ours: u32,
}

/// A trait for *bridged* error representations specifically.
///
/// This should be used for any errors that need to provide additional properties *themselves,* but
/// if they merely delegate to another error, or have no additional properties at all, prefer
/// implementing [`IntoFfiError`] instead, using [`SimpleError`] for the cases that don't need
/// special handling.
#[allow(rustdoc::private_intra_doc_links)]
pub trait FfiError: UpcastAsAny + fmt::Debug + Send + 'static {
    fn describe(&self) -> Cow<'_, str>;
    fn code(&self) -> ZonaRosaErrorCode;

    fn provide_address(&self) -> Result<ProtocolAddress, WrongErrorKind> {
        Err(WrongErrorKind)
    }
    fn provide_uuid(&self) -> Result<uuid::Uuid, WrongErrorKind> {
        Err(WrongErrorKind)
    }
    fn provide_invalid_address(&self) -> Result<(&str, u32), WrongErrorKind> {
        Err(WrongErrorKind)
    }
    fn provide_retry_after_seconds(&self) -> Result<u32, WrongErrorKind> {
        Err(WrongErrorKind)
    }
    fn provide_tries_remaining(&self) -> Result<u32, WrongErrorKind> {
        Err(WrongErrorKind)
    }
    fn provide_unknown_fields(&self) -> Result<Vec<String>, WrongErrorKind> {
        Err(WrongErrorKind)
    }
    fn provide_registration_code_not_deliverable(
        &self,
    ) -> Result<&VerificationCodeNotDeliverable, WrongErrorKind> {
        Err(WrongErrorKind)
    }
    fn provide_registration_lock(&self) -> Result<&RegistrationLock, WrongErrorKind> {
        Err(WrongErrorKind)
    }
    fn provide_rate_limit_challenge(&self) -> Result<&RateLimitChallenge, WrongErrorKind> {
        Err(WrongErrorKind)
    }
    fn provide_fingerprint_versions(&self) -> Result<FingerprintVersions, WrongErrorKind> {
        Err(WrongErrorKind)
    }
    fn provide_mismatched_device_errors(&self) -> Result<&[MismatchedDeviceError], WrongErrorKind> {
        Err(WrongErrorKind)
    }
}

/// An [`FfiError`] that only has a code and message.
#[derive(Debug)]
struct SimpleError {
    code: ZonaRosaErrorCode,
    message: Cow<'static, str>,
}

impl FfiError for SimpleError {
    fn describe(&self) -> Cow<'_, str> {
        Cow::Borrowed(&self.message)
    }

    fn code(&self) -> ZonaRosaErrorCode {
        self.code
    }
}

impl SimpleError {
    fn new(code: ZonaRosaErrorCode, message: impl Into<Cow<'static, str>>) -> Self {
        Self {
            code,
            message: message.into(),
        }
    }
}

/// The top-level error type (opaquely) returned to C clients when something goes wrong.
///
/// Ideally this would use [ThinBox][], and then we wouldn't need an extra level of indirection when
/// returning it to C, but unfortunately that isn't stable yet.
///
/// [ThinBox]: https://doc.rust-lang.org/std/boxed/struct.ThinBox.html
#[derive(Debug)]
pub struct ZonaRosaFfiError(Box<dyn FfiError + Send>);

impl ZonaRosaFfiError {
    pub fn downcast_ref<T: FfiError>(&self) -> Option<&T> {
        (*self.0).upcast_as_any().downcast_ref()
    }

    #[cold]
    pub fn into_raw_box_for_ffi(self) -> *mut Self {
        Box::into_raw(Box::new(self))
    }
}

/// ZonaRosaFfiError is a typed wrapper around a Box, and as such it's reasonable for it to have the
/// same Deref behavior as a Box. All the interesting functionality is present on the [`FfiError`]
/// trait.
impl std::ops::Deref for ZonaRosaFfiError {
    type Target = dyn FfiError;

    fn deref(&self) -> &Self::Target {
        &*self.0
    }
}

impl fmt::Display for ZonaRosaFfiError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.write_str(&self.0.describe())
    }
}

/// A slightly more convenient version of `From`/`Into` for [`ZonaRosaFfiError`].
///
/// We have a lot of errors, so the convenience is worth it. The main improvement is that the return
/// type only has to make it to the `FfiError` state.
pub trait IntoFfiError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError>;
}

impl<T: FfiError> IntoFfiError for T {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        ZonaRosaFfiError(Box::new(self))
    }
}

impl FfiError for std::convert::Infallible {
    fn describe(&self) -> Cow<'_, str> {
        match *self {}
    }

    fn code(&self) -> ZonaRosaErrorCode {
        match *self {}
    }
}

impl<T: IntoFfiError> From<T> for ZonaRosaFfiError {
    fn from(value: T) -> Self {
        value.into_ffi_error().into()
    }
}

impl IntoFfiError for IllegalArgumentError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        SimpleError::new(ZonaRosaErrorCode::InvalidArgument, self.0)
    }
}

#[derive(Debug)]
struct InvalidRegistrationId {
    peer_addr: ProtocolAddress,
    invalid_id: u32,
}

impl FfiError for InvalidRegistrationId {
    fn describe(&self) -> Cow<'_, str> {
        format!(
            "session for {} has invalid registration ID {:X}",
            self.peer_addr, self.invalid_id
        )
        .into()
    }

    fn code(&self) -> ZonaRosaErrorCode {
        ZonaRosaErrorCode::InvalidRegistrationId
    }

    fn provide_address(&self) -> Result<ProtocolAddress, WrongErrorKind> {
        Ok(self.peer_addr.clone())
    }
}

#[derive(Debug)]
struct InvalidSenderKeySession {
    distribution_id: uuid::Uuid,
}

impl FfiError for InvalidSenderKeySession {
    fn describe(&self) -> Cow<'_, str> {
        format!(
            "invalid sender key session with distribution ID {}",
            self.distribution_id
        )
        .into()
    }

    fn code(&self) -> ZonaRosaErrorCode {
        ZonaRosaErrorCode::InvalidSenderKeySession
    }

    fn provide_uuid(&self) -> Result<uuid::Uuid, WrongErrorKind> {
        Ok(self.distribution_id)
    }
}

#[derive(Debug)]
struct InvalidProtocolAddress {
    name: String,
    device_id: u32,
}

impl FfiError for InvalidProtocolAddress {
    fn describe(&self) -> Cow<'_, str> {
        format!(
            "protocol address is invalid: {}.{}",
            self.name, self.device_id,
        )
        .into()
    }

    fn code(&self) -> ZonaRosaErrorCode {
        ZonaRosaErrorCode::InvalidProtocolAddress
    }

    fn provide_invalid_address(&self) -> Result<(&str, u32), WrongErrorKind> {
        Ok((&self.name, self.device_id))
    }
}

impl IntoFfiError for ZonaRosaProtocolError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let code = match &self {
            &Self::InvalidSenderKeySession { distribution_id } => {
                return ZonaRosaFfiError::from(InvalidSenderKeySession { distribution_id });
            }
            Self::InvalidRegistrationId(_, _) => {
                // Re-match as owned.
                return assert_matches!(
                    self,
                    Self::InvalidRegistrationId(peer_addr, invalid_id) =>
                    InvalidRegistrationId {
                        peer_addr, invalid_id
                    }
                )
                .into();
            }
            Self::InvalidProtocolAddress { .. } => {
                // Re-match as owned.
                return assert_matches!(
                    self,
                    Self::InvalidProtocolAddress { name, device_id } =>
                    InvalidProtocolAddress {
                        name, device_id
                    }
                )
                .into();
            }

            Self::InvalidArgument(_) => ZonaRosaErrorCode::InvalidArgument,
            Self::InvalidState(_, _) => ZonaRosaErrorCode::InvalidState,
            Self::InvalidProtobufEncoding => ZonaRosaErrorCode::ProtobufError,
            Self::CiphertextMessageTooShort(_)
            | Self::InvalidMessage(_, _)
            | Self::InvalidSealedSenderMessage(_)
            | Self::BadKEMCiphertextLength(_, _) => ZonaRosaErrorCode::InvalidMessage,
            Self::LegacyCiphertextVersion(_) => ZonaRosaErrorCode::LegacyCiphertextVersion,
            Self::UnrecognizedCiphertextVersion(_) => ZonaRosaErrorCode::UnknownCiphertextVersion,
            Self::UnrecognizedMessageVersion(_) | Self::UnknownSealedSenderVersion(_) => {
                ZonaRosaErrorCode::UnrecognizedMessageVersion
            }
            Self::NoKeyTypeIdentifier
            | Self::BadKeyType(_)
            | Self::BadKeyLength(_, _)
            | Self::InvalidKeyAgreement
            | Self::InvalidMacKeyLength(_)
            | Self::BadKEMKeyType(_)
            | Self::WrongKEMKeyType(_, _)
            | Self::BadKEMKeyLength(_, _) => ZonaRosaErrorCode::InvalidKey,
            Self::SignatureValidationFailed => ZonaRosaErrorCode::InvalidSignature,
            Self::UntrustedIdentity(_) => ZonaRosaErrorCode::UntrustedIdentity,
            Self::InvalidPreKeyId | Self::InvalidSignedPreKeyId | Self::InvalidKyberPreKeyId => {
                ZonaRosaErrorCode::InvalidKeyIdentifier
            }
            Self::NoSenderKeyState { .. } | Self::SessionNotFound(_) => {
                ZonaRosaErrorCode::SessionNotFound
            }
            Self::InvalidSessionStructure(_) => ZonaRosaErrorCode::InvalidSession,
            Self::DuplicatedMessage(_, _) => ZonaRosaErrorCode::DuplicatedMessage,
            Self::FfiBindingError(_) => ZonaRosaErrorCode::InternalError,
            Self::ApplicationCallbackError(_, _) => ZonaRosaErrorCode::CallbackError,
            Self::SealedSenderSelfSend => ZonaRosaErrorCode::SealedSenderSelfSend,
            Self::UnknownSealedSenderServerCertificateId(_) => ZonaRosaErrorCode::VerificationFailure,
        };

        SimpleError::new(code, self.to_string()).into()
    }
}

impl FfiError for libzonarosa_protocol::FingerprintError {
    fn describe(&self) -> Cow<'_, str> {
        self.to_string().into()
    }

    fn code(&self) -> ZonaRosaErrorCode {
        match self {
            Self::VersionMismatch { .. } => ZonaRosaErrorCode::FingerprintVersionMismatch,
            Self::ParsingError(_) => ZonaRosaErrorCode::FingerprintParsingError,
            Self::InvalidIterationCount(_) => ZonaRosaErrorCode::InvalidArgument,
        }
    }

    fn provide_fingerprint_versions(&self) -> Result<FingerprintVersions, WrongErrorKind> {
        match self {
            &Self::VersionMismatch { theirs, ours } => Ok(FingerprintVersions { theirs, ours }),
            _ => Err(WrongErrorKind),
        }
    }
}

impl IntoFfiError for DeviceTransferError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let code = match self {
            Self::KeyDecodingFailed => ZonaRosaErrorCode::InvalidKey,
            Self::InternalError(_) => ZonaRosaErrorCode::InternalError,
        };
        SimpleError::new(code, format!("Device transfer operation failed: {self}"))
    }
}

impl IntoFfiError for HsmEnclaveError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let code = match self {
            Self::HSMCommunicationError(_) | Self::HSMHandshakeError(_) => {
                ZonaRosaErrorCode::InvalidMessage
            }
            Self::TrustedCodeError => ZonaRosaErrorCode::UntrustedIdentity,
            Self::InvalidPublicKeyError => ZonaRosaErrorCode::InvalidKey,
            Self::InvalidCodeHashError => ZonaRosaErrorCode::InvalidArgument,
            Self::InvalidBridgeStateError => ZonaRosaErrorCode::InvalidState,
        };
        SimpleError::new(code, format!("HSM enclave operation failed: {self}"))
    }
}

impl IntoFfiError for EnclaveError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let message = format!("SGX operation failed: {self}");
        let code = match self {
            Self::AttestationError(_) | Self::NoiseError(_) | Self::NoiseHandshakeError(_) => {
                ZonaRosaErrorCode::InvalidMessage
            }
            Self::AttestationDataError { .. } => ZonaRosaErrorCode::InvalidAttestationData,
            Self::InvalidBridgeStateError => ZonaRosaErrorCode::InvalidState,
        };
        SimpleError::new(code, message)
    }
}

impl IntoFfiError for PinError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let code = match self {
            Self::Argon2Error(_) | Self::DecodingError(_) | Self::MrenclaveLookupError => {
                ZonaRosaErrorCode::InvalidArgument
            }
        };
        SimpleError::new(code, self.to_string())
    }
}

impl IntoFfiError for ZonaRosaCryptoError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let code = match self {
            Self::UnknownAlgorithm(_, _)
            | Self::InvalidKeySize
            | Self::InvalidNonceSize
            | Self::InvalidInputSize => ZonaRosaErrorCode::InvalidArgument,
            Self::InvalidTag => ZonaRosaErrorCode::InvalidMessage,
        };
        SimpleError::new(code, format!("Cryptographic operation failed: {self}"))
    }
}

impl IntoFfiError for ZkGroupVerificationFailure {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        SimpleError::new(ZonaRosaErrorCode::VerificationFailure, self.to_string())
    }
}

impl IntoFfiError for ZkGroupDeserializationFailure {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        SimpleError::new(ZonaRosaErrorCode::InvalidType, self.to_string())
    }
}

impl IntoFfiError for UsernameError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let code = match self {
            Self::MissingSeparator => ZonaRosaErrorCode::UsernameMissingSeparator,
            Self::NicknameCannotBeEmpty => ZonaRosaErrorCode::UsernameCannotBeEmpty,
            Self::NicknameCannotStartWithDigit => ZonaRosaErrorCode::UsernameCannotStartWithDigit,
            Self::BadNicknameCharacter => ZonaRosaErrorCode::UsernameBadNicknameCharacter,
            Self::NicknameTooShort => ZonaRosaErrorCode::UsernameTooShort,
            Self::NicknameTooLong => ZonaRosaErrorCode::UsernameTooLong,
            Self::DiscriminatorCannotBeEmpty => ZonaRosaErrorCode::UsernameDiscriminatorCannotBeEmpty,
            Self::DiscriminatorCannotBeZero => ZonaRosaErrorCode::UsernameDiscriminatorCannotBeZero,
            Self::DiscriminatorCannotBeSingleDigit => {
                ZonaRosaErrorCode::UsernameDiscriminatorCannotBeSingleDigit
            }
            Self::DiscriminatorCannotHaveLeadingZeros => {
                ZonaRosaErrorCode::UsernameDiscriminatorCannotHaveLeadingZeros
            }
            Self::BadDiscriminatorCharacter => ZonaRosaErrorCode::UsernameBadDiscriminatorCharacter,
            Self::DiscriminatorTooLarge => ZonaRosaErrorCode::UsernameDiscriminatorTooLarge,
        };
        SimpleError::new(code, self.to_string())
    }
}

impl IntoFfiError for usernames::ProofVerificationFailure {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        SimpleError::new(ZonaRosaErrorCode::VerificationFailure, self.to_string())
    }
}

impl IntoFfiError for UsernameLinkError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let code = match self {
            Self::InputDataTooLong => ZonaRosaErrorCode::UsernameTooLong,
            Self::InvalidEntropyDataLength => ZonaRosaErrorCode::UsernameLinkInvalidEntropyDataLength,
            Self::UsernameLinkDataTooShort
            | Self::HmacMismatch
            | Self::BadCiphertext
            | Self::InvalidDecryptedDataStructure => ZonaRosaErrorCode::UsernameLinkInvalid,
        };
        SimpleError::new(code, self.to_string())
    }
}

impl IntoFfiError for std::io::Error {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        // Special case: if the error being boxed is an IoError containing a ZonaRosaProtocolError,
        // extract the ZonaRosaProtocolError up front.
        match self.downcast::<ZonaRosaProtocolError>() {
            Ok(inner) => inner.into_ffi_error().into(),
            Err(original) => {
                SimpleError::new(ZonaRosaErrorCode::IoError, format!("IO error: {original}")).into()
            }
        }
    }
}

impl IntoFfiError for libzonarosa_net::cdsi::LookupError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let result: ZonaRosaFfiError = match self {
            Self::CdsiProtocol(_) | Self::EnclaveProtocol(_) | Self::Server { .. } => {
                SimpleError::new(
                    ZonaRosaErrorCode::NetworkProtocol,
                    format!("Protocol error: {self}"),
                )
                .into()
            }
            Self::AttestationError(inner) => inner.into(),
            Self::RateLimited(inner) => inner.into(),
            Self::InvalidToken => SimpleError::new(
                ZonaRosaErrorCode::CdsiInvalidToken,
                "CDSI request token was invalid",
            )
            .into(),
            Self::ConnectTransport(e) => {
                SimpleError::new(ZonaRosaErrorCode::IoError, format!("IO error: {e}")).into()
            }
            Self::WebSocket(e) => {
                SimpleError::new(ZonaRosaErrorCode::WebSocket, format!("WebSocket error: {e}")).into()
            }
            Self::AllConnectionAttemptsFailed => SimpleError::new(
                ZonaRosaErrorCode::ConnectionFailed,
                "No connection attempts succeeded before timeout",
            )
            .into(),
            Self::InvalidArgument { .. } => SimpleError::new(
                ZonaRosaErrorCode::InvalidArgument,
                format!("invalid argument: {self}"),
            )
            .into(),
        };
        result
    }
}

impl IntoFfiError for libzonarosa_net::chat::ConnectError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        match self {
            // Special case for self-signed certs, in case the app wants to tell the user to switch
            // networks.
            Self::WebSocket(WebSocketConnectError::Transport(
                ref e @ TransportConnectError::SslFailedHandshake(ref reason),
            )) if reason.is_possible_captive_network() => {
                SimpleError::new(ZonaRosaErrorCode::PossibleCaptiveNetwork, e.to_string()).into()
            }
            Self::WebSocket(e) => {
                SimpleError::new(ZonaRosaErrorCode::WebSocket, format!("WebSocket error: {e}")).into()
            }
            Self::AllAttemptsFailed { .. } | Self::InvalidConnectionConfiguration => {
                SimpleError::new(ZonaRosaErrorCode::ConnectionFailed, "Connection failed").into()
            }
            Self::Timeout => {
                SimpleError::new(ZonaRosaErrorCode::ConnectionTimedOut, "Connect timed out").into()
            }
            Self::AppExpired => SimpleError::new(ZonaRosaErrorCode::AppExpired, "App expired").into(),
            Self::DeviceDeregistered => SimpleError::new(
                ZonaRosaErrorCode::DeviceDeregistered,
                "Device deregistered or delinked",
            )
            .into(),
            Self::RetryLater(retry_later) => retry_later.into_ffi_error().into(),
        }
    }
}

impl FfiError for libzonarosa_net::infra::errors::RetryLater {
    fn code(&self) -> ZonaRosaErrorCode {
        ZonaRosaErrorCode::RateLimited
    }

    fn describe(&self) -> Cow<'_, str> {
        let Self {
            retry_after_seconds,
        } = self;
        format!("Rate limited; try again after {retry_after_seconds}s").into()
    }

    fn provide_retry_after_seconds(&self) -> Result<u32, WrongErrorKind> {
        Ok(self.retry_after_seconds)
    }
}

impl IntoFfiError for libzonarosa_net::chat::SendError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        match self {
            Self::WebSocket(e) => {
                SimpleError::new(ZonaRosaErrorCode::WebSocket, format!("WebSocket error: {e}"))
            }
            Self::IncomingDataInvalid => SimpleError::new(
                ZonaRosaErrorCode::NetworkProtocol,
                format!("Protocol error: {self}"),
            ),
            Self::RequestHasInvalidHeader => SimpleError::new(
                ZonaRosaErrorCode::InternalError,
                format!("internal error: {self}"),
            ),
            Self::RequestTimedOut => {
                SimpleError::new(ZonaRosaErrorCode::RequestTimedOut, "Request timed out")
            }
            Self::Disconnected => SimpleError::new(
                ZonaRosaErrorCode::ChatServiceInactive,
                "Chat service disconnected",
            ),
            Self::ConnectionInvalidated => SimpleError::new(
                ZonaRosaErrorCode::ConnectionInvalidated,
                "Connection invalidated",
            ),
            Self::ConnectedElsewhere => {
                SimpleError::new(ZonaRosaErrorCode::ConnectedElsewhere, "Connected elsewhere")
            }
        }
    }
}

impl<E: IntoFfiError> IntoFfiError
    for libzonarosa_net_chat::api::RequestError<E, libzonarosa_net_chat::api::DisconnectedError>
where
    libzonarosa_net_chat::api::RequestError<E>: std::fmt::Display,
{
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        match self {
            Self::Timeout => ZonaRosaFfiError::from(SimpleError::new(
                ZonaRosaErrorCode::RequestTimedOut,
                self.to_string(),
            )),
            Self::Unexpected { log_safe: _ } => {
                SimpleError::new(ZonaRosaErrorCode::NetworkProtocol, self.to_string()).into()
            }
            Self::ServerSideError => {
                // TODO: "IO error" isn't really apt at all, but it is an existing error code that
                // the iOS app considers retryable.
                SimpleError::new(ZonaRosaErrorCode::IoError, self.to_string()).into()
            }
            Self::Other(err) => err.into_ffi_error().into(),
            Self::RetryLater(retry_later) => retry_later.into(),
            Self::Challenge(challenge) => challenge.into(),
            Self::Disconnected(d) => d.into_ffi_error().into(),
        }
    }
}

impl FfiError for libzonarosa_net_chat::api::messages::MultiRecipientSendFailure {
    fn describe(&self) -> Cow<'_, str> {
        self.to_string().into()
    }

    fn code(&self) -> ZonaRosaErrorCode {
        match self {
            Self::Unauthorized => ZonaRosaErrorCode::RequestUnauthorized,
            Self::MismatchedDevices(_) => ZonaRosaErrorCode::MismatchedDevices,
        }
    }

    fn provide_mismatched_device_errors(&self) -> Result<&[MismatchedDeviceError], WrongErrorKind> {
        match self {
            Self::Unauthorized => Err(WrongErrorKind),
            Self::MismatchedDevices(mismatched_device_errors) => Ok(mismatched_device_errors),
        }
    }
}

impl IntoFfiError for libzonarosa_net_chat::api::DisconnectedError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let code = match self {
            Self::ConnectedElsewhere => ZonaRosaErrorCode::ConnectedElsewhere,
            Self::ConnectionInvalidated => ZonaRosaErrorCode::ConnectionInvalidated,
            Self::Transport { .. } => ZonaRosaErrorCode::NetworkProtocol,
            Self::Closed => ZonaRosaErrorCode::ChatServiceInactive,
        };
        SimpleError::new(code, self.to_string())
    }
}

impl IntoFfiError for KeyTransError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        let message = self.to_string();
        let code = match self {
            Self::VerificationFailed(libzonarosa_keytrans::Error::VerificationFailed(_)) => {
                ZonaRosaErrorCode::KeyTransparencyVerificationFailed
            }
            Self::VerificationFailed(_) | Self::InvalidResponse(_) | Self::InvalidRequest(_) => {
                ZonaRosaErrorCode::KeyTransparencyError
            }
        };
        SimpleError::new(code, message)
    }
}

impl FfiError for RateLimitChallenge {
    fn describe(&self) -> Cow<'_, str> {
        self.to_string().into()
    }

    fn code(&self) -> ZonaRosaErrorCode {
        ZonaRosaErrorCode::RateLimitChallenge
    }
    fn provide_rate_limit_challenge(&self) -> Result<&RateLimitChallenge, WrongErrorKind> {
        Ok(self)
    }
}

mod registration {
    use libzonarosa_net_chat::api::registration::{
        CheckSvr2CredentialsError, CreateSessionError, RegisterAccountError,
        RequestVerificationCodeError, ResumeSessionError, SubmitVerificationError,
        UpdateSessionError,
    };
    use libzonarosa_net_chat::registration::RequestError;

    use super::*;

    // We require Display but not LogSafeDisplay for `E` because we will only format other cases of
    // Self.
    impl<E> IntoFfiError for RequestError<E>
    where
        E: std::fmt::Display + IntoFfiError,
    {
        fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
            match self {
                RequestError::Timeout => ZonaRosaFfiError::from(SimpleError::new(
                    ZonaRosaErrorCode::RequestTimedOut,
                    self.to_string(),
                )),
                RequestError::ServerSideError | RequestError::Unexpected { log_safe: _ } => {
                    SimpleError::new(ZonaRosaErrorCode::RegistrationUnknown, self.to_string()).into()
                }
                RequestError::Other(err) => err.into_ffi_error().into(),
                RequestError::RetryLater(retry_later) => retry_later.into(),
                RequestError::Challenge(challenge) => challenge.into(),
                RequestError::Disconnected(d) => d.into_ffi_error().into(),
            }
        }
    }

    impl IntoFfiError for CreateSessionError
    where
        Self: LogSafeDisplay,
    {
        fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
            let code = match &self {
                Self::InvalidSessionId => ZonaRosaErrorCode::RegistrationInvalidSessionId,
            };
            SimpleError::new(code, self.to_string())
        }
    }

    impl IntoFfiError for ResumeSessionError
    where
        Self: LogSafeDisplay,
    {
        fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
            let code = match &self {
                Self::InvalidSessionId => ZonaRosaErrorCode::RegistrationInvalidSessionId,
                Self::SessionNotFound => ZonaRosaErrorCode::RegistrationSessionNotFound,
            };
            SimpleError::new(code, self.to_string())
        }
    }

    impl IntoFfiError for RequestVerificationCodeError
    where
        Self: LogSafeDisplay,
    {
        fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
            let code = match &self {
                Self::InvalidSessionId => ZonaRosaErrorCode::RegistrationInvalidSessionId,
                Self::SessionNotFound => ZonaRosaErrorCode::RegistrationSessionNotFound,
                Self::NotReadyForVerification => {
                    ZonaRosaErrorCode::RegistrationNotReadyForVerification
                }
                Self::SendFailed => ZonaRosaErrorCode::RegistrationSendVerificationCodeFailed,
                Self::CodeNotDeliverable(_) => {
                    // Re-match as owned.
                    return ZonaRosaFfiError::from(
                        assert_matches!(self, Self::CodeNotDeliverable(inner) => inner),
                    );
                }
            };
            SimpleError::new(code, self.to_string()).into()
        }
    }

    impl FfiError for VerificationCodeNotDeliverable {
        fn describe(&self) -> Cow<'_, str> {
            "the code could not be delivered".into()
        }

        fn code(&self) -> ZonaRosaErrorCode {
            ZonaRosaErrorCode::RegistrationCodeNotDeliverable
        }

        fn provide_registration_code_not_deliverable(
            &self,
        ) -> Result<&VerificationCodeNotDeliverable, WrongErrorKind> {
            Ok(self)
        }
    }

    impl IntoFfiError for UpdateSessionError
    where
        Self: LogSafeDisplay,
    {
        fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
            let code = match &self {
                Self::Rejected => ZonaRosaErrorCode::RegistrationSessionUpdateRejected,
            };
            SimpleError::new(code, self.to_string())
        }
    }

    impl IntoFfiError for SubmitVerificationError
    where
        Self: LogSafeDisplay,
    {
        fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
            let code = match &self {
                Self::InvalidSessionId => ZonaRosaErrorCode::RegistrationInvalidSessionId,
                Self::SessionNotFound => ZonaRosaErrorCode::RegistrationSessionNotFound,
                Self::NotReadyForVerification => {
                    ZonaRosaErrorCode::RegistrationNotReadyForVerification
                }
            };
            SimpleError::new(code, self.to_string())
        }
    }

    impl IntoFfiError for CheckSvr2CredentialsError
    where
        Self: LogSafeDisplay,
    {
        fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
            let code = match &self {
                Self::CredentialsCouldNotBeParsed => {
                    ZonaRosaErrorCode::RegistrationCredentialsCouldNotBeParsed
                }
            };
            SimpleError::new(code, self.to_string())
        }
    }

    impl IntoFfiError for RegisterAccountError
    where
        Self: LogSafeDisplay,
    {
        fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
            let code = match &self {
                Self::DeviceTransferIsPossibleButNotSkipped => {
                    ZonaRosaErrorCode::RegistrationDeviceTransferPossible
                }
                Self::RegistrationRecoveryVerificationFailed => {
                    ZonaRosaErrorCode::RegistrationRecoveryVerificationFailed
                }
                Self::RegistrationLock(_) => {
                    // Re-match as owned.
                    return ZonaRosaFfiError::from(
                        assert_matches!(self, Self::RegistrationLock(inner) => inner),
                    );
                }
            };
            SimpleError::new(code, self.to_string()).into()
        }
    }

    impl FfiError for RegistrationLock {
        fn describe(&self) -> Cow<'_, str> {
            "registration lock is enabled".into()
        }

        fn code(&self) -> ZonaRosaErrorCode {
            ZonaRosaErrorCode::RegistrationLock
        }

        fn provide_registration_lock(&self) -> Result<&RegistrationLock, WrongErrorKind> {
            Ok(self)
        }
    }
}

impl IntoFfiError for http::uri::InvalidUri {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        SimpleError::new(
            ZonaRosaErrorCode::InvalidArgument,
            format!("invalid argument: {self}"),
        )
    }
}

#[cfg(feature = "zonarosa-media")]
impl IntoFfiError for zonarosa_media::sanitize::mp4::Error {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        match self {
            Self::Io(e) => e.into_ffi_error().into(),
            Self::Parse(e) => {
                use zonarosa_media::sanitize::mp4::ParseError;
                let code = match e.kind {
                    ParseError::InvalidBoxLayout
                    | ParseError::InvalidInput
                    | ParseError::MissingRequiredBox { .. }
                    | ParseError::TruncatedBox => ZonaRosaErrorCode::InvalidMediaInput,

                    ParseError::UnsupportedBoxLayout
                    | ParseError::UnsupportedBox { .. }
                    | ParseError::UnsupportedFormat { .. } => {
                        ZonaRosaErrorCode::UnsupportedMediaInput
                    }
                };
                SimpleError::new(code, format!("Mp4 sanitizer failed to parse mp4 file: {e}"))
                    .into()
            }
        }
    }
}

#[cfg(feature = "zonarosa-media")]
impl IntoFfiError for zonarosa_media::sanitize::webp::Error {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        match self {
            Self::Io(e) => e.into_ffi_error().into(),
            Self::Parse(e) => {
                use zonarosa_media::sanitize::webp::ParseError;
                let code = match e.kind {
                    ParseError::InvalidChunkLayout
                    | ParseError::InvalidInput
                    | ParseError::InvalidVp8lPrefixCode
                    | ParseError::MissingRequiredChunk { .. }
                    | ParseError::TruncatedChunk => ZonaRosaErrorCode::InvalidMediaInput,

                    ParseError::UnsupportedChunk { .. }
                    | ParseError::UnsupportedVp8lVersion { .. } => {
                        ZonaRosaErrorCode::UnsupportedMediaInput
                    }
                };
                SimpleError::new(
                    code,
                    format!("WebP sanitizer failed to parse webp file: {e}"),
                )
                .into()
            }
        }
    }
}

impl FfiError for libzonarosa_message_backup::ReadError {
    fn describe(&self) -> Cow<'_, str> {
        self.to_string().into()
    }

    fn code(&self) -> ZonaRosaErrorCode {
        ZonaRosaErrorCode::BackupValidation
    }

    fn provide_unknown_fields(&self) -> Result<Vec<String>, WrongErrorKind> {
        Ok(self
            .found_unknown_fields
            .iter()
            .map(ToString::to_string)
            .collect())
    }
}

impl IntoFfiError for NullPointerError {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        SimpleError::new(ZonaRosaErrorCode::NullParameter, "null pointer")
    }
}

impl IntoFfiError for UnexpectedPanic {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        SimpleError::new(
            ZonaRosaErrorCode::InternalError,
            format!("unexpected panic: {}", describe_panic(&self.0)),
        )
    }
}

impl IntoFfiError for std::str::Utf8Error {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        SimpleError::new(ZonaRosaErrorCode::InvalidUtf8String, "invalid UTF8 string")
    }
}

impl IntoFfiError for FutureCancelled {
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        SimpleError::new(ZonaRosaErrorCode::Cancelled, "cancelled")
    }
}

#[derive(Debug)]
struct SvrRestoreFailed {
    tries_remaining: u32,
}

impl FfiError for SvrRestoreFailed {
    fn describe(&self) -> Cow<'_, str> {
        format!(
            "Failure to restore data; {} tries remaining",
            self.tries_remaining
        )
        .into()
    }

    fn code(&self) -> ZonaRosaErrorCode {
        ZonaRosaErrorCode::SvrRestoreFailed
    }

    fn provide_tries_remaining(&self) -> Result<u32, WrongErrorKind> {
        Ok(self.tries_remaining)
    }
}

impl IntoFfiError for libzonarosa_net::svrb::Error
where
    Self: LogSafeDisplay,
{
    fn into_ffi_error(self) -> impl Into<ZonaRosaFfiError> {
        use libzonarosa_net::infra::ws::WebSocketConnectError;
        match self {
            e @ Self::AllConnectionAttemptsFailed => {
                SimpleError::new(ZonaRosaErrorCode::ConnectionFailed, e.to_string()).into()
            }
            Self::Connect(e) => match e {
                WebSocketConnectError::Transport(e) => {
                    SimpleError::new(ZonaRosaErrorCode::IoError, format!("IO error: {e}")).into()
                }
                WebSocketConnectError::WebSocketError(e) => {
                    SimpleError::new(ZonaRosaErrorCode::WebSocket, format!("WebSocket error: {e}"))
                        .into()
                }
            },
            Self::RateLimited(inner) => inner.into_ffi_error().into(),
            Self::Service(e) => {
                SimpleError::new(ZonaRosaErrorCode::WebSocket, format!("WebSocket error: {e}")).into()
            }
            Self::AttestationError(inner) => inner.into_ffi_error().into(),
            e @ Self::Protocol(_) => {
                SimpleError::new(ZonaRosaErrorCode::NetworkProtocol, e.to_string()).into()
            }
            Self::RestoreFailed(tries_remaining) => SvrRestoreFailed { tries_remaining }.into(),
            e @ Self::DataMissing => {
                SimpleError::new(ZonaRosaErrorCode::SvrDataMissing, e.to_string()).into()
            }
            e @ (Self::PreviousBackupDataInvalid
            | Self::MetadataInvalid
            | Self::DecryptionError(_)) => {
                SimpleError::new(ZonaRosaErrorCode::InvalidArgument, e.to_string()).into()
            }
        }
    }
}

pub type ZonaRosaFfiResult<T> = Result<T, ZonaRosaFfiError>;

/// Represents an error returned by a callback, following the C conventions that 0 means "success".
#[derive(Debug)]
pub struct CallbackError {
    value: std::num::NonZeroI32,
}

impl CallbackError {
    /// Returns `Ok(())` if `value` is zero; otherwise, wraps the value in `Self` as an error.
    pub fn check(value: i32) -> Result<(), Self> {
        match std::num::NonZeroI32::try_from(value).ok() {
            None => Ok(()),
            Some(value) => Err(Self { value }),
        }
    }

    pub fn log_on_error(operation: &str, value: i32) {
        match Self::check(value) {
            Ok(()) => {}
            Err(value) => log::error!("failed '{operation}' with {value}"),
        }
    }
}

impl fmt::Display for CallbackError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "error code {}", self.value)
    }
}

impl std::error::Error for CallbackError {}

impl From<WithContext<CallbackError>> for ZonaRosaProtocolError {
    fn from(value: WithContext<CallbackError>) -> Self {
        let WithContext { operation, inner } = value;
        ZonaRosaProtocolError::for_application_callback(operation)(inner)
    }
}

/// This is overly general, but in practice is only used to handle errors converting callback
/// results.
impl From<WithContext<ZonaRosaFfiError>> for ZonaRosaProtocolError {
    fn from(value: WithContext<ZonaRosaFfiError>) -> Self {
        let WithContext {
            operation: _,
            inner,
        } = value;
        ZonaRosaProtocolError::FfiBindingError(inner.to_string())
    }
}

impl From<WithContext<CallbackError>> for std::io::Error {
    fn from(value: WithContext<CallbackError>) -> Self {
        std::io::Error::other(ZonaRosaProtocolError::from(value))
    }
}

/// This is overly general, but in practice is only used to handle errors converting callback
/// results.
impl From<WithContext<ZonaRosaFfiError>> for std::io::Error {
    fn from(value: WithContext<ZonaRosaFfiError>) -> Self {
        let WithContext {
            operation: _,
            inner,
        } = value;
        std::io::Error::other(inner.to_string())
    }
}
