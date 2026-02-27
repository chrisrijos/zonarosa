//
// Copyright 2025 ZonaRosa Platform
// SPDX-License-Identifier: MIT-3.0-only
//

public import ZonaRosaServiceKit

public protocol StoryStickerPickerDelegate: AnyObject {
    func didSelect(storySticker: EditorSticker.StorySticker)
}

public enum StoryStickerConfiguration {
    case hide
    case showWithDelegate(StoryStickerPickerDelegate)
}

public protocol StickerPickerDelegate: AnyObject {
    func didSelectSticker(_ stickerInfo: StickerInfo)
}
