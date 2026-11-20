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
  adminAccounts: `${rootPaths.pagesRoot}admin/accounts`,
  /** Route pattern; build links with adminAccountHistoryPath(accountNumber). */
  adminAccountHistory: `${rootPaths.pagesRoot}admin/accounts/:accountNumber/history`,
  accounts: `${rootPaths.pagesRoot}accounts`,
  login: `${rootPaths.authRoot}/login`,
  signup: `${rootPaths.authRoot}/sign-up`,
  notFound: `${rootPaths.errorRoot}/404`,
};

/** An admin's view of one account's history: "/admin/accounts/1234567890/history". */
export const adminAccountHistoryPath = (accountNumber: string) =>
  paths.adminAccountHistory.replace(':accountNumber', encodeURIComponent(accountNumber));

export default paths;
