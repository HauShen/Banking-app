/**
 * Query keys in one place. Invalidate by prefix after a mutation,
 * e.g. queryClient.invalidateQueries({ queryKey: queryKeys.accounts.all }).
 */
export const queryKeys = {
  accounts: {
    all: ['accounts'] as const,
    summary: () => ['accounts', 'summary'] as const,
    /** Under 'accounts', so everything that refreshes the balances (transfer, top-up) refreshes it too. */
    balanceHistory: (accountNumber: string, days: number) =>
      ['accounts', 'balance-history', accountNumber, days] as const,
  },
  admin: {
    accounts: {
      all: ['admin', 'accounts'] as const,
      byNumber: (accountNumber: string) => ['admin', 'accounts', 'number', accountNumber] as const,
      list: (filter: { status?: string; userId?: string }) =>
        ['admin', 'accounts', 'list', filter] as const,
    },
    user: (id: string) => ['admin', 'users', 'one', id] as const,
    users: {
      all: ['admin', 'users'] as const,
      list: (params: object) => ['admin', 'users', 'list', params] as const,
    },
  },
  fx: {
    rates: () => ['fx', 'rates'] as const,
    quote: (currency: string, amount: string, direction: string) =>
      ['fx', 'quote', currency, amount, direction] as const,
  },
  transactions: {
    all: ['transactions'] as const,
    byAccount: (accountNumber: string, page: number, size: number) =>
      ['transactions', 'account', accountNumber, { page, size }] as const,
  },
};
