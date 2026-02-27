//
// Copyright 2021-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import * as ZonaRosaClient from '../../index.js';
import * as util from '../util.js';

import { assert, use } from 'chai';
import chaiAsPromised from 'chai-as-promised';
import * as uuid from 'uuid';
import { Buffer } from 'node:buffer';

use(chaiAsPromised);
util.initLogger();

describe('ProtocolAddress', () => {
  it('can hold arbitrary name', () => {
    const addr = ZonaRosaClient.ProtocolAddress.new('name', 42);
    assert.deepEqual(addr.name(), 'name');
    assert.deepEqual(addr.deviceId(), 42);
  });
  it('can round-trip ServiceIds', () => {
    const newUuid = uuid.v4();
    const aci = ZonaRosaClient.Aci.fromUuid(newUuid);
    const pni = ZonaRosaClient.Pni.fromUuid(newUuid);

    const aciAddr = ZonaRosaClient.ProtocolAddress.new(aci, 1);
    const pniAddr = ZonaRosaClient.ProtocolAddress.new(pni, 1);
    assert.notEqual(aciAddr.toString(), pniAddr.toString());
    assert.isTrue(aciAddr.serviceId()?.isEqual(aci));
    assert.isTrue(pniAddr.serviceId()?.isEqual(pni));
  });

  it('rejects out-of-range device IDs', () => {
    assert.throws(
      () => ZonaRosaClient.ProtocolAddress.new('name', 128),
      'invalid: name.128'
    );
  });
});

describe('ServiceId', () => {
  const testingUuid = '8c78cd2a-16ff-427d-83dc-1a5e36ce713d';

  it('handles ACIs', () => {
    const aci = ZonaRosaClient.Aci.fromUuid(testingUuid);
    assert.instanceOf(aci, ZonaRosaClient.Aci);
    assert.isTrue(
      aci.isEqual(ZonaRosaClient.Aci.fromUuidBytes(uuid.parse(testingUuid)))
    );
    assert.isFalse(aci.isEqual(ZonaRosaClient.Pni.fromUuid(testingUuid)));

    assert.deepEqual(testingUuid, aci.getRawUuid());
    assert.deepEqual(uuid.parse(testingUuid), aci.getRawUuidBytes());
    assert.deepEqual(testingUuid, aci.getServiceIdString());
    assert.deepEqual(uuid.parse(testingUuid), aci.getServiceIdBinary());
    assert.deepEqual(`<ACI:${testingUuid}>`, `${aci}`);

    {
      const aciServiceId = ZonaRosaClient.ServiceId.parseFromServiceIdString(
        aci.getServiceIdString()
      );
      assert.instanceOf(aciServiceId, ZonaRosaClient.Aci);
      assert.deepEqual(aci, aciServiceId);

      const _: ZonaRosaClient.Aci = ZonaRosaClient.Aci.parseFromServiceIdString(
        aci.getServiceIdString()
      );
    }

    {
      const aciServiceId = ZonaRosaClient.ServiceId.parseFromServiceIdBinary(
        aci.getServiceIdBinary()
      );
      assert.instanceOf(aciServiceId, ZonaRosaClient.Aci);
      assert.deepEqual(aci, aciServiceId);

      const _: ZonaRosaClient.Aci = ZonaRosaClient.Aci.parseFromServiceIdBinary(
        aci.getServiceIdBinary()
      );
    }
  });
  it('handles PNIs', () => {
    const pni = ZonaRosaClient.Pni.fromUuid(testingUuid);
    assert.instanceOf(pni, ZonaRosaClient.Pni);
    assert.isTrue(
      pni.isEqual(ZonaRosaClient.Pni.fromUuidBytes(uuid.parse(testingUuid)))
    );
    assert.isFalse(pni.isEqual(ZonaRosaClient.Aci.fromUuid(testingUuid)));

    assert.deepEqual(testingUuid, pni.getRawUuid());
    assert.deepEqual(uuid.parse(testingUuid), pni.getRawUuidBytes());
    assert.deepEqual(`PNI:${testingUuid}`, pni.getServiceIdString());
    assert.deepEqual(
      Buffer.concat([Buffer.of(0x01), pni.getRawUuidBytes()]),
      pni.getServiceIdBinary()
    );
    assert.deepEqual(`<PNI:${testingUuid}>`, `${pni}`);

    {
      const pniServiceId = ZonaRosaClient.ServiceId.parseFromServiceIdString(
        pni.getServiceIdString()
      );
      assert.instanceOf(pniServiceId, ZonaRosaClient.Pni);
      assert.deepEqual(pni, pniServiceId);

      const _: ZonaRosaClient.Pni = ZonaRosaClient.Pni.parseFromServiceIdString(
        pni.getServiceIdString()
      );
    }

    {
      const pniServiceId = ZonaRosaClient.ServiceId.parseFromServiceIdBinary(
        pni.getServiceIdBinary()
      );
      assert.instanceOf(pniServiceId, ZonaRosaClient.Pni);
      assert.deepEqual(pni, pniServiceId);

      const _: ZonaRosaClient.Pni = ZonaRosaClient.Pni.parseFromServiceIdBinary(
        pni.getServiceIdBinary()
      );
    }
  });
  it('accepts the null UUID', () => {
    ZonaRosaClient.ServiceId.parseFromServiceIdString(uuid.NIL);
  });
  it('rejects invalid values', () => {
    assert.throws(() =>
      ZonaRosaClient.ServiceId.parseFromServiceIdBinary(Buffer.of())
    );
    assert.throws(() => ZonaRosaClient.ServiceId.parseFromServiceIdString(''));
  });
  it('follows the standard ordering', () => {
    const original = [
      ZonaRosaClient.Aci.fromUuid(uuid.NIL),
      ZonaRosaClient.Aci.fromUuid(testingUuid),
      ZonaRosaClient.Pni.fromUuid(uuid.NIL),
      ZonaRosaClient.Pni.fromUuid(testingUuid),
    ];
    const ids = util.shuffled(original);
    ids.sort(ZonaRosaClient.ServiceId.comparator);
    assert.deepEqual(ids, original);
  });
});
