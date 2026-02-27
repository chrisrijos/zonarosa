//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

const ZONAROSA_DOMAIN_SUFFIX: &str = ".zonarosa.io";

pub(crate) fn log_safe_domain(domain: &str) -> &str {
    match domain {
        "localhost" => domain,
        d if d.ends_with(ZONAROSA_DOMAIN_SUFFIX) => d,
        _ => "REDACTED",
    }
}
