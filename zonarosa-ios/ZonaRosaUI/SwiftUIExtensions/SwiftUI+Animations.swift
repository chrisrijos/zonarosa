//
// Copyright 2024 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import SwiftUI

public extension Animation {
    static func quickSpring() -> Animation {
        .spring(response: 0.3, dampingFraction: 1)
    }
}

#Preview {
    struct PreviewContent: View {
        @State private var isOn = false

        var body: some View {
            VStack {
                Rectangle()
                    .fill(Color(UIColor.ows_zonarosaBlue))
                    .frame(width: 100, height: 100)
                    .offset(x: isOn ? 100 : -100)
                Button(String("Quick spring")) {
                    withAnimation(.quickSpring()) {
                        isOn.toggle()
                    }
                }
                .padding()
            }
        }
    }
    return PreviewContent()
}
