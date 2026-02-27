//
// Copyright 2021 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import ZonaRosaServiceKit

extension Sounds {

    private static func shouldAudioPlayerLoop(forSound sound: Sound) -> Bool {
        guard case .standard(let standardSound) = sound else { return false }
        switch standardSound {
        case .callConnecting, .callOutboundRinging:
            return true
        default:
            return false
        }
    }

    public static func audioPlayer(forSound sound: Sound, audioBehavior: AudioBehavior) -> AudioPlayer? {
        guard let soundUrl = sound.soundUrl(quiet: false) else {
            return nil
        }
        let player = AudioPlayer(decryptedFileUrl: soundUrl, audioBehavior: audioBehavior)
        if shouldAudioPlayerLoop(forSound: sound) {
            player.isLooping = true
        }
        return player
    }
}
