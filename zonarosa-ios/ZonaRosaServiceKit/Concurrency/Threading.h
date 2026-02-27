//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

NS_ASSUME_NONNULL_BEGIN

/// Returns YES if the result returned from dispatch_get_current_queue() matches
/// the provided queue. There's all sorts of different circumstances where these queue
/// comparisons may fail (queue hierarchies, etc.) so this should only be used optimistically
/// for perf optimizations. This should never be used to determine if some pattern of block dispatch is deadlock free.
BOOL DispatchQueueIsCurrentQueue(dispatch_queue_t queue);

/// Returns a value [0.0, 1.0] indicating the proportion of the current thread's stack that's in-use
/// Returns NaN on any unexpected error
/// Only for use in ZonaRosaServiceKit's promise implementation. Please do not use.
double _CurrentStackUsage(void);

NS_ASSUME_NONNULL_END
