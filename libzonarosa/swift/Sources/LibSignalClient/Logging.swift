//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import Foundation
import ZonaRosaFfi

public enum LibzonarosaLogLevel: Comparable, Hashable, Sendable {
    case error, warn, info, debug, trace

    fileprivate init?(_ ffiLevel: ZonaRosaLogLevel) {
        switch ffiLevel {
        case ZonaRosaLogLevelError: self = .error
        case ZonaRosaLogLevelWarn: self = .warn
        case ZonaRosaLogLevelInfo: self = .info
        case ZonaRosaLogLevelDebug: self = .debug
        case ZonaRosaLogLevelTrace: self = .trace
        default: return nil
        }
    }

    fileprivate var asFFI: ZonaRosaLogLevel {
        switch self {
        case .error: return ZonaRosaLogLevelError
        case .warn: return ZonaRosaLogLevelWarn
        case .info: return ZonaRosaLogLevelInfo
        case .debug: return ZonaRosaLogLevelDebug
        case .trace: return ZonaRosaLogLevelTrace
        }
    }
}

public protocol LibzonarosaLogger: Sendable {
    /// Requests that a log message be output at the given log level.
    ///
    /// The file and message are given as C strings to avoid more copying than necessary; however, if they need to be persisted beyond the length of this call, they'll need to be copied somehow.
    ///
    /// This method may be called on any thread, and will be called synchronously from the middle of complicated operations; endeavor to make it quick!
    func log(level: LibzonarosaLogLevel, file: UnsafePointer<CChar>?, line: UInt32, message: UnsafePointer<CChar>)

    /// Requests that the log be flushed.
    ///
    /// This may be called before a fatal error, so it should be handled synchronously if possible, even if that causes a delay.
    ///
    /// This method may be called on any thread.
    func flush()

    /// A special form of `log` for unrecoverable errors.
    ///
    /// The default implementation, based on ZonaRosa-iOS's `owsFail`, logs a stack trace, then the message, then calls `flush`, then calls `fatalError`.
    ///
    /// This method may be called on any thread.
    func logFatal(file: UnsafePointer<CChar>?, line: UInt32, message: UnsafePointer<CChar>) -> Never
}

extension LibzonarosaLogger {
    public func logFatal(file: UnsafePointer<CChar>?, line: UInt32, message: UnsafePointer<CChar>) -> Never {
        log(level: .error, file: file, line: line, message: Thread.callStackSymbols.joined(separator: "\n"))
        log(level: .error, file: file, line: line, message: message)
        flush()
        fatalError(String(cString: message))
    }

    /// Can only be called once in the lifetime of a program; later calls will result in a warning and will not change the active logger.
    public func setUpLibzonarosaLogging(level: LibzonarosaLogLevel) {
        let bridge = LoggerBridge(logger: self)
        let opaqueBridge = Unmanaged.passRetained(bridge)
        let success = zonarosa_init_logger(
            level.asFFI,
            ZonaRosaFfiLogger(
                ctx: opaqueBridge.toOpaque(),
                log: { ctx, ffiLevel, file, line, message in
                    let bridge: LoggerBridge = Unmanaged.fromOpaque(ctx!).takeUnretainedValue()
                    // Unknown log levels might have personal info in them, so map them to something low.
                    let level = LibzonarosaLogLevel(ffiLevel) ?? .debug
                    "".withCString { emptyStringPtr in
                        bridge.logger.log(level: level, file: file, line: line, message: message ?? emptyStringPtr)
                    }
                },
                flush: { ctx in
                    let bridge: LoggerBridge = Unmanaged.fromOpaque(ctx!).takeUnretainedValue()
                    bridge.logger.flush()
                }
            )
        )
        if success {
            // We save this for use within the Swift code as well,
            // but only if it was registered as the Rust logger successfully.
            LoggerBridge.shared = bridge
        } else {
            // Balance the `passRetained` from above.
            opaqueBridge.release()
        }
    }
}

/// A context-pointer-compatible wrapper around a logger.
internal class LoggerBridge {
    let logger: any LibzonarosaLogger
    init(logger: any LibzonarosaLogger) {
        self.logger = logger
    }

    private static let globalLoggerLock = NSLock()
    private nonisolated(unsafe) static var _globalLogger: LoggerBridge? = nil

    internal fileprivate(set) static var shared: LoggerBridge? {
        // Ideally we would use NSLock.withLock here, but that's not available on Linux,
        // which we still support for development and CI.
        get {
            globalLoggerLock.lock()
            defer { globalLoggerLock.unlock() }
            return _globalLogger
        }
        set {
            globalLoggerLock.lock()
            defer { globalLoggerLock.unlock() }
            _globalLogger = newValue
        }
    }
}
