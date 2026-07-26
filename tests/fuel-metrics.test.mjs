import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateFuelIntervals,
  compareFuelEntries,
  normalizeBoolean,
  normalizeFuelEntry,
} from '../modules/fuel-metrics.js';

const entry = (id, odometer, liters, full, extra = {}) => ({
  id,
  vehicleId: 'v1',
  date: `2026-01-${String(Number(id.replace(/\D/g, '')) || 1).padStart(2, '0')}`,
  time: '10:00',
  odometer,
  liters,
  total: liters * 30,
  full,
  ...extra,
});

test('legacy records without full are migrated as full-tank records', () => {
  assert.equal(normalizeFuelEntry({ odometer: '1000', liters: '40', total: '1200' }).full, true);
});

test('normalizes boolean values from local, Cloud and import formats', () => {
  for (const value of [true, 1, '1', 'true', 'yes', 'on', 'full']) assert.equal(normalizeBoolean(value), true);
  for (const value of [false, 0, '0', 'false', 'no', 'off', 'partial']) assert.equal(normalizeBoolean(value, true), false);
});

test('calculates an adjacent full-to-full interval', () => {
  const result = calculateFuelIntervals([entry('1', 1000, 40, true), entry('2', 1500, 50, true)]);
  assert.equal(result.length, 1);
  assert.equal(result[0].distance, 500);
  assert.equal(result[0].liters, 50);
  assert.equal(result[0].kml, 10);
});

test('aggregates partial fills until the next full tank', () => {
  const result = calculateFuelIntervals([
    entry('1', 1000, 40, true),
    entry('2', 1200, 20, false),
    entry('3', 1500, 30, true),
  ]);
  assert.equal(result.length, 1);
  assert.equal(result[0].distance, 500);
  assert.equal(result[0].liters, 50);
  assert.equal(result[0].kml, 10);
  assert.equal(result[0].partialFillCount, 1);
});

test('invalidates an interval when a previous fill was missed', () => {
  const result = calculateFuelIntervals([
    entry('1', 1000, 40, true),
    entry('2', 1200, 20, false, { previousFillMissed: true }),
    entry('3', 1500, 30, true),
    entry('4', 2000, 50, true),
  ]);
  assert.equal(result.length, 1);
  assert.equal(result[0].startId, '3');
  assert.equal(result[0].endId, '4');
});

test('sorts same-day records by time before odometer', () => {
  const list = [
    { date: '2026-01-01', time: '18:00', odometer: 1200 },
    { date: '2026-01-01', time: '08:00', odometer: 1000 },
  ].sort(compareFuelEntries);
  assert.equal(list[0].time, '08:00');
});

test('does not calculate across a non-increasing odometer anomaly', () => {
  const result = calculateFuelIntervals([
    entry('1', 1000, 40, true),
    entry('2', 900, 20, false),
    entry('3', 1500, 50, true),
    entry('4', 2000, 50, true),
  ]);
  assert.equal(result.length, 1);
  assert.equal(result[0].startId, '3');
  assert.equal(result[0].kml, 10);
});
