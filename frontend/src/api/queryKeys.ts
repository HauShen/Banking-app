/**
 * Query keys in one place. Invalidate by prefix after a mutation,
 * e.g. queryClient.invalidateQueries({ queryKey: queryKeys.accounts.all }).
 */
export const queryKeys = {
  accounts: {
    all: ['accounts'] as const,
    summary: () => ['accounts', 'summary'] as const,
  },
  transactions: {
    all: ['transactions'] as const,
    byAccount: (accountNumber: string, page: number, size: number) =>
      ['transactions', 'account', accountNumber, { page, size }] as const,
  },
};
