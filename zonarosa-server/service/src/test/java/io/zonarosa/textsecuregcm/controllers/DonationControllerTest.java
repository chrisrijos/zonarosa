/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.ResourceExtension;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredentialPresentation;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptSerial;
import io.zonarosa.libzonarosa.zkgroup.receipts.ServerZkReceiptOperations;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.configuration.BadgeConfiguration;
import io.zonarosa.server.configuration.BadgesConfiguration;
import io.zonarosa.server.entities.BadgeSvg;
import io.zonarosa.server.entities.RedeemReceiptRequest;
import io.zonarosa.server.storage.AccountBadge;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.RedeemedReceiptsManager;
import io.zonarosa.server.tests.util.AccountsHelper;
import io.zonarosa.server.tests.util.AuthHelper;
import io.zonarosa.server.util.TestClock;
import io.zonarosa.server.util.TestRandomUtil;

class DonationControllerTest {

  private static final long nowEpochSeconds = 1_500_000_000L;

  static BadgesConfiguration getBadgesConfiguration() {
    return new BadgesConfiguration(
        List.of(
            new BadgeConfiguration("TEST", "other", List.of("l", "m", "h", "x", "xx", "xxx"), "SVG",
                List.of(new BadgeSvg("sl", "sd"), new BadgeSvg("ml", "md"), new BadgeSvg("ll", "ld"))),
            new BadgeConfiguration("TEST1", "testing", List.of("l", "m", "h", "x", "xx", "xxx"), "SVG",
                List.of(new BadgeSvg("sl", "sd"), new BadgeSvg("ml", "md"), new BadgeSvg("ll", "ld"))),
            new BadgeConfiguration("TEST2", "testing", List.of("l", "m", "h", "x", "xx", "xxx"), "SVG",
                List.of(new BadgeSvg("sl", "sd"), new BadgeSvg("ml", "md"), new BadgeSvg("ll", "ld"))),
            new BadgeConfiguration("TEST3", "testing", List.of("l", "m", "h", "x", "xx", "xxx"), "SVG",
                List.of(new BadgeSvg("sl", "sd"), new BadgeSvg("ml", "md"), new BadgeSvg("ll", "ld")))),
        List.of("TEST"),
        Map.of(1L, "TEST1", 2L, "TEST2", 3L, "TEST3"));
  }

  final Clock clock = TestClock.pinned(Instant.ofEpochSecond(nowEpochSeconds));
  ServerZkReceiptOperations zkReceiptOperations;
  RedeemedReceiptsManager redeemedReceiptsManager;
  AccountsManager accountsManager;
  byte[] receiptSerialBytes;
  ReceiptSerial receiptSerial;
  byte[] presentation;
  DonationController.ReceiptCredentialPresentationFactory receiptCredentialPresentationFactory;
  ReceiptCredentialPresentation receiptCredentialPresentation;
  ResourceExtension resources;

  @BeforeEach
  void beforeEach() throws Throwable {
    zkReceiptOperations = mock(ServerZkReceiptOperations.class);
    redeemedReceiptsManager = mock(RedeemedReceiptsManager.class);
    accountsManager = mock(AccountsManager.class);
    AccountsHelper.setupMockUpdate(accountsManager);
    receiptSerial = new ReceiptSerial(TestRandomUtil.nextBytes(ReceiptSerial.SIZE));
    presentation = TestRandomUtil.nextBytes(25);
    receiptCredentialPresentationFactory = mock(DonationController.ReceiptCredentialPresentationFactory.class);
    receiptCredentialPresentation = mock(ReceiptCredentialPresentation.class);

    try {
      when(receiptCredentialPresentationFactory.build(presentation)).thenReturn(receiptCredentialPresentation);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }

    resources = ResourceExtension.builder()
        .addProvider(AuthHelper.getAuthFilter())
        .addProvider(new AuthValueFactoryProvider.Binder<>(AuthenticatedDevice.class))
        .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
        .addResource(new DonationController(clock, zkReceiptOperations, redeemedReceiptsManager, accountsManager,
            getBadgesConfiguration(), receiptCredentialPresentationFactory))
        .build();
    resources.before();
  }

  @AfterEach
  void afterEach() throws Throwable {
    resources.after();
  }

  @Test
  void testRedeemReceipt() {
    when(receiptCredentialPresentation.getReceiptSerial()).thenReturn(receiptSerial);
    final long receiptLevel = 1L;
    when(receiptCredentialPresentation.getReceiptLevel()).thenReturn(receiptLevel);
    final long receiptExpiration = nowEpochSeconds + 86400 * 30;
    when(receiptCredentialPresentation.getReceiptExpirationTime()).thenReturn(receiptExpiration);
    when(redeemedReceiptsManager.put(same(receiptSerial), eq(receiptExpiration), eq(receiptLevel), eq(AuthHelper.VALID_UUID))).thenReturn(
        CompletableFuture.completedFuture(Boolean.TRUE));
    when(accountsManager.getByAccountIdentifier(eq(AuthHelper.VALID_UUID)))
        .thenReturn(Optional.of(AuthHelper.VALID_ACCOUNT));

    RedeemReceiptRequest request = new RedeemReceiptRequest(presentation, true, true);
    Response response = resources.getJerseyTest()
        .target("/v1/donation/redeem-receipt")
        .request()
        .header("Authorization", AuthHelper.getAuthHeader(AuthHelper.VALID_UUID, AuthHelper.VALID_PASSWORD))
        .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));

    assertThat(response.getStatus()).isEqualTo(200);
    verify(AuthHelper.VALID_ACCOUNT).addBadge(same(clock), eq(new AccountBadge("TEST1", Instant.ofEpochSecond(receiptExpiration), true)));
    verify(AuthHelper.VALID_ACCOUNT).makeBadgePrimaryIfExists(same(clock), eq("TEST1"));
  }

  @Test
  void testRedeemReceiptAlreadyRedeemedWithDifferentParameters() {
    when(receiptCredentialPresentation.getReceiptSerial()).thenReturn(receiptSerial);
    final long receiptLevel = 1L;
    when(receiptCredentialPresentation.getReceiptLevel()).thenReturn(receiptLevel);
    final long receiptExpiration = nowEpochSeconds + 86400 * 30;
    when(receiptCredentialPresentation.getReceiptExpirationTime()).thenReturn(receiptExpiration);
    when(redeemedReceiptsManager.put(same(receiptSerial), eq(receiptExpiration), eq(receiptLevel), eq(AuthHelper.VALID_UUID))).thenReturn(
        CompletableFuture.completedFuture(Boolean.FALSE));
    when(accountsManager.getByAccountIdentifier(eq(AuthHelper.VALID_UUID)))
        .thenReturn(Optional.of(AuthHelper.VALID_ACCOUNT));

    RedeemReceiptRequest request = new RedeemReceiptRequest(presentation, true, true);
    Response response = resources.getJerseyTest()
        .target("/v1/donation/redeem-receipt")
        .request()
        .header("Authorization", AuthHelper.getAuthHeader(AuthHelper.VALID_UUID, AuthHelper.VALID_PASSWORD))
        .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(response.readEntity(String.class)).isEqualTo("receipt serial is already redeemed");
  }

  @Test
  void testRedeemReceiptBadCredentialPresentation() throws InvalidInputException {
    when(receiptCredentialPresentationFactory.build(any())).thenThrow(new InvalidInputException());

    final Response response = resources.getJerseyTest()
        .target("/v1/donation/redeem-receipt")
        .request()
        .header("Authorization", AuthHelper.getAuthHeader(AuthHelper.VALID_UUID, AuthHelper.VALID_PASSWORD))
        .post(Entity.entity(new RedeemReceiptRequest(presentation, true, true), MediaType.APPLICATION_JSON_TYPE));

    assertThat(response.getStatus()).isEqualTo(400);
  }
}
