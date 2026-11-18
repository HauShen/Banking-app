import { useEffect, useState } from 'react';

/** Whole seconds left until `deadline` (ms since epoch), updated every second; 0 when passed. */
export const useCountdown = (deadline: number | undefined): number => {
  const secondsLeft = () => (deadline ? Math.max(0, Math.ceil((deadline - Date.now()) / 1000)) : 0);
  const [left, setLeft] = useState(secondsLeft);

  useEffect(() => {
    setLeft(secondsLeft());
    if (!deadline) return;
    const timer = setInterval(() => {
      const next = secondsLeft();
      setLeft(next);
      if (next === 0) clearInterval(timer);
    }, 250);
    return () => clearInterval(timer);
  }, [deadline]);

  return left;
};

/** Quote validity in the backend (fx.quote-validity). Used when the browser clock disagrees with the server's. */
const QUOTE_VALIDITY_MS = 60_000;
/** Stop offering a quote a little early, so it doesn't expire while the transfer is on its way. */
const SAFETY_MARGIN_MS = 2_000;

/**
 * When to treat a locked quote as expired, in this browser's clock. `expiresAt` comes from the server;
 * if the two clocks disagree badly (remaining time not between 0 and 2 minutes), count 60 s from now.
 */
export const quoteDeadline = (expiresAt: string, receivedAt = Date.now()): number => {
  const remaining = new Date(expiresAt).getTime() - receivedAt;
  const trusted =
    remaining > 0 && remaining <= 2 * QUOTE_VALIDITY_MS ? remaining : QUOTE_VALIDITY_MS;
  return receivedAt + trusted - SAFETY_MARGIN_MS;
};
