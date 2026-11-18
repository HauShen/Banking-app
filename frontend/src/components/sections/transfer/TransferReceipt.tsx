import { Alert, Box, Button, Divider, Stack, Typography } from '@mui/material';
import type { TransferResult } from 'api/transfers';
import IconifyIcon from 'components/base/IconifyIcon';
import { formatDateTime } from 'lib/dates';
import { formatMoney } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { ReactNode } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import paths from 'routes/path';
import { ValidTransfer } from './transferValidation';

interface TransferReceiptProps {
  result: TransferResult;
  transfer: ValidTransfer;
  onNewTransfer: () => void;
}

const Row = ({ label, children }: { label: string; children: ReactNode }) => (
  <Stack direction="row" justifyContent="space-between" gap={2} sx={{ py: 1.25 }}>
    <Typography sx={{ color: 'text.secondary' }}>{label}</Typography>
    <Typography sx={{ textAlign: 'right', fontWeight: 500, wordBreak: 'break-all' }}>
      {children}
    </Typography>
  </Stack>
);

const TransferReceipt = ({ result, transfer, onNewTransfer }: TransferReceiptProps) => {
  const duplicate = result.message.toLowerCase().startsWith('duplicate');
  return (
    <Stack gap={3}>
      <Stack alignItems="center" gap={1} sx={{ color: 'success.main', textAlign: 'center' }}>
        <IconifyIcon icon="mdi:check-circle" width={56} />
        <Typography variant="h4" sx={{ color: 'text.primary' }}>
          {formatMoney(result.amount)} sent
        </Typography>
        <Typography sx={{ color: 'text.secondary' }}>
          to {formatAccountNumber(transfer.toAccountNumber)}
        </Typography>
      </Stack>
      {duplicate && (
        <Alert severity="info">
          This transfer had already been sent, so no money moved again. These are the original
          details.
        </Alert>
      )}
      <Box>
        <Row label="Reference">{result.transactionReference}</Row>
        <Divider />
        <Row label="From">{formatAccountNumber(transfer.fromAccountNumber)}</Row>
        <Divider />
        <Row label="Description">{transfer.description}</Row>
        <Divider />
        <Row label="Date">{formatDateTime(result.createdAt)}</Row>
      </Box>
      <Stack direction="row" gap={2} flexWrap="wrap">
        <Button variant="contained" onClick={onNewTransfer}>
          Make another transfer
        </Button>
        <Button
          variant="outlined"
          component={RouterLink}
          to={`${paths.transactions}?account=${transfer.fromAccountNumber}`}
        >
          View transactions
        </Button>
      </Stack>
    </Stack>
  );
};

export default TransferReceipt;
