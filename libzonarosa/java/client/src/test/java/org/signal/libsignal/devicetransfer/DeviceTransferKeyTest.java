//
// Copyright 2021-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.devicetransfer;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import junit.framework.TestCase;

public class DeviceTransferKeyTest extends TestCase {
  public void testDeviceTransferKey() throws Exception {
    DeviceTransferKey key = new DeviceTransferKey();
    byte[] certBytes = key.generateCertificate("name", 365);

    CertificateFactory cf = CertificateFactory.getInstance("X.509");
    cf.generateCertificate(new ByteArrayInputStream(certBytes));
  }
}
