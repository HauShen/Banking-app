import { Alert, Box, Button, Divider, Stack, Typography } from '@mui/material';
import type { Account } from 'api/types';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { ReactNode } from 'react';
import { ValidTransfer } from './transferValidation';

interface TransferReviewProps {
  transfer: ValidTransfer;
  from: Account;
  sending: boolean;
  problem: string | null;
  onBack: () => void;
  onConfirm: () => void;
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
}: TransferReviewProps) => (
  <Stack gap={3}>
    <Typography sx={{ color: 'text.secondary' }}>
      Check the details. Money moves as soon as you confirm.
    </Typography>
    <Box>
      <Row label="From">{formatAccountNumber(from.accountNumber)}</Row>
      <Divider />
      <Row label="To">{formatAccountNumber(transfer.toAccountNumber)}</Row>
      <Divider />
      <Row label="Amount">{formatMoney(transfer.amount)}</Row>
      <Divider />
      <Row label="Description">{transfer.description}</Row>
    </Box>
    {problem && <Alert severity="error">{problem}</Alert>}
    <Stack direction="row" gap={2}>
      <Button variant="outlined" onClick={onBack} disabled={sending}>
        Back
      </Button>
      <Button variant="contained" onClick={onConfirm} disabled={sending}>
        {sending ? 'Sending…' : 'Confirm transfer'}
      </Button>
    </Stack>
  </Stack>
);

export default TransferReview;
