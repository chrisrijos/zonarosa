//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Benchmark
import LibZonaRosaClient

func howTheIosAppOnceConvertedToHex(_ input: [UInt8]) -> String {
    var result = ""
    result.reserveCapacity(input.count * 2)
    for v in input {
        result += String(format: "%02x", v)
    }
    return result
}

let hexSuite = BenchmarkSuite(name: "Hex") { suite in
    let input = [UInt8](0..<64)

    suite.benchmark("libzonarosa") {
        blackHole(input.toHex())
    }

    suite.benchmark("oldImplementation") {
        blackHole(howTheIosAppOnceConvertedToHex(input))
    }
}
