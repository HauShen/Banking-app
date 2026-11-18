import * as authApi from 'api/auth';
import { setAuthFailureListener } from 'api/client';
import type { UserProfile } from 'api/types';
import { useQueryClient } from '@tanstack/react-query';
import { createContext, PropsWithChildren, useContext, useEffect, useMemo, useState } from 'react';

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous';

interface AuthContextValue {
  status: AuthStatus;
  user: UserProfile | null;
  login: (username: string, password: string) => Promise<void>;
  register: (input: authApi.RegisterInput) => Promise<void>;
  /** Saves the user's name and email; everything that shows the user updates at once. */
  updateProfile: (update: authApi.ProfileUpdate) => Promise<UserProfile>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const AuthProvider = ({ children }: PropsWithChildren) => {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const [user, setUser] = useState<UserProfile | null>(null);
  const queryClient = useQueryClient();

  const signedIn = (profile: UserProfile) => {
    setUser(profile);
    setStatus('authenticated');
  };
  const signedOut = () => {
    // Drop every cached API response, so the next user never sees this user's accounts or history.
    queryClient.clear();
    setUser(null);
    setStatus('anonymous');
  };

  useEffect(() => {
    let active = true;
    // The API client calls this when a refresh is refused (session expired or revoked).
    setAuthFailureListener(signedOut);

    authApi
      .restoreSession()
      .then((profile) => {
        if (!active) return;
        if (profile) signedIn(profile);
        else signedOut();
      })
      .catch(() => {
        // Backend unreachable: show the login page, which reports the error on submit.
        if (active) signedOut();
      });

    return () => {
      active = false;
      setAuthFailureListener(null);
    };
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      login: async (username, password) => {
        await authApi.login(username, password);
        signedIn(await authApi.getCurrentUser());
      },
      register: async (input) => {
        await authApi.register(input);
        signedIn(await authApi.getCurrentUser());
      },
      updateProfile: async (update) => {
        const updated = await authApi.updateMyProfile(update);
        setUser(updated);
        return updated;
      },
      logout: async () => {
        try {
          await authApi.logout();
        } catch {
          // Token already gone or server unreachable: still sign out in this browser.
        } finally {
          signedOut();
        }
      },
    }),
    [status, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside <AuthProvider>');
  return context;
};

export default AuthProvider;
