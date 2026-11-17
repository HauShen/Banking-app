import { Alert, Button, Card, Skeleton, Stack, Typography } from '@mui/material';
import type { Account, AccountSummary } from 'api/types';
import { errorMessage } from 'lib/errors';
import { formatMoney } from 'lib/money';
import { useAuth } from 'providers/AuthProvider';
import SimpleBar from 'simplebar-react';
import AccountCard from './AccountCard';

interface MyAccountsProps {
  summary: AccountSummary | undefined;
  isLoading: boolean;
  error: unknown;
  onRetry: () => void;
  selectedAccountNumber: string | undefined;
  onSelect: (accountNumber: string) => void;
}

const Heading = ({ summary }: { summary?: AccountSummary }) => (
  <Stack
    direction="row"
    justifyContent="space-between"
    alignItems="baseline"
    sx={{ pt: 3, pb: 2.5 }}
  >
    <Typography
      sx={{
        fontSize: { xs: 'body2.fontSize', md: 'h6.fontSize', xl: 'h3.fontSize' },
        fontWeight: 600,
      }}
    >
      My Accounts
    </Typography>
    {summary && summary.accounts.length > 0 && (
      <Typography
        sx={{ color: 'text.secondary', fontSize: { xs: 'caption.fontSize', md: 'body1.fontSize' } }}
      >
        Total balance{' '}
        <Typography
          component="span"
          sx={{ color: 'text.primary', fontWeight: 600, font: 'inherit' }}
        >
          {formatMoney(summary.balance)}
        </Typography>
      </Typography>
    )}
  </Stack>
);

/** Shown to a user without accounts. Only admins open accounts, so tell customers who does. */
const NoAccounts = ({ isAdmin }: { isAdmin: boolean }) => (
  <Card sx={{ p: { xs: 3, md: 5 }, textAlign: 'center' }}>
    <Typography variant="h5" sx={{ mb: 1 }}>
      No account yet
    </Typography>
    <Typography sx={{ color: 'text.secondary' }}>
      {isAdmin
        ? 'Administrators manage customers’ accounts and have none of their own by default.'
        : 'An administrator opens accounts for new customers. Your accounts will show up here.'}
    </Typography>
  </Card>
);

const MyAccounts = ({
  summary,
  isLoading,
  error,
  onRetry,
  selectedAccountNumber,
  onSelect,
}: MyAccountsProps) => {
  const { user } = useAuth();

  let body;
  if (isLoading) {
    body = (
      <Stack direction="row" gap={4} aria-label="Loading accounts">
        <Skeleton variant="rounded" height={235} sx={{ flex: 1 }} />
        <Skeleton variant="rounded" height={235} sx={{ flex: 1 }} />
      </Stack>
    );
  } else if (error) {
    body = (
      <Alert
        severity="error"
        action={
          <Button color="inherit" size="small" onClick={onRetry}>
            Try again
          </Button>
        }
      >
        {errorMessage(error)}
      </Alert>
    );
  } else if (!summary || summary.accounts.length === 0) {
    body = <NoAccounts isAdmin={user?.role === 'ADMIN'} />;
  } else {
    body = (
      <SimpleBar style={{ maxWidth: '100%', overflowX: 'auto' }}>
        <Stack direction="row" gap={4} sx={{ pb: 1 }}>
          {summary.accounts.map((account: Account) => (
            <AccountCard
              key={account.accountNumber}
              account={account}
              holderName={user?.fullName ?? ''}
              selected={account.accountNumber === selectedAccountNumber}
              onSelect={() => onSelect(account.accountNumber)}
            />
          ))}
        </Stack>
      </SimpleBar>
    );
  }

  return (
    <>
      <Heading summary={summary} />
      {body}
    </>
  );
};

export default MyAccounts;
