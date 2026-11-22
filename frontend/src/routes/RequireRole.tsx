import type { Role } from 'api/types';
import { useAuth } from 'providers/AuthProvider';
import { PropsWithChildren } from 'react';
import { Navigate } from 'react-router-dom';
import paths from './path';

interface RequireRoleProps {
  role: Role;
}

/**
 * Renders its children only for users with this role; others go to the dashboard.
 * Use inside RequireAuth (it assumes the user is signed in). This only hides UI:
 * the backend still checks the role on every request (403).
 */
const RequireRole = ({ role, children }: PropsWithChildren<RequireRoleProps>) => {
  const { user } = useAuth();
  if (user?.role !== role) return <Navigate to={paths.default} replace />;
  return <>{children}</>;
};

export default RequireRole;
