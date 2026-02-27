/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.controllers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;
import static io.zonarosa.server.util.MockUtils.randomSecretBytes;

import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.auth.ExternalServiceCredentials;
import io.zonarosa.server.auth.ExternalServiceCredentialsGenerator;
import io.zonarosa.server.configuration.SecureStorageServiceConfiguration;
import io.zonarosa.server.tests.util.AuthHelper;
import io.zonarosa.server.util.MockUtils;
import io.zonarosa.server.util.SystemMapper;

@ExtendWith(DropwizardExtensionsSupport.class)
class SecureStorageControllerTest {

  private static final SecureStorageServiceConfiguration STORAGE_CFG = MockUtils.buildMock(
      SecureStorageServiceConfiguration.class,
      cfg -> when(cfg.userAuthenticationTokenSharedSecret()).thenReturn(randomSecretBytes(32)));

  private static final ExternalServiceCredentialsGenerator STORAGE_CREDENTIAL_GENERATOR = SecureStorageController
      .credentialsGenerator(STORAGE_CFG);

  private static final ResourceExtension resources = ResourceExtension.builder()
      .addProvider(AuthHelper.getAuthFilter())
      .addProvider(new AuthValueFactoryProvider.Binder<>(AuthenticatedDevice.class))
      .setMapper(SystemMapper.jsonMapper())
      .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
      .addResource(new SecureStorageController(STORAGE_CREDENTIAL_GENERATOR))
      .build();


  @Test
  void testGetCredentials() throws Exception {
    ExternalServiceCredentials credentials = resources.getJerseyTest()
                                                      .target("/v1/storage/auth")
                                                      .request()
                                                      .header("Authorization", AuthHelper.getAuthHeader(AuthHelper.VALID_UUID, AuthHelper.VALID_PASSWORD))
                                                      .get(ExternalServiceCredentials.class);

    assertThat(credentials.password()).isNotEmpty();
    assertThat(credentials.username()).isNotEmpty();
  }

  @Test
  void testGetCredentialsBadAuth() throws Exception {
    Response response = resources.getJerseyTest()
                                 .target("/v1/storage/auth")
                                 .request()
                                 .header("Authorization", AuthHelper.getAuthHeader(AuthHelper.INVALID_UUID, AuthHelper.INVALID_PASSWORD))
                                 .get();

    assertThat(response.getStatus()).isEqualTo(401);
  }
}
