import { ThemeProvider } from '@mui/material';
import { QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import { createQueryClient } from 'api/queryClient';
import { http, HttpResponse } from 'msw';
import AuthProvider from 'providers/AuthProvider';
import BreakpointsProvider from 'providers/useBreakpoints';
import { ReactElement } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import GuestOnly from 'routes/GuestOnly';
import paths from 'routes/path';
import RequireAuth from 'routes/RequireAuth';
import { theme } from 'theme/theme';
import { API, server } from './server';

/**
 * Renders the real auth setup (theme, query client, AuthProvider, RequireAuth, GuestOnly) with a
 * stand-in "Dashboard" at / and the given pages under the guest-only routes.
 */
export const renderApp = (startAt: string, guestPages: Record<string, ReactElement>) =>
  render(
    <ThemeProvider theme={theme}>
      <BreakpointsProvider>
        <QueryClientProvider client={createQueryClient()}>
          <MemoryRouter initialEntries={[startAt]}>
            <AuthProvider>
              <Routes>
                <Route
                  path={paths.default}
                  element={
                    <RequireAuth>
                      <p>Dashboard</p>
                    </RequireAuth>
                  }
                />
                {Object.entries(guestPages).map(([path, page]) => (
                  <Route key={path} path={path} element={<GuestOnly>{page}</GuestOnly>} />
                ))}
              </Routes>
            </AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>
      </BreakpointsProvider>
    </ThemeProvider>,
  );

/** No refresh cookie: the app starts signed out. */
export const mockNoSession = () =>
  server.use(
    http.post(`${API}/api/auth/refresh`, () =>
      HttpResponse.json({ status: 401, message: 'Invalid refresh token' }, { status: 401 }),
    ),
  );
