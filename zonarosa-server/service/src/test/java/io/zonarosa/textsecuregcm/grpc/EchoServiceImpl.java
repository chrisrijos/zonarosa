/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import io.grpc.stub.StreamObserver;
import io.zonarosa.chat.rpc.EchoRequest;
import io.zonarosa.chat.rpc.EchoResponse;
import io.zonarosa.chat.rpc.EchoServiceGrpc;

public class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {
  @Override
  public void echo(final EchoRequest echoRequest, final StreamObserver<EchoResponse> responseObserver) {
    responseObserver.onNext(buildResponse(echoRequest));
    responseObserver.onCompleted();
  }

  @Override
  public void echo2(final EchoRequest echoRequest, final StreamObserver<EchoResponse> responseObserver) {
    responseObserver.onNext(buildResponse(echoRequest));
    responseObserver.onCompleted();
  }

  @Override
  public StreamObserver<EchoRequest> echoStream(final StreamObserver<EchoResponse> responseObserver) {
    return new StreamObserver<>() {
      @Override
      public void onNext(final EchoRequest echoRequest) {
        responseObserver.onNext(buildResponse(echoRequest));
      }

      @Override
      public void onError(final Throwable throwable) {
        responseObserver.onError(throwable);
      }

      @Override
      public void onCompleted() {
        responseObserver.onCompleted();
      }
    };
  }

  private static EchoResponse buildResponse(final EchoRequest echoRequest) {
    return EchoResponse.newBuilder().setPayload(echoRequest.getPayload()).build();
  }
}
