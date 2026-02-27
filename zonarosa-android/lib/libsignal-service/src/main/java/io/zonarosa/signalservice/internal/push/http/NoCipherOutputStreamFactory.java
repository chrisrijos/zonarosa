package io.zonarosa.service.internal.push.http;

import io.zonarosa.service.api.crypto.DigestingOutputStream;
import io.zonarosa.service.api.crypto.NoCipherOutputStream;

import java.io.OutputStream;

/**
 * See {@link NoCipherOutputStream}.
 */
public final class NoCipherOutputStreamFactory implements OutputStreamFactory {

  @Override
  public DigestingOutputStream createFor(OutputStream wrap) {
    return new NoCipherOutputStream(wrap);
  }
}
