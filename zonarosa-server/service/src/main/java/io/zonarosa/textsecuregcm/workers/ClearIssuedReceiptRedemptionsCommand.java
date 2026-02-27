/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.workers;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Environment;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.zonarosa.server.WhisperServerConfiguration;
import io.zonarosa.server.storage.IssuedReceiptsManager;
import io.zonarosa.server.storage.SubscriberCredentials;
import io.zonarosa.server.storage.SubscriptionManager;
import io.zonarosa.server.storage.Subscriptions;
import io.zonarosa.server.subscriptions.PaymentProvider;
import io.zonarosa.server.subscriptions.SubscriptionPaymentProcessor;

public class ClearIssuedReceiptRedemptionsCommand extends AbstractCommandWithDependencies {

  private final Logger logger = LoggerFactory.getLogger(ClearIssuedReceiptRedemptionsCommand.class);

  public ClearIssuedReceiptRedemptionsCommand() {
    super(new Application<>() {
      @Override
      public void run(WhisperServerConfiguration configuration, Environment environment) {

      }
    }, "clear-issued-receipt-redemptions", "Clear issued receipt redemptions");
  }

  @Override
  public void configure(Subparser subparser) {
    super.configure(subparser);
    subparser.addArgument("-s", "--subscriber-id")
        .dest("subscriberId")
        .type(String.class)
        .required(true)
        .help("The subscriber-id whose receipt redemptions should be clear");
  }

  @Override
  protected void run(Environment environment, Namespace namespace, WhisperServerConfiguration configuration,
      CommandDependencies deps) throws Exception {
    try {
      final String subscriberId = namespace.getString("subscriberId");

      final SubscriberCredentials creds = SubscriberCredentials
          .process(Optional.empty(), subscriberId, Clock.systemUTC());

      final IssuedReceiptsManager issuedReceiptsManager = deps.issuedReceiptsManager();
      final SubscriptionManager subscriptionManager = deps.subscriptionManager();

      final Subscriptions.Record subscriber = subscriptionManager.getSubscriber(creds);
      final PaymentProvider processorType = subscriber.getProcessorCustomer()
          .orElseThrow(() -> new IllegalArgumentException("susbcriber did not have a subscription"))
          .processor();
      final SubscriptionPaymentProcessor processor = switch (processorType) {
        case APPLE_APP_STORE -> deps.appleAppStoreManager();
        case GOOGLE_PLAY_BILLING -> deps.googlePlayBillingManager();
        default ->
            throw new IllegalStateException("Cannot clear issued receipts for a non-IAP processor: " + processorType);
      };
      final SubscriptionPaymentProcessor.ReceiptItem receiptItem = processor.getReceiptItem(subscriber.subscriptionId);
      final boolean deleted = issuedReceiptsManager.clearIssuance(receiptItem.itemId(), processorType).join();
      logger.info("Deleted issuances for receiptItem: {}, subscriberId: {}, hadExistingIssuances: {}",
          receiptItem.itemId(), subscriberId, deleted);
    } catch (Exception ex) {
      logger.warn("Removal Exception", ex);
      throw new RuntimeException(ex);
    }
  }

  public static void main(String[] args) throws Exception {
    final String subscriberId = "7ywqmymkSMBkBi9v06Iy4AN8DiN_lg8gHXM8TSpO0Z0";

    final SubscriberCredentials creds = SubscriberCredentials
        .process(Optional.empty(), subscriberId, Clock.systemUTC());
    System.out.println(Base64.getEncoder().encodeToString(creds.subscriberUser()));

    final String pc = "AWN1c19TV3hDUEhlWDBldzB0UA==";
    final byte[] bc = Base64.getDecoder().decode(pc);
    System.out.println(bc[0]);
    System.out.println(new String(bc, 1, bc.length - 1));
  }
}
