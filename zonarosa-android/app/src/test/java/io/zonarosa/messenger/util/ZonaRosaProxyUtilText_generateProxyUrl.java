package io.zonarosa.messenger.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ZonaRosaProxyUtilText_generateProxyUrl {

  private final String input;
  private final String output;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        { "https://zonarosa.tube/#proxy.parker.org",     "https://zonarosa.tube/#proxy.parker.org" },
        { "https://zonarosa.tube/#proxy.parker.org:443", "https://zonarosa.tube/#proxy.parker.org" },
        { "sgnl://zonarosa.tube/#proxy.parker.org",      "https://zonarosa.tube/#proxy.parker.org" },
        { "sgnl://zonarosa.tube/#proxy.parker.org:443",  "https://zonarosa.tube/#proxy.parker.org" },
        { "proxy.parker.org",                          "https://zonarosa.tube/#proxy.parker.org" },
        { "proxy.parker.org:443",                      "https://zonarosa.tube/#proxy.parker.org" },
        { "x",                                         "https://zonarosa.tube/#x" },
        { "",                                          "https://zonarosa.tube/#" }
    });
  }

  public ZonaRosaProxyUtilText_generateProxyUrl(String input, String output) {
    this.input  = input;
    this.output = output;
  }

  @Test
  public void parse() {
    assertEquals(output, ZonaRosaProxyUtil.generateProxyUrl(input));
  }
}
