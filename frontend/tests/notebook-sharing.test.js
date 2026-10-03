const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { emptyGrant, grantCapabilities, grantPayload, recordWithinGrant,
  boundedGrantRange, validSharedRange, draftFromGrant } = require('../utils/notebook-permissions');

const root = path.join(__dirname, '..');
const source = (name) => fs.readFileSync(path.join(root, name), 'utf8');

test('new sharing grant defaults to read-only with distinct data and validity dates', () => {
  const draft = emptyGrant('2026-10-03', 'Asia/Shanghai');
  assert.equal(draft.canCreate, false);
  assert.equal(draft.canEdit, false);
  assert.equal(draft.canExport, false);
  assert.equal(draft.dataFrom, '2026-10-03');
  assert.equal(draft.validFromDate, '2026-10-03');
  assert.notEqual(draft.dataTo, draft.validToDate);
  assert.equal(draft.dataTimeZone, 'Asia/Shanghai');
});

test('sharing payload keeps data range separate from authorization validity', () => {
  const payload = grantPayload({ ...emptyGrant('2026-01-01', 'Asia/Shanghai'),
    granteeUserId: 9, dataTo: '2026-02-01', validToDate: '2026-03-01', canExport: true }, 12);
  assert.equal(payload.dataTo, '2026-02-01');
  assert.equal(payload.canExport, true);
  assert.equal(payload.canCreate, false);
  assert.equal(payload.validTo.startsWith('2026-03-02T'), true);
  assert.throws(() => grantPayload({ ...payload, dataFrom: '2026-03-01', dataTo: '2026-02-01' }, 12));
});

test('revoked and expired grants expose no shared capability', () => {
  const active = { status: 'ACTIVE', canCreate: true, canEdit: false, canExport: true,
    validFrom: '2026-01-01T00:00:00Z', validTo: '2026-12-01T00:00:00Z' };
  assert.deepEqual(grantCapabilities(active, '2026-06-01T00:00:00Z'),
    { read: true, create: true, edit: false, export: true });
  assert.deepEqual(grantCapabilities({ ...active, status: 'REVOKED' }, '2026-06-01T00:00:00Z'),
    { read: false, create: false, edit: false, export: false });
  assert.equal(grantCapabilities(active, '2027-01-01T00:00:00Z').read, false);
});

test('record edit visibility requires complete containment in a live editable grant', () => {
  const grant = { status: 'ACTIVE', canEdit: true, dataFrom: '2026-01-01',
    dataTo: '2026-01-31', dataTimeZone: 'Asia/Shanghai',
    validFrom: '2026-01-01T00:00:00Z', validTo: '2030-01-01T00:00:00Z' };
  assert.equal(recordWithinGrant(grant, { occurredFrom: '2026-01-10T10:00:00+08:00',
    occurredTo: '2026-01-11T10:00:00+08:00' }, '2026-06-01T00:00:00Z'), true);
  assert.equal(recordWithinGrant(grant, { occurredFrom: '2026-01-31T10:00:00+08:00',
    occurredTo: '2026-02-01T10:00:00+08:00' }, '2026-06-01T00:00:00Z'), false);
  assert.equal(recordWithinGrant({ ...grant, canEdit: false }, { occurredFrom: '2026-01-10T10:00:00+08:00',
    occurredTo: '2026-01-11T10:00:00+08:00' }, '2026-06-01T00:00:00Z'), false);
});

test('shared browsing clamps an old grant to the current platform month limit', () => {
  const grant = { dataFrom: '2024-01-01', dataTo: '2026-12-31' };
  assert.deepEqual(boundedGrantRange(grant, 12), { from: '2026-01-01', to: '2026-12-31' });
  assert.equal(validSharedRange(grant, '2025-01-01', '2026-12-31', 12), false);
  assert.equal(validSharedRange(grant, '2026-05-01', '2026-06-30', 12), true);
  assert.equal(validSharedRange(grant, '2023-12-01', '2024-01-31', 12), false);
});

test('editing a grant converts exclusive validity end back to its inclusive local date', () => {
  const draft = draftFromGrant({ granteeUserId: 9, dataFrom: '2026-01-01',
    dataTo: '2026-02-01', dataTimeZone: 'Asia/Shanghai',
    validFrom: '2026-01-01T00:00:00+08:00', validTo: '2026-03-02T00:00:00+08:00',
    canCreate: false, canEdit: true, canExport: false });
  assert.equal(draft.validFromDate, '2026-01-01');
  assert.equal(draft.validToDate, '2026-03-01');
  assert.equal(draft.canEdit, true);
});

test('contact confirmation, external account selection, sharing and shared pages have guarded actions', () => {
  const contacts = source('pages/notebook/detail/contacts/index.js');
  const sharing = source('pages/notebook/detail/sharing/index.js');
  const shared = source('pages/notebook/detail/shared/index.js');
  assert.match(contacts, /acceptInvite/);
  assert.match(contacts, /removeContact/);
  assert.match(sharing, /listContacts/);
  assert.match(sharing, /createGrant/);
  assert.match(sharing, /revokeGrant/);
  assert.match(shared, /grantCapabilities/);
  assert.match(shared, /capabilities\.create/);
  assert.match(shared, /capabilities\.export/);
  assert.match(source('pages/notebook/detail/record-detail/index.js'), /owner/);
  assert.doesNotMatch(shared, /deleteRecord|publishTemplate|createGrant/);
});
