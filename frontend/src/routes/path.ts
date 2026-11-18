export const rootPaths = {
  root: '/',
  pagesRoot: '/',
  authRoot: '/authentication',
  errorRoot: '/error',
};

/**
 * Object containing various paths used in the application.
 */
const paths = {
  default: `${rootPaths.root}`,
  transactions: `${rootPaths.pagesRoot}transactions`,
  transfer: `${rootPaths.pagesRoot}transfer`,
  rates: `${rootPaths.pagesRoot}rates`,
  profile: `${rootPaths.pagesRoot}profile`,
  adminUsers: `${rootPaths.pagesRoot}admin/users`,
  accounts: `${rootPaths.pagesRoot}accounts`,
  login: `${rootPaths.authRoot}/login`,
  signup: `${rootPaths.authRoot}/sign-up`,
  notFound: `${rootPaths.errorRoot}/404`,
};

export default paths;
