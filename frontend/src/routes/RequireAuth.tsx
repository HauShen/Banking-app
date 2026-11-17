import Splash from 'components/loading/Splash';
import { useAuth } from 'providers/AuthProvider';
import { PropsWithChildren } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import paths from './path';

/** Renders its children only for a signed-in user; otherwise sends them to the login page. */
const RequireAuth = ({ children }: PropsWithChildren) => {
  const { status } = useAuth();
  const location = useLocation();

  if (status === 'loading') return <Splash />;
  if (status === 'anonymous') {
    // Remember where they were going, so login can send them back there.
    return <Navigate to={paths.login} replace state={{ from: location }} />;
  }
  return <>{children}</>;
};

export default RequireAuth;
