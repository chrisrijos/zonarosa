/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Optional;

/**
 * Interface to be implemented by our custom exceptions that are consistently mapped to a gRPC status.
 */
public interface ConvertibleToGrpcStatus {
  StatusRuntimeException toStatusRuntimeException();
}
