//
// Copyright 2021-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

/* eslint-disable @typescript-eslint/require-await */

import * as ZonaRosaClient from '../../index.js';
import * as util from '../util.js';

util.initLogger();

export class InMemorySessionStore extends ZonaRosaClient.SessionStore {
  private state = new Map<string, Uint8Array>();
  async saveSession(
    name: ZonaRosaClient.ProtocolAddress,
    record: ZonaRosaClient.SessionRecord
  ): Promise<void> {
    const idx = `${name.name()}::${name.deviceId()}`;
    this.state.set(idx, record.serialize());
  }
  async getSession(
    name: ZonaRosaClient.ProtocolAddress
  ): Promise<ZonaRosaClient.SessionRecord | null> {
    const idx = `${name.name()}::${name.deviceId()}`;
    const serialized = this.state.get(idx);
    if (serialized) {
      return ZonaRosaClient.SessionRecord.deserialize(serialized);
    } else {
      return null;
    }
  }
  async getExistingSessions(
    addresses: ZonaRosaClient.ProtocolAddress[]
  ): Promise<ZonaRosaClient.SessionRecord[]> {
    return addresses.map((address) => {
      const idx = `${address.name()}::${address.deviceId()}`;
      const serialized = this.state.get(idx);
      if (!serialized) {
        throw new Error(`no session for ${idx}`);
      }
      return ZonaRosaClient.SessionRecord.deserialize(serialized);
    });
  }
}

export class InMemoryIdentityKeyStore extends ZonaRosaClient.IdentityKeyStore {
  private idKeys = new Map<string, ZonaRosaClient.PublicKey>();
  private localRegistrationId: number;
  private identityKey: ZonaRosaClient.PrivateKey;

  constructor(localRegistrationId?: number) {
    super();
    this.identityKey = ZonaRosaClient.PrivateKey.generate();
    this.localRegistrationId = localRegistrationId ?? 5;
  }

  async getIdentityKey(): Promise<ZonaRosaClient.PrivateKey> {
    return this.identityKey;
  }
  async getLocalRegistrationId(): Promise<number> {
    return this.localRegistrationId;
  }

  async isTrustedIdentity(
    name: ZonaRosaClient.ProtocolAddress,
    key: ZonaRosaClient.PublicKey,
    _direction: ZonaRosaClient.Direction
  ): Promise<boolean> {
    const idx = `${name.name()}::${name.deviceId()}`;
    const currentKey = this.idKeys.get(idx);
    if (currentKey) {
      return currentKey.equals(key);
    } else {
      return true;
    }
  }

  async saveIdentity(
    name: ZonaRosaClient.ProtocolAddress,
    key: ZonaRosaClient.PublicKey
  ): Promise<ZonaRosaClient.IdentityChange> {
    const idx = `${name.name()}::${name.deviceId()}`;
    const currentKey = this.idKeys.get(idx);
    this.idKeys.set(idx, key);

    const changed = !(currentKey?.equals(key) ?? true);
    return changed
      ? ZonaRosaClient.IdentityChange.ReplacedExisting
      : ZonaRosaClient.IdentityChange.NewOrUnchanged;
  }
  async getIdentity(
    name: ZonaRosaClient.ProtocolAddress
  ): Promise<ZonaRosaClient.PublicKey | null> {
    const idx = `${name.name()}::${name.deviceId()}`;
    return this.idKeys.get(idx) ?? null;
  }
}

export class InMemoryPreKeyStore extends ZonaRosaClient.PreKeyStore {
  private state = new Map<number, Uint8Array>();
  async savePreKey(
    id: number,
    record: ZonaRosaClient.PreKeyRecord
  ): Promise<void> {
    this.state.set(id, record.serialize());
  }
  async getPreKey(id: number): Promise<ZonaRosaClient.PreKeyRecord> {
    const record = this.state.get(id);
    if (!record) {
      throw new Error(`pre-key ${id} not found`);
    }
    return ZonaRosaClient.PreKeyRecord.deserialize(record);
  }
  async removePreKey(id: number): Promise<void> {
    this.state.delete(id);
  }
}

export class InMemorySignedPreKeyStore extends ZonaRosaClient.SignedPreKeyStore {
  private state = new Map<number, Uint8Array>();
  async saveSignedPreKey(
    id: number,
    record: ZonaRosaClient.SignedPreKeyRecord
  ): Promise<void> {
    this.state.set(id, record.serialize());
  }
  async getSignedPreKey(id: number): Promise<ZonaRosaClient.SignedPreKeyRecord> {
    const record = this.state.get(id);
    if (!record) {
      throw new Error(`pre-key ${id} not found`);
    }
    return ZonaRosaClient.SignedPreKeyRecord.deserialize(record);
  }
}

export class InMemoryKyberPreKeyStore extends ZonaRosaClient.KyberPreKeyStore {
  private state = new Map<number, Uint8Array>();
  private used = new Set<number>();
  private baseKeysSeen = new Map<bigint, ZonaRosaClient.PublicKey[]>();
  async saveKyberPreKey(
    id: number,
    record: ZonaRosaClient.KyberPreKeyRecord
  ): Promise<void> {
    this.state.set(id, record.serialize());
  }
  async getKyberPreKey(id: number): Promise<ZonaRosaClient.KyberPreKeyRecord> {
    const record = this.state.get(id);
    if (!record) {
      throw new Error(`kyber pre-key ${id} not found`);
    }
    return ZonaRosaClient.KyberPreKeyRecord.deserialize(record);
  }
  async markKyberPreKeyUsed(
    id: number,
    signedPreKeyId: number,
    baseKey: ZonaRosaClient.PublicKey
  ): Promise<void> {
    this.used.add(id);
    const bothKeyIds = (BigInt(id) << 32n) | BigInt(signedPreKeyId);
    const baseKeysSeen = this.baseKeysSeen.get(bothKeyIds);
    if (!baseKeysSeen) {
      this.baseKeysSeen.set(bothKeyIds, [baseKey]);
    } else if (baseKeysSeen.every((key) => !key.equals(baseKey))) {
      baseKeysSeen.push(baseKey);
    } else {
      throw new Error('reused base key');
    }
  }
  async hasKyberPreKeyBeenUsed(id: number): Promise<boolean> {
    return this.used.has(id);
  }
}

export class InMemorySenderKeyStore extends ZonaRosaClient.SenderKeyStore {
  private state = new Map<string, ZonaRosaClient.SenderKeyRecord>();
  async saveSenderKey(
    sender: ZonaRosaClient.ProtocolAddress,
    distributionId: ZonaRosaClient.Uuid,
    record: ZonaRosaClient.SenderKeyRecord
  ): Promise<void> {
    const idx = `${distributionId}::${sender.name()}::${sender.deviceId()}`;
    this.state.set(idx, record);
  }
  async getSenderKey(
    sender: ZonaRosaClient.ProtocolAddress,
    distributionId: ZonaRosaClient.Uuid
  ): Promise<ZonaRosaClient.SenderKeyRecord | null> {
    const idx = `${distributionId}::${sender.name()}::${sender.deviceId()}`;
    return this.state.get(idx) ?? null;
  }
}

export default class TestStores {
  sender: InMemorySenderKeyStore;
  prekey: InMemoryPreKeyStore;
  signed: InMemorySignedPreKeyStore;
  kyber: InMemoryKyberPreKeyStore;
  identity: InMemoryIdentityKeyStore;
  session: InMemorySessionStore;

  constructor() {
    this.sender = new InMemorySenderKeyStore();
    this.prekey = new InMemoryPreKeyStore();
    this.signed = new InMemorySignedPreKeyStore();
    this.kyber = new InMemoryKyberPreKeyStore();
    this.identity = new InMemoryIdentityKeyStore();
    this.session = new InMemorySessionStore();
  }
}
