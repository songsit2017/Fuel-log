const TRUE_VALUES = new Set(['1', 'true', 'yes', 'y', 'on', 'full', 'เต็ม', 'เต็มถัง']);
const FALSE_VALUES = new Set(['0', 'false', 'no', 'n', 'off', 'partial', 'ไม่เต็ม', 'ไม่เต็มถัง']);

export function normalizeBoolean(value, fallback = false) {
  if (typeof value === 'boolean') return value;
  if (typeof value === 'number') return value !== 0;
  const normalized = String(value ?? '').trim().toLowerCase();
  if (TRUE_VALUES.has(normalized)) return true;
  if (FALSE_VALUES.has(normalized)) return false;
  return fallback;
}

export function normalizeFuelEntry(entry = {}) {
  const number = value => {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : 0;
  };
  const discount = Math.max(0, number(entry.discount));
  const total = Math.max(0, number(entry.total));
  const gross = number(entry.grossTotal);
  return {
    ...entry,
    odometer: number(entry.odometer),
    liters: Math.max(0, number(entry.liters)),
    pricePerLiter: Math.max(0, number(entry.pricePerLiter)),
    discount,
    grossTotal: gross > 0 ? gross : total + discount,
    total,
    // Legacy FuelLog entries predate the full field and were full-tank records.
    full: normalizeBoolean(entry.full, true),
    previousFillMissed: normalizeBoolean(entry.previousFillMissed, false),
  };
}

export function compareFuelEntries(a, b) {
  const dateCompare = String(a?.date || '').localeCompare(String(b?.date || ''));
  if (dateCompare) return dateCompare;
  const timeCompare = String(a?.time || '').localeCompare(String(b?.time || ''));
  if (timeCompare) return timeCompare;
  return (Number(a?.odometer) || 0) - (Number(b?.odometer) || 0);
}

export function calculateFuelIntervals(input = [], options = {}) {
  const minEfficiency = options.minEfficiency ?? 1;
  const maxEfficiency = options.maxEfficiency ?? 100;
  const list = input.map(normalizeFuelEntry).sort(compareFuelEntries);
  const intervals = [];
  let anchor = null;
  let liters = 0;
  let cost = 0;
  let invalid = false;
  let partialFillCount = 0;

  for (const entry of list) {
    if (!anchor) {
      if (entry.full) anchor = entry;
      continue;
    }

    const distance = entry.odometer - anchor.odometer;
    if (distance <= 0) {
      if (entry.full) {
        anchor = entry;
        liters = 0;
        cost = 0;
        invalid = false;
        partialFillCount = 0;
      } else {
        invalid = true;
      }
      continue;
    }

    liters += entry.liters;
    cost += entry.total;
    if (entry.previousFillMissed) invalid = true;
    if (!entry.full) partialFillCount++;

    if (entry.full) {
      const efficiency = liters > 0 ? distance / liters : 0;
      if (!invalid && liters > 0 && efficiency >= minEfficiency && efficiency <= maxEfficiency) {
        intervals.push({
          id: entry.id,
          startId: anchor.id,
          endId: entry.id,
          date: entry.date,
          distance,
          liters,
          cost,
          kml: efficiency,
          costPerKm: cost / distance,
          partialFillCount,
        });
      }
      anchor = entry;
      liters = 0;
      cost = 0;
      invalid = false;
      partialFillCount = 0;
    }
  }

  return intervals;
}
