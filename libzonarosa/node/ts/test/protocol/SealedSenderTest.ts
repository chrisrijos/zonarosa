//
// Copyright 2021-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import * as ZonaRosaClient from '../../index.js';
import * as util from '../util.js';

import { assert, use } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import { Buffer } from 'node:buffer';

import {
  InMemoryIdentityKeyStore,
  InMemoryKyberPreKeyStore,
  InMemoryPreKeyStore,
  InMemorySenderKeyStore,
  InMemorySessionStore,
  InMemorySignedPreKeyStore,
} from './TestStores.js';

use(chaiAsPromised);
util.initLogger();

describe('SealedSender', () => {
  it('can encrypt/decrypt 1-1 messages', async () => {
    const aKeys = new InMemoryIdentityKeyStore();
    const bKeys = new InMemoryIdentityKeyStore();

    const aSess = new InMemorySessionStore();
    const bSess = new InMemorySessionStore();

    const bPreK = new InMemoryPreKeyStore();
    const bSPreK = new InMemorySignedPreKeyStore();
    const bKyberStore = new InMemoryKyberPreKeyStore();

    const bPreKey = ZonaRosaClient.PrivateKey.generate();
    const bSPreKey = ZonaRosaClient.PrivateKey.generate();

    const aIdentityKey = await aKeys.getIdentityKey();
    const bIdentityKey = await bKeys.getIdentityKey();

    const aE164 = '+14151111111';
    const bE164 = '+19192222222';

    const aDeviceId = 1;
    const bDeviceId = 3;

    const aUuid = '9d0652a3-dcc3-4d11-975f-74d61598733f';
    const bUuid = '796abedb-ca4e-4f18-8803-1fde5b921f9f';

    const trustRoot = ZonaRosaClient.PrivateKey.generate();
    const serverKey = ZonaRosaClient.PrivateKey.generate();

    const serverCert = ZonaRosaClient.ServerCertificate.new(
      1,
      serverKey.getPublicKey(),
      trustRoot
    );

    const expires = 1605722925;
    const senderCert = ZonaRosaClient.SenderCertificate.new(
      aUuid,
      aE164,
      aDeviceId,
      aIdentityKey.getPublicKey(),
      expires,
      serverCert,
      serverKey
    );

    const bRegistrationId = await bKeys.getLocalRegistrationId();
    const bPreKeyId = 31337;
    const bSignedPreKeyId = 22;

    const bSignedPreKeySig = bIdentityKey.sign(
      bSPreKey.getPublicKey().serialize()
    );

    const bKyberPrekeyId = 777;
    const bKyberKeyPair = ZonaRosaClient.KEMKeyPair.generate();
    const bKyberPrekeySignature = bIdentityKey.sign(
      bKyberKeyPair.getPublicKey().serialize()
    );

    const bPreKeyBundle = ZonaRosaClient.PreKeyBundle.new(
      bRegistrationId,
      bDeviceId,
      bPreKeyId,
      bPreKey.getPublicKey(),
      bSignedPreKeyId,
      bSPreKey.getPublicKey(),
      bSignedPreKeySig,
      bIdentityKey.getPublicKey(),
      bKyberPrekeyId,
      bKyberKeyPair.getPublicKey(),
      bKyberPrekeySignature
    );

    const bPreKeyRecord = ZonaRosaClient.PreKeyRecord.new(
      bPreKeyId,
      bPreKey.getPublicKey(),
      bPreKey
    );
    await bPreK.savePreKey(bPreKeyId, bPreKeyRecord);

    const bSPreKeyRecord = ZonaRosaClient.SignedPreKeyRecord.new(
      bSignedPreKeyId,
      42, // timestamp
      bSPreKey.getPublicKey(),
      bSPreKey,
      bSignedPreKeySig
    );
    await bSPreK.saveSignedPreKey(bSignedPreKeyId, bSPreKeyRecord);

    const bKyberPreKeyRecord = ZonaRosaClient.KyberPreKeyRecord.new(
      bKyberPrekeyId,
      42, // timestamp
      bKyberKeyPair,
      bKyberPrekeySignature
    );
    await bKyberStore.saveKyberPreKey(bKyberPrekeyId, bKyberPreKeyRecord);

    const bAddress = ZonaRosaClient.ProtocolAddress.new(bUuid, bDeviceId);
    await ZonaRosaClient.processPreKeyBundle(
      bPreKeyBundle,
      bAddress,
      aSess,
      aKeys
    );

    const aPlaintext = Buffer.from('hi there', 'utf8');

    const aCiphertext = await ZonaRosaClient.sealedSenderEncryptMessage(
      aPlaintext,
      bAddress,
      senderCert,
      aSess,
      aKeys
    );

    const bPlaintext = await ZonaRosaClient.sealedSenderDecryptMessage(
      aCiphertext,
      trustRoot.getPublicKey(),
      43, // timestamp,
      bE164,
      bUuid,
      bDeviceId,
      bSess,
      bKeys,
      bPreK,
      bSPreK,
      bKyberStore
    );

    assert(bPlaintext != null);
    assert.deepEqual(bPlaintext.message(), aPlaintext);
    assert.deepEqual(bPlaintext.senderE164(), aE164);
    assert.deepEqual(bPlaintext.senderUuid(), aUuid);
    assert.deepEqual(bPlaintext.senderAci()?.getServiceIdString(), aUuid);
    assert.deepEqual(bPlaintext.deviceId(), aDeviceId);

    const randomPublicKey = () =>
      ZonaRosaClient.PrivateKey.generate().getPublicKey();
    assert.isTrue(
      senderCert.validateWithTrustRoots(
        [randomPublicKey(), trustRoot.getPublicKey(), randomPublicKey()],
        31335
      )
    );

    assert.isFalse(
      senderCert.validateWithTrustRoots(
        [randomPublicKey(), randomPublicKey()],
        31335
      )
    );

    const innerMessage = await ZonaRosaClient.zonarosaEncrypt(
      aPlaintext,
      bAddress,
      aSess,
      aKeys
    );

    for (const hint of [
      200,
      ZonaRosaClient.ContentHint.Default,
      ZonaRosaClient.ContentHint.Resendable,
      ZonaRosaClient.ContentHint.Implicit,
    ]) {
      const content = ZonaRosaClient.UnidentifiedSenderMessageContent.new(
        innerMessage,
        senderCert,
        hint,
        null
      );
      const ciphertext = await ZonaRosaClient.sealedSenderEncrypt(
        content,
        bAddress,
        aKeys
      );
      const decryptedContent = await ZonaRosaClient.sealedSenderDecryptToUsmc(
        ciphertext,
        bKeys
      );
      assert.deepEqual(decryptedContent.contentHint(), hint);
    }
  });

  it('rejects self-sent messages', async () => {
    const sharedKeys = new InMemoryIdentityKeyStore();

    const aSess = new InMemorySessionStore();
    const bSess = new InMemorySessionStore();

    const bPreK = new InMemoryPreKeyStore();
    const bSPreK = new InMemorySignedPreKeyStore();
    const bKyberStore = new InMemoryKyberPreKeyStore();

    const bPreKey = ZonaRosaClient.PrivateKey.generate();
    const bSPreKey = ZonaRosaClient.PrivateKey.generate();

    const sharedIdentityKey = await sharedKeys.getIdentityKey();

    const aE164 = '+14151111111';

    const sharedDeviceId = 1;

    const sharedUuid = '9d0652a3-dcc3-4d11-975f-74d61598733f';

    const trustRoot = ZonaRosaClient.PrivateKey.generate();
    const serverKey = ZonaRosaClient.PrivateKey.generate();

    const serverCert = ZonaRosaClient.ServerCertificate.new(
      1,
      serverKey.getPublicKey(),
      trustRoot
    );

    const expires = 1605722925;
    const senderCert = ZonaRosaClient.SenderCertificate.new(
      sharedUuid,
      aE164,
      sharedDeviceId,
      sharedIdentityKey.getPublicKey(),
      expires,
      serverCert,
      serverKey
    );

    const sharedRegistrationId = await sharedKeys.getLocalRegistrationId();
    const bPreKeyId = 31337;
    const bSignedPreKeyId = 22;

    const bSignedPreKeySig = sharedIdentityKey.sign(
      bSPreKey.getPublicKey().serialize()
    );

    const bKyberPrekeyId = 777;
    const bKyberKeyPair = ZonaRosaClient.KEMKeyPair.generate();
    const bKyberPrekeySignature = sharedIdentityKey.sign(
      bKyberKeyPair.getPublicKey().serialize()
    );

    const bPreKeyBundle = ZonaRosaClient.PreKeyBundle.new(
      sharedRegistrationId,
      sharedDeviceId,
      bPreKeyId,
      bPreKey.getPublicKey(),
      bSignedPreKeyId,
      bSPreKey.getPublicKey(),
      bSignedPreKeySig,
      sharedIdentityKey.getPublicKey(),
      bKyberPrekeyId,
      bKyberKeyPair.getPublicKey(),
      bKyberPrekeySignature
    );

    const bPreKeyRecord = ZonaRosaClient.PreKeyRecord.new(
      bPreKeyId,
      bPreKey.getPublicKey(),
      bPreKey
    );
    await bPreK.savePreKey(bPreKeyId, bPreKeyRecord);

    const bSPreKeyRecord = ZonaRosaClient.SignedPreKeyRecord.new(
      bSignedPreKeyId,
      42, // timestamp
      bSPreKey.getPublicKey(),
      bSPreKey,
      bSignedPreKeySig
    );
    await bSPreK.saveSignedPreKey(bSignedPreKeyId, bSPreKeyRecord);

    const bKyberPreKeyRecord = ZonaRosaClient.KyberPreKeyRecord.new(
      bKyberPrekeyId,
      42, // timestamp
      bKyberKeyPair,
      bKyberPrekeySignature
    );
    await bKyberStore.saveKyberPreKey(bKyberPrekeyId, bKyberPreKeyRecord);

    const sharedAddress = ZonaRosaClient.ProtocolAddress.new(
      sharedUuid,
      sharedDeviceId
    );
    await ZonaRosaClient.processPreKeyBundle(
      bPreKeyBundle,
      sharedAddress,
      aSess,
      sharedKeys
    );

    const aPlaintext = Buffer.from('hi there', 'utf8');

    const aCiphertext = await ZonaRosaClient.sealedSenderEncryptMessage(
      aPlaintext,
      sharedAddress,
      senderCert,
      aSess,
      sharedKeys
    );

    try {
      await ZonaRosaClient.sealedSenderDecryptMessage(
        aCiphertext,
        trustRoot.getPublicKey(),
        43, // timestamp,
        null,
        sharedUuid,
        sharedDeviceId,
        bSess,
        sharedKeys,
        bPreK,
        bSPreK,
        bKyberStore
      );
      assert.fail();
    } catch (e) {
      assert.instanceOf(e, Error);
      assert.instanceOf(e, ZonaRosaClient.LibZonaRosaErrorBase);
      const err = e as ZonaRosaClient.LibZonaRosaError;
      assert.equal(err.name, 'SealedSenderSelfSend');
      assert.equal(err.code, ZonaRosaClient.ErrorCode.SealedSenderSelfSend);
      assert.equal(err.operation, 'SealedSender_DecryptMessage'); // the Rust entry point
      assert.exists(err.stack); // Make sure we're still getting the benefits of Error.
    }
  });

  it('can encrypt/decrypt group messages', async () => {
    const aKeys = new InMemoryIdentityKeyStore();
    const bKeys = new InMemoryIdentityKeyStore();

    const aSess = new InMemorySessionStore();

    const bPreK = new InMemoryPreKeyStore();
    const bSPreK = new InMemorySignedPreKeyStore();
    const bKyberStore = new InMemoryKyberPreKeyStore();

    const bPreKey = ZonaRosaClient.PrivateKey.generate();
    const bSPreKey = ZonaRosaClient.PrivateKey.generate();

    const aIdentityKey = await aKeys.getIdentityKey();
    const bIdentityKey = await bKeys.getIdentityKey();

    const aE164 = '+14151111111';

    const aDeviceId = 1;
    const bDeviceId = 3;

    const aUuid = '9d0652a3-dcc3-4d11-975f-74d61598733f';
    const bUuid = '796abedb-ca4e-4f18-8803-1fde5b921f9f';

    const trustRoot = ZonaRosaClient.PrivateKey.generate();
    const serverKey = ZonaRosaClient.PrivateKey.generate();

    const serverCert = ZonaRosaClient.ServerCertificate.new(
      1,
      serverKey.getPublicKey(),
      trustRoot
    );

    const expires = 1605722925;
    const senderCert = ZonaRosaClient.SenderCertificate.new(
      aUuid,
      aE164,
      aDeviceId,
      aIdentityKey.getPublicKey(),
      expires,
      serverCert,
      serverKey
    );

    const bRegistrationId = await bKeys.getLocalRegistrationId();
    const bPreKeyId = 31337;
    const bSignedPreKeyId = 22;

    const bSignedPreKeySig = bIdentityKey.sign(
      bSPreKey.getPublicKey().serialize()
    );

    const bKyberPrekeyId = 777;
    const bKyberKeyPair = ZonaRosaClient.KEMKeyPair.generate();
    const bKyberPrekeySignature = bIdentityKey.sign(
      bKyberKeyPair.getPublicKey().serialize()
    );

    const bPreKeyBundle = ZonaRosaClient.PreKeyBundle.new(
      bRegistrationId,
      bDeviceId,
      bPreKeyId,
      bPreKey.getPublicKey(),
      bSignedPreKeyId,
      bSPreKey.getPublicKey(),
      bSignedPreKeySig,
      bIdentityKey.getPublicKey(),
      bKyberPrekeyId,
      bKyberKeyPair.getPublicKey(),
      bKyberPrekeySignature
    );

    const bPreKeyRecord = ZonaRosaClient.PreKeyRecord.new(
      bPreKeyId,
      bPreKey.getPublicKey(),
      bPreKey
    );
    await bPreK.savePreKey(bPreKeyId, bPreKeyRecord);

    const bSPreKeyRecord = ZonaRosaClient.SignedPreKeyRecord.new(
      bSignedPreKeyId,
      42, // timestamp
      bSPreKey.getPublicKey(),
      bSPreKey,
      bSignedPreKeySig
    );
    await bSPreK.saveSignedPreKey(bSignedPreKeyId, bSPreKeyRecord);

    const bKyberPreKeyRecord = ZonaRosaClient.KyberPreKeyRecord.new(
      bKyberPrekeyId,
      42, // timestamp
      bKyberKeyPair,
      bKyberPrekeySignature
    );
    await bKyberStore.saveKyberPreKey(bKyberPrekeyId, bKyberPreKeyRecord);

    const bAddress = ZonaRosaClient.ProtocolAddress.new(bUuid, bDeviceId);
    await ZonaRosaClient.processPreKeyBundle(
      bPreKeyBundle,
      bAddress,
      aSess,
      aKeys
    );

    const aAddress = ZonaRosaClient.ProtocolAddress.new(aUuid, aDeviceId);

    const distributionId = 'd1d1d1d1-7000-11eb-b32a-33b8a8a487a6';
    const aSenderKeyStore = new InMemorySenderKeyStore();
    const skdm = await ZonaRosaClient.SenderKeyDistributionMessage.create(
      aAddress,
      distributionId,
      aSenderKeyStore
    );

    const bSenderKeyStore = new InMemorySenderKeyStore();
    await ZonaRosaClient.processSenderKeyDistributionMessage(
      aAddress,
      skdm,
      bSenderKeyStore
    );

    const message = Buffer.from('0a0b0c', 'hex');

    const aCtext = await ZonaRosaClient.groupEncrypt(
      aAddress,
      distributionId,
      aSenderKeyStore,
      message
    );

    const aUsmc = ZonaRosaClient.UnidentifiedSenderMessageContent.new(
      aCtext,
      senderCert,
      ZonaRosaClient.ContentHint.Implicit,
      Buffer.from([42])
    );

    const aSealedSenderMessage =
      await ZonaRosaClient.sealedSenderMultiRecipientEncrypt(
        aUsmc,
        [bAddress],
        aKeys,
        aSess
      );

    const bSealedSenderMessage =
      ZonaRosaClient.sealedSenderMultiRecipientMessageForSingleRecipient(
        aSealedSenderMessage
      );

    const bUsmc = await ZonaRosaClient.sealedSenderDecryptToUsmc(
      bSealedSenderMessage,
      bKeys
    );

    assert.deepEqual(bUsmc.senderCertificate().senderE164(), aE164);
    assert.deepEqual(bUsmc.senderCertificate().senderUuid(), aUuid);
    assert.deepEqual(bUsmc.senderCertificate().senderDeviceId(), aDeviceId);
    assert.deepEqual(bUsmc.contentHint(), ZonaRosaClient.ContentHint.Implicit);
    assert.deepEqual(bUsmc.groupId(), Buffer.from([42]));

    const bPtext = await ZonaRosaClient.groupDecrypt(
      aAddress,
      bSenderKeyStore,
      bUsmc.contents()
    );

    util.assertArrayEquals(message, bPtext);

    // Make sure the option-based syntax does the same thing.
    const aSealedSenderMessageViaOptions =
      await ZonaRosaClient.sealedSenderMultiRecipientEncrypt({
        content: aUsmc,
        recipients: [bAddress],
        identityStore: aKeys,
        sessionStore: aSess,
      });

    const bSealedSenderMessageViaOptions =
      ZonaRosaClient.sealedSenderMultiRecipientMessageForSingleRecipient(
        aSealedSenderMessageViaOptions
      );

    const bUsmcViaOptions = await ZonaRosaClient.sealedSenderDecryptToUsmc(
      bSealedSenderMessageViaOptions,
      bKeys
    );

    assert.deepEqual(bUsmcViaOptions, bUsmc);
  });

  it('rejects invalid registration IDs', async () => {
    const aKeys = new InMemoryIdentityKeyStore();
    const bKeys = new InMemoryIdentityKeyStore(0x4000);

    const aSess = new InMemorySessionStore();

    const bPreKey = ZonaRosaClient.PrivateKey.generate();
    const bSPreKey = ZonaRosaClient.PrivateKey.generate();

    const aIdentityKey = await aKeys.getIdentityKey();
    const bIdentityKey = await bKeys.getIdentityKey();

    const aE164 = '+14151111111';

    const aDeviceId = 1;
    const bDeviceId = 3;

    const aUuid = '9d0652a3-dcc3-4d11-975f-74d61598733f';
    const bUuid = '796abedb-ca4e-4f18-8803-1fde5b921f9f';

    const trustRoot = ZonaRosaClient.PrivateKey.generate();
    const serverKey = ZonaRosaClient.PrivateKey.generate();

    const serverCert = ZonaRosaClient.ServerCertificate.new(
      1,
      serverKey.getPublicKey(),
      trustRoot
    );

    const expires = 1605722925;
    const senderCert = ZonaRosaClient.SenderCertificate.new(
      aUuid,
      aE164,
      aDeviceId,
      aIdentityKey.getPublicKey(),
      expires,
      serverCert,
      serverKey
    );

    const bPreKeyId = 31337;
    const bSignedPreKeyId = 22;

    const bSignedPreKeySig = bIdentityKey.sign(
      bSPreKey.getPublicKey().serialize()
    );

    const bKyberPrekeyId = 777;
    const bKyberKeyPair = ZonaRosaClient.KEMKeyPair.generate();
    const bKyberPrekeySignature = bIdentityKey.sign(
      bKyberKeyPair.getPublicKey().serialize()
    );

    const bPreKeyBundle = ZonaRosaClient.PreKeyBundle.new(
      0x4000,
      bDeviceId,
      bPreKeyId,
      bPreKey.getPublicKey(),
      bSignedPreKeyId,
      bSPreKey.getPublicKey(),
      bSignedPreKeySig,
      bIdentityKey.getPublicKey(),
      bKyberPrekeyId,
      bKyberKeyPair.getPublicKey(),
      bKyberPrekeySignature
    );

    const bAddress = ZonaRosaClient.ProtocolAddress.new(bUuid, bDeviceId);
    await ZonaRosaClient.processPreKeyBundle(
      bPreKeyBundle,
      bAddress,
      aSess,
      aKeys
    );

    const aAddress = ZonaRosaClient.ProtocolAddress.new(aUuid, aDeviceId);

    const distributionId = 'd1d1d1d1-7000-11eb-b32a-33b8a8a487a6';
    const aSenderKeyStore = new InMemorySenderKeyStore();
    await ZonaRosaClient.SenderKeyDistributionMessage.create(
      aAddress,
      distributionId,
      aSenderKeyStore
    );

    const message = Buffer.from('0a0b0c', 'hex');

    const aCtext = await ZonaRosaClient.groupEncrypt(
      aAddress,
      distributionId,
      aSenderKeyStore,
      message
    );

    const aUsmc = ZonaRosaClient.UnidentifiedSenderMessageContent.new(
      aCtext,
      senderCert,
      ZonaRosaClient.ContentHint.Implicit,
      Buffer.from([42])
    );

    try {
      await ZonaRosaClient.sealedSenderMultiRecipientEncrypt(
        aUsmc,
        [bAddress],
        aKeys,
        aSess
      );
      assert.fail('should have thrown');
    } catch (e) {
      assert.instanceOf(e, Error);
      assert.instanceOf(e, ZonaRosaClient.LibZonaRosaErrorBase);
      const err = e as ZonaRosaClient.LibZonaRosaError;
      assert.equal(err.name, 'InvalidRegistrationId');
      assert.equal(err.code, ZonaRosaClient.ErrorCode.InvalidRegistrationId);
      assert.exists(err.stack); // Make sure we're still getting the benefits of Error.

      // Note: This is not a Chai assert; Chai doesn't yet support TypeScript assert functions for
      // type narrowing. But we already checked the code above.
      assert(err.is(ZonaRosaClient.ErrorCode.InvalidRegistrationId));
      assert.equal(err.addr.name(), bAddress.name());
      assert.equal(err.addr.deviceId(), bAddress.deviceId());

      // We can also narrow directly from the original thrown value. (But we didn't do that
      // earlier because we wanted to check all the properties individually.)
      assert(
        ZonaRosaClient.LibZonaRosaErrorBase.is(
          e,
          ZonaRosaClient.ErrorCode.InvalidRegistrationId
        )
      );
      assert.equal(e.addr.name(), bAddress.name());
    }
  });

  it('can have excluded recipients', async () => {
    const aKeys = new InMemoryIdentityKeyStore();
    const bKeys = new InMemoryIdentityKeyStore(0x4000);

    const aSess = new InMemorySessionStore();

    const bPreKey = ZonaRosaClient.PrivateKey.generate();
    const bSPreKey = ZonaRosaClient.PrivateKey.generate();

    const aIdentityKey = await aKeys.getIdentityKey();
    const bIdentityKey = await bKeys.getIdentityKey();

    const aE164 = '+14151111111';

    const aDeviceId = 1;
    const bDeviceId = 3;

    const aUuid = '9d0652a3-dcc3-4d11-975f-74d61598733f';
    const bUuid = '796abedb-ca4e-4f18-8803-1fde5b921f9f';
    const eUuid = '3f0f4734-e331-4434-bd4f-6d8f6ea6dcc7';
    const mUuid = '5d088142-6fd7-4dbd-af00-fdda1b3ce988';

    const trustRoot = ZonaRosaClient.PrivateKey.generate();
    const serverKey = ZonaRosaClient.PrivateKey.generate();

    const serverCert = ZonaRosaClient.ServerCertificate.new(
      1,
      serverKey.getPublicKey(),
      trustRoot
    );

    const expires = 1605722925;
    const senderCert = ZonaRosaClient.SenderCertificate.new(
      aUuid,
      aE164,
      aDeviceId,
      aIdentityKey.getPublicKey(),
      expires,
      serverCert,
      serverKey
    );

    const bPreKeyId = 31337;
    const bSignedPreKeyId = 22;

    const bSignedPreKeySig = bIdentityKey.sign(
      bSPreKey.getPublicKey().serialize()
    );

    const bKyberPrekeyId = 777;
    const bKyberKeyPair = ZonaRosaClient.KEMKeyPair.generate();
    const bKyberPrekeySignature = bIdentityKey.sign(
      bKyberKeyPair.getPublicKey().serialize()
    );

    const bPreKeyBundle = ZonaRosaClient.PreKeyBundle.new(
      0x2000,
      bDeviceId,
      bPreKeyId,
      bPreKey.getPublicKey(),
      bSignedPreKeyId,
      bSPreKey.getPublicKey(),
      bSignedPreKeySig,
      bIdentityKey.getPublicKey(),
      bKyberPrekeyId,
      bKyberKeyPair.getPublicKey(),
      bKyberPrekeySignature
    );

    const bAddress = ZonaRosaClient.ProtocolAddress.new(bUuid, bDeviceId);
    await ZonaRosaClient.processPreKeyBundle(
      bPreKeyBundle,
      bAddress,
      aSess,
      aKeys
    );

    const aAddress = ZonaRosaClient.ProtocolAddress.new(aUuid, aDeviceId);

    const distributionId = 'd1d1d1d1-7000-11eb-b32a-33b8a8a487a6';
    const aSenderKeyStore = new InMemorySenderKeyStore();
    await ZonaRosaClient.SenderKeyDistributionMessage.create(
      aAddress,
      distributionId,
      aSenderKeyStore
    );

    const message = Buffer.from('0a0b0c', 'hex');

    const aCtext = await ZonaRosaClient.groupEncrypt(
      aAddress,
      distributionId,
      aSenderKeyStore,
      message
    );

    const aUsmc = ZonaRosaClient.UnidentifiedSenderMessageContent.new(
      aCtext,
      senderCert,
      ZonaRosaClient.ContentHint.Implicit,
      Buffer.from([42])
    );

    const aSentMessage = await ZonaRosaClient.sealedSenderMultiRecipientEncrypt({
      content: aUsmc,
      recipients: [bAddress],
      excludedRecipients: [
        ZonaRosaClient.ServiceId.parseFromServiceIdString(eUuid),
        ZonaRosaClient.ServiceId.parseFromServiceIdString(mUuid),
      ],
      identityStore: aKeys,
      sessionStore: aSess,
    });

    // Clients can't directly parse arbitrary SSv2 SentMessages, so just check that it contains
    // the excluded recipient service IDs followed by a device ID of 0.
    const hexEncodedSentMessage = Buffer.from(aSentMessage).toString('hex');

    const indexOfE = hexEncodedSentMessage.indexOf(
      Buffer.from(
        ZonaRosaClient.ServiceId.parseFromServiceIdString(
          eUuid
        ).getServiceIdFixedWidthBinary()
      ).toString('hex')
    );
    assert.notEqual(indexOfE, -1);
    assert.equal(aSentMessage[indexOfE / 2 + 17], 0);

    const indexOfM = hexEncodedSentMessage.indexOf(
      Buffer.from(
        ZonaRosaClient.ServiceId.parseFromServiceIdString(
          mUuid
        ).getServiceIdFixedWidthBinary()
      ).toString('hex')
    );
    assert.notEqual(indexOfM, -1);
    assert.equal(aSentMessage[indexOfM / 2 + 17], 0);
  });
});
