import { render, screen } from '@testing-library/react';
import type { Role, UserProfile } from 'api/types';
import { useAuth } from 'providers/AuthProvider';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import RequireRole from './RequireRole';

vi.mock('providers/AuthProvider', () => ({ useAuth: vi.fn() }));

const signedInAs = (role: Role) => {
  vi.mocked(useAuth).mockReturnValue({
    status: 'authenticated',
    user: { username: 'someone', role } as UserProfile,
    login: vi.fn(),
    updateProfile: vi.fn(),
    logout: vi.fn(),
  });
};

const renderAdminRoute = () =>
  render(
    <MemoryRouter initialEntries={['/admin']}>
      <Routes>
        <Route path="/" element={<p>Dashboard</p>} />
        <Route
          path="/admin"
          element={
            <RequireRole role="ADMIN">
              <p>Admin area</p>
            </RequireRole>
          }
        />
      </Routes>
    </MemoryRouter>,
  );

describe('RequireRole', () => {
  it('shows the page to a user with the role', () => {
    signedInAs('ADMIN');
    renderAdminRoute();
    expect(screen.getByText('Admin area')).toBeInTheDocument();
  });

  it('sends other users to the dashboard', () => {
    signedInAs('CUSTOMER');
    renderAdminRoute();
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
    expect(screen.queryByText('Admin area')).not.toBeInTheDocument();
  });
});
