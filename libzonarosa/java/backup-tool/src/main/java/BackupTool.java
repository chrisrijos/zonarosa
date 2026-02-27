//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.concurrent.Callable;
import io.zonarosa.libzonarosa.messagebackup.MessageBackup;
import io.zonarosa.libzonarosa.messagebackup.MessageBackupKey;
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLogger;
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLoggerProvider;
import io.zonarosa.libzonarosa.protocol.util.Hex;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

class BackupTool implements Callable<Integer> {
  @Option(names = "--hmac-key")
  String hmacKey;

  @Option(names = "--aes-key")
  String aesKey;

  @Parameters File input;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new BackupTool()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() throws Exception {
    ZonaRosaProtocolLoggerProvider.initializeLogging(ZonaRosaProtocolLogger.INFO);
    ZonaRosaProtocolLoggerProvider.setProvider(
        new ZonaRosaProtocolLogger() {
          public void log(int priority, String tag, String message) {
            System.err.println(priority + " " + message);
          }
        });

    byte[] hmacKey = Hex.fromStringCondensed(this.hmacKey);
    byte[] aesKey = Hex.fromStringCondensed(this.aesKey);
    var backupKey = MessageBackupKey.fromParts(hmacKey, aesKey);

    MessageBackup.ValidationResult result =
        MessageBackup.validate(
            backupKey,
            MessageBackup.Purpose.REMOTE_BACKUP,
            () -> {
              try {
                return new FileInputStream(input);
              } catch (FileNotFoundException e) {
                throw new AssertionError(e);
              }
            },
            input.length());
    return result.unknownFieldMessages.length == 0 ? 0 : 1;
  }
}
