package io.zonarosa.service.internal.push.http;

import io.zonarosa.service.api.messages.SendMessageResult;

import java.util.List;

/**
 * Used to let a listener know when a batch of sends in a collection of sends has been completed.
 */
public interface PartialSendBatchCompleteListener {
  void onPartialSendComplete(List<SendMessageResult> results);
}
