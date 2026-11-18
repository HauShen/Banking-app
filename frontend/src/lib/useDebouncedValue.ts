import { useEffect, useState } from 'react';

/** `value`, but only after it stopped changing for `delayMs` (e.g. quote while the user types). */
export const useDebouncedValue = <T>(value: T, delayMs = 400): T => {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);
  return debounced;
};
