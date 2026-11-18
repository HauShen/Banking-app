import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom';
import GuestOnly from './GuestOnly';
import paths, { rootPaths } from './path';
import RequireAuth from './RequireAuth';
import RequireRole from './RequireRole';

/* ---------------- Lazy loads various components ------------------------- */
const App = lazy(() => import('App'));
const MainLayout = lazy(() => import('layouts/main-layout'));
const AuthLayout = lazy(() => import('layouts/auth-layout'));
const Dashboard = lazy(() => import('pages/dashboard'));
const TransactionsPage = lazy(() => import('pages/transactions'));
const TransferPage = lazy(() => import('pages/transfer'));
const RatesPage = lazy(() => import('pages/rates'));
const ProfilePage = lazy(() => import('pages/profile'));
const AdminUsersPage = lazy(() => import('pages/admin/users'));
const Spinner = lazy(() => import('components/loading/Splash'));
const LoadingProgress = lazy(() => import('components/loading/LoadingProgress'));

const LoginPage = lazy(() => import('pages/authentication/login'));
const SignUpPage = lazy(() => import('pages/authentication/signup'));

const NotFoundPage = lazy(() => import('pages/not-found'));
/* -------------------------------------------------------------------------- */

/**
 * @Defines the routes for the application using React Router.
 */
export const routes = [
  {
    element: (
      <Suspense fallback={<Spinner />}>
        <App />
      </Suspense>
    ),
    children: [
      {
        path: paths.default,
        element: (
          <RequireAuth>
            <MainLayout>
              <Suspense fallback={<LoadingProgress />}>
                <Outlet />
              </Suspense>
            </MainLayout>
          </RequireAuth>
        ),
        children: [
          {
            index: true,
            element: <Dashboard />,
          },
          {
            path: paths.transactions,
            element: <TransactionsPage />,
          },
          {
            path: paths.transfer,
            element: <TransferPage />,
          },
          {
            path: paths.rates,
            element: <RatesPage />,
          },
          {
            path: paths.profile,
            element: <ProfilePage />,
          },
          {
            path: paths.adminUsers,
            element: (
              <RequireRole role="ADMIN">
                <AdminUsersPage />
              </RequireRole>
            ),
          },
        ],
      },
      {
        path: rootPaths.authRoot,
        element: (
          <GuestOnly>
            <AuthLayout />
          </GuestOnly>
        ),
        children: [
          {
            path: paths.login,
            element: <LoginPage />,
          },
          {
            path: paths.signup,
            element: <SignUpPage />,
          },
        ],
      },
      {
        path: rootPaths.errorRoot,
        children: [
          {
            path: paths.notFound,
            element: <NotFoundPage />,
          },
        ],
      },
      {
        path: '*',
        element: <Navigate to={paths.notFound} replace />,
      },
    ],
  },
];

const router = createBrowserRouter(routes);

export default router;
