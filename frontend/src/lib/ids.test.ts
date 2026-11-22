import { describe, expect, it } from 'vitest';
import { newIdempotencyKey } from './ids';

const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;

describe('newIdempotencyKey', () => {
  it('returns a different UUID v4 every time (the backend accepts only UUID-shaped keys for quotes)', () => {
    const keys = new Set(Array.from({ length: 100 }, newIdempotencyKey));
    expect(keys.size).toBe(100);
    for (const key of keys) expect(key).toMatch(UUID_V4);
  });

  it('also works where crypto.randomUUID is missing (plain http on a LAN address)', () => {
    const original = crypto.randomUUID;
    Object.defineProperty(crypto, 'randomUUID', { value: undefined, configurable: true });
    try {
      expect(newIdempotencyKey()).toMatch(UUID_V4);
    } finally {
      Object.defineProperty(crypto, 'randomUUID', { value: original, configurable: true });
    }
  });
});
