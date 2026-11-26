import { useQuery } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';

export interface DemoLogin {
  username: string;
  password: string;
}

/** DemoLoginsResponse.java: the live demo's public logins. Only the demo answers; elsewhere it's a 404. */
export interface DemoLogins {
  /** The customer the tour starts with. */
  customer: DemoLogin;
  admin: DemoLogin;
  /** The other demo customers; they use the customer's password. */
  otherCustomers: string[];
  /** Local time of the nightly reset, e.g. "04:00"; null when the reset is switched off. */
  resetTime: string | null;
  /** The time zone `resetTime` is in, e.g. "Asia/Kuala_Lumpur". */
  timeZone: string;
}

/** GET /api/demo/logins, public: the visitor isn't logged in yet. */
export const getDemoLogins = (): Promise<DemoLogins> =>
  apiFetch<DemoLogins>('/api/demo/logins', { auth: false });

/**
 * Asked once per page load, never retried: outside the live demo the 404 is the expected answer and simply means
 * "no demo box".
 */
export const useDemoLogins = () =>
  useQuery({
    queryKey: queryKeys.demo.logins(),
    queryFn: getDemoLogins,
    retry: false,
    staleTime: Infinity,
  });
