import { Alert, Box, Button, Divider, Stack, Typography } from '@mui/material';
import type { LockedQuote } from 'api/fx';
import type { Account } from 'api/types';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { ReactNode } from 'react';
import LockedRate from './LockedRate';
import { isForeign, ValidTransfer } from './transferValidation';

interface TransferReviewProps {
  transfer: ValidTransfer;
  from: Account;
  sending: boolean;
  problem: string | null;
  onBack: () => void;
  onConfirm: () => void;
  /** Foreign currency only: the locked rate. */
  quote?: {
    locked: LockedQuote | undefined;
    locking: boolean;
    secondsLeft: number;
    onRelock: () => void;
  };
}

const Row = ({ label, children }: { label: string; children: ReactNode }) => (
  <Stack direction="row" justifyContent="space-between" gap={2} sx={{ py: 1.25 }}>
    <Typography sx={{ color: 'text.secondary' }}>{label}</Typography>
    <Typography sx={{ textAlign: 'right', fontWeight: 500, wordBreak: 'break-word' }}>
      {children}
    </Typography>
  </Stack>
);

const TransferReview = ({
  transfer,
  from,
  sending,
  problem,
  onBack,
  onConfirm,
  quote,
}: TransferReviewProps) => {
  const foreign = isForeign(transfer);
  // A foreign transfer needs a locked, unexpired rate.
  const rateReady = !foreign || Boolean(quote?.locked && quote.secondsLeft > 0 && !quote.locking);

  return (
    <Stack gap={3}>
      <Typography sx={{ color: 'text.secondary' }}>
        Check the details. Money moves as soon as you confirm.
      </Typography>
      <Box>
        <Row label="From">{formatAccountNumber(from.accountNumber)}</Row>
        <Divider />
        <Row label="To">{formatAccountNumber(transfer.toAccountNumber)}</Row>
        <Divider />
        <Row label="Amount">{formatMoney(transfer.amount, transfer.currency)}</Row>
        <Divider />
        <Row label="Description">{transfer.description}</Row>
      </Box>
      {foreign && quote && (
        <LockedRate
          quote={quote.locked}
          locking={quote.locking}
          secondsLeft={quote.secondsLeft}
          onRelock={quote.onRelock}
        />
      )}
      {problem && <Alert severity="error">{problem}</Alert>}
      <Stack direction="row" gap={2}>
        <Button variant="outlined" onClick={onBack} disabled={sending}>
          Back
        </Button>
        <Button variant="contained" onClick={onConfirm} disabled={sending || !rateReady}>
          {sending ? 'Sending…' : 'Confirm transfer'}
        </Button>
      </Stack>
    </Stack>
  );
};

export default TransferReview;
