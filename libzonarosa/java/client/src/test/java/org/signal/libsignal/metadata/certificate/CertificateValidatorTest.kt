//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata.certificate

import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey

class CertificateValidatorTest {
  // This is the compile-time test. Android relies on this class being inheritable for tests.
  class CertificateValidatorNeedsToBeinheritable(
    trustRoot: ECPublicKey,
  ) : CertificateValidator(trustRoot)
}
