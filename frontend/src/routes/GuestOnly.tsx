import Splash from 'components/loading/Splash';
import { useAuth } from 'providers/AuthProvider';
import { PropsWithChildren } from 'react';
import { Location, Navigate, useLocation } from 'react-router-dom';
import paths from './path';

/**
 * For the login / sign-up pages: a signed-in user is sent on to the page they wanted
 * (or the dashboard). This is also what moves the user on after a successful login.
 */
const GuestOnly = ({ children }: PropsWithChildren) => {
  const { status } = useAuth();
  const location = useLocation();

  if (status === 'loading') return <Splash />;
  if (status === 'authenticated') {
    const from = (location.state as { from?: Location } | null)?.from;
    return <Navigate to={from ? `${from.pathname}${from.search}` : paths.default} replace />;
  }
  return <>{children}</>;
};

export default GuestOnly;
