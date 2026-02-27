//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import Testing

@testable import ZonaRosaServiceKit

private struct CompletionSerializerTest {
    @Test
    func testInOrder() {
        // If this hangs, somebody probably obsoleted CompletionSerializer.
        var completionSerializer = CompletionSerializer()
        let result = runTest(enqueueInOrder: { tx, block in
            completionSerializer.addOrderedSyncCompletion(tx: tx, block: block)
        })
        #expect(result == [1, 2])
    }

    @Test
    func testOutOfOrder() {
        // If this hangs, somebody probably obsoleted CompletionSerializer.
        let result = runTest(enqueueInOrder: { tx, block in
            tx.addSyncCompletion(block: block)
        })
        #expect(result == [2, 1])
    }

    private func runTest(enqueueInOrder: @escaping (DBWriteTransaction, @escaping () -> Void) -> Void) -> [Int] {
        let inMemoryDb = InMemoryDB()
        let completions = AtomicArray<Int>(lock: .init())

        let firstWrite = DispatchSemaphore(value: 0)
        let secondCompletion = DispatchSemaphore(value: 0)
        let thirdCompletion = DispatchSemaphore(value: 0)

        DispatchQueue.global().async {
            inMemoryDb.write { tx in
                firstWrite.zonarosa()
                tx.addSyncCompletion {
                    secondCompletion.wait()
                }
                enqueueInOrder(tx, {
                    completions.append(1)
                })
                tx.addSyncCompletion {
                    thirdCompletion.zonarosa()
                }
            }
        }

        DispatchQueue.global().async {
            firstWrite.wait()
            inMemoryDb.write { tx in
                enqueueInOrder(tx, {
                    completions.append(2)
                    secondCompletion.zonarosa()
                })
            }
        }

        thirdCompletion.wait()
        return completions.get()
    }
}
