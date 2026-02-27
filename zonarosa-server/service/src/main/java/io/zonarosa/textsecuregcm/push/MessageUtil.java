/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.push;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import io.zonarosa.libzonarosa.protocol.SealedSenderMultiRecipientMessage;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.Device;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class MessageUtil {

  public static final int DEFAULT_MAX_FETCH_ACCOUNT_CONCURRENCY = 8;

  private MessageUtil() {
  }

  /**
   * Finds account records for all recipients named in the given multi-recipient manager. Note that the returned map
   * of recipients to account records will omit entries for recipients that could not be resolved to active accounts;
   * callers that require full resolution should check for a missing entries and take appropriate action.
   *
   * @param accountsManager the {@code AccountsManager} instance to use to find account records
   * @param multiRecipientMessage the message for which to resolve recipients
   *
   * @return a map of recipients to account records
   *
   * @see #getUnresolvedRecipients(SealedSenderMultiRecipientMessage, Map)
   */
  public static Map<SealedSenderMultiRecipientMessage.Recipient, Account> resolveRecipients(
      final AccountsManager accountsManager,
      final SealedSenderMultiRecipientMessage multiRecipientMessage) {

    return resolveRecipients(accountsManager, multiRecipientMessage, DEFAULT_MAX_FETCH_ACCOUNT_CONCURRENCY);
  }

  /**
   * Finds account records for all recipients named in the given multi-recipient manager. Note that the returned map
   * of recipients to account records will omit entries for recipients that could not be resolved to active accounts;
   * callers that require full resolution should check for a missing entries and take appropriate action.
   *
   * @param accountsManager the {@code AccountsManager} instance to use to find account records
   * @param multiRecipientMessage the message for which to resolve recipients
   * @param maxFetchAccountConcurrency the maximum number of concurrent account-retrieval operations
   *
   * @return a map of recipients to account records
   *
   * @see #getUnresolvedRecipients(SealedSenderMultiRecipientMessage, Map)
   */
  public static Map<SealedSenderMultiRecipientMessage.Recipient, Account> resolveRecipients(
      final AccountsManager accountsManager,
      final SealedSenderMultiRecipientMessage multiRecipientMessage,
      final int maxFetchAccountConcurrency) {

    return Flux.fromIterable(multiRecipientMessage.getRecipients().entrySet())
        .flatMap(serviceIdAndRecipient -> {
          final ServiceIdentifier serviceIdentifier =
              ServiceIdentifier.fromLibzonarosa(serviceIdAndRecipient.getKey());

          return Mono.fromFuture(() -> accountsManager.getByServiceIdentifierAsync(serviceIdentifier))
              .flatMap(Mono::justOrEmpty)
              .map(account -> Tuples.of(serviceIdAndRecipient.getValue(), account));
        }, maxFetchAccountConcurrency)
        .collectMap(Tuple2::getT1, Tuple2::getT2)
        .blockOptional()
        .orElse(Collections.emptyMap());
  }

  /**
   * Returns a list of recipients missing from the map of resolved recipients for a multi-recipient message.
   *
   * @param multiRecipientMessage the multi-recipient message
   * @param resolvedRecipients the map of resolved recipients to check for missing entries
   *
   * @return a list of {@code ServiceIdentifiers} belonging to multi-recipient message recipients that are not present
   * in the given map of {@code resolvedRecipients}
   */
  public static List<ServiceIdentifier> getUnresolvedRecipients(
      final SealedSenderMultiRecipientMessage multiRecipientMessage,
      final Map<SealedSenderMultiRecipientMessage.Recipient, Account> resolvedRecipients) {

    return multiRecipientMessage.getRecipients().entrySet().stream()
        .filter(entry -> !resolvedRecipients.containsKey(entry.getValue()))
        .map(entry -> ServiceIdentifier.fromLibzonarosa(entry.getKey()))
        .toList();
  }

  /**
   * Checks if a multi-recipient message contains duplicate recipients.
   *
   * @param multiRecipientMessage the message to check for duplicate recipients
   *
   * @return {@code true} if the message contains duplicate recipients or {@code false} otherwise
   */
  public static boolean hasDuplicateDevices(final SealedSenderMultiRecipientMessage multiRecipientMessage) {
    final boolean[] usedDeviceIds = new boolean[Device.MAXIMUM_DEVICE_ID + 1];

    for (final SealedSenderMultiRecipientMessage.Recipient recipient : multiRecipientMessage.getRecipients().values()) {
      if (recipient.getDevices().length == 1) {
        // A recipient can't have repeated devices if they only have one device
        continue;
      }

      Arrays.fill(usedDeviceIds, false);

      for (final byte deviceId : recipient.getDevices()) {
        if (usedDeviceIds[deviceId]) {
          return true;
        }

        usedDeviceIds[deviceId] = true;
      }
    }

    return false;
  }
}
