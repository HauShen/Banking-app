import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  InputAdornment,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { NetworkError } from 'api/client';
import { useQuote, useRates } from 'api/fx';
import { RemittanceResult, useReceiveRemittance } from 'api/remittances';
import type { Account } from 'api/types';
import { formatDateTime } from 'lib/dates';
import { errorMessage } from 'lib/errors';
import { newIdempotencyKey } from 'lib/ids';
import { formatMoney, formatRate, normalizeAmountInput } from 'lib/money';
import { formatAccountNumber } from 'lib/transactions';
import { useDebouncedValue } from 'lib/useDebouncedValue';
import { FormEvent, ReactNode, useRef, useState } from 'react';
import {
  RemittanceErrors,
  RemittanceField,
  remittanceProblem,
  RemittanceValues,
  SENDER_NAME_MAX,
  validateRemittance,
} from './remittanceValidation';

interface RemittanceDialogProps {
  account: Account;
  open: boolean;
  onClose: () => void;
  onReceived: (result: RemittanceResult) => void;
}

const Row = ({ label, children }: { label: string; children: ReactNode }) => (
  <Stack direction="row" justifyContent="space-between" gap={2} sx={{ py: 1 }}>
    <Typography sx={{ color: 'text.secondary' }}>{label}</Typography>
    <Typography sx={{ fontWeight: 500, textAlign: 'right' }}>{children}</Typography>
  </Stack>
);

/** "≈ RM 416.50 will be credited · 1 USD = RM 4.1650 (buy rate)" while the admin types. */
const IndicativeCredit = ({ currency, amount }: { currency: string; amount: string }) => {
  const normalized = useDebouncedValue(normalizeAmountInput(amount, currency));
  const quote = useQuote(
    normalized ? { currency, amount: normalized, direction: 'RECEIVE' } : undefined,
  );
  if (!normalized) return null;
  let text: string;
  if (quote.isLoading) text = 'Getting the rate…';
  else if (quote.error) text = errorMessage(quote.error);
  else if (quote.data) {
    text = `≈ ${formatMoney(quote.data.myrAmount)} will be credited · 1 ${currency} = RM ${formatRate(
      quote.data.exchangeRate,
    )} (buy rate, applied when received)`;
  } else return null;
  return (
    <Typography
      variant="body2"
      sx={{ color: quote.error ? 'error.main' : 'text.secondary', mt: -1 }}
    >
      {text}
    </Typography>
  );
};

/**
 * Admin: record money arriving from abroad into this account (simulates the bank's payment gateway).
 * POST /api/admin/remittances, one idempotency key per attempt (decision 4).
 */
const RemittanceDialog = ({ account, open, onClose, onReceived }: RemittanceDialogProps) => {
  const rates = useRates();
  const currencies = (rates.data ?? []).map((rate) => rate.currency);
  const [values, setValues] = useState<RemittanceValues>({
    currency: 'USD',
    amount: '',
    senderName: '',
  });
  const [errors, setErrors] = useState<RemittanceErrors>({});
  const [problem, setProblem] = useState<string | null>(null);
  const [result, setResult] = useState<RemittanceResult>();
  const receive = useReceiveRemittance();
  // Kept when the same remittance is sent again after a network error; dropped when anything changes.
  const idempotencyKey = useRef<string>();

  const currency = currencies.includes(values.currency)
    ? values.currency
    : currencies[0] ?? values.currency;
  const current = { ...values, currency };

  const close = () => {
    if (receive.isPending) return;
    idempotencyKey.current = undefined;
    setValues({ currency, amount: '', senderName: '' });
    setErrors({});
    setProblem(null);
    setResult(undefined);
    onClose();
  };

  const change = (field: RemittanceField, value: string) => {
    idempotencyKey.current = undefined;
    setValues({ ...current, [field]: value });
    setErrors((existing) => ({ ...existing, [field]: undefined }));
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setProblem(null);
    const checked = validateRemittance(current);
    setErrors(checked.errors);
    if (!checked.amount || !checked.senderName) return;

    idempotencyKey.current ??= newIdempotencyKey();
    receive.mutate(
      {
        toAccountNumber: account.accountNumber,
        currency,
        amount: checked.amount,
        senderName: checked.senderName,
        idempotencyKey: idempotencyKey.current,
      },
      {
        onSuccess: (data) => {
          idempotencyKey.current = undefined;
          setResult(data);
          onReceived(data);
        },
        onError: (error) => {
          if (error instanceof NetworkError) {
            setProblem(
              `${errorMessage(error)} You can send it again safely: it won't be credited twice.`,
            );
            return;
          }
          const found = remittanceProblem(error);
          if (found.newKeyNeeded) idempotencyKey.current = undefined;
          if (found.field) setErrors({ [found.field]: found.message });
          else setProblem(found.message);
        },
      },
    );
  };

  return (
    <Dialog open={open} onClose={close} fullWidth maxWidth="xs">
      {result ? (
        <>
          <DialogTitle>Remittance received</DialogTitle>
          <DialogContent>
            <Typography variant="h4" sx={{ textAlign: 'center', color: 'success.main', my: 2 }}>
              +{formatMoney(result.amount)}
            </Typography>
            {result.message.toLowerCase().startsWith('duplicate') && (
              <Alert severity="info" sx={{ mb: 2 }}>
                This remittance had already been recorded, so nothing was credited again.
              </Alert>
            )}
            <Box>
              <Row label="Credited to">{formatAccountNumber(result.toAccountNumber)}</Row>
              <Divider />
              <Row label="Sent">{formatMoney(result.originalAmount, result.originalCurrency)}</Row>
              <Divider />
              <Row label="Exchange rate">
                1 {result.originalCurrency} = RM {formatRate(result.exchangeRate)}
              </Row>
              <Divider />
              <Row label="Reference">{result.transactionReference}</Row>
              <Divider />
              <Row label="Date">{formatDateTime(result.createdAt)}</Row>
            </Box>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2.5 }}>
            <Button variant="contained" onClick={close}>
              Done
            </Button>
          </DialogActions>
        </>
      ) : (
        <form noValidate onSubmit={handleSubmit}>
          <DialogTitle>Receive remittance</DialogTitle>
          <DialogContent>
            <Typography sx={{ color: 'text.secondary', mb: 2.5 }}>
              Money from abroad into {formatAccountNumber(account.accountNumber)}, converted to MYR
              at the bank's buy rate. Simulates the payment gateway (demo only).
            </Typography>
            <Stack gap={2.5}>
              <Stack direction="row" gap={2} alignItems="flex-start">
                <TextField
                  select
                  label="Currency"
                  value={currency}
                  onChange={(e) => change('currency', e.target.value)}
                  error={Boolean(errors.currency)}
                  helperText={errors.currency}
                  disabled={receive.isPending || currencies.length === 0}
                  sx={{ width: 130, flexShrink: 0 }}
                >
                  {currencies.map((code) => (
                    <MenuItem key={code} value={code}>
                      {code}
                    </MenuItem>
                  ))}
                </TextField>
                <TextField
                  label="Amount"
                  value={values.amount}
                  onChange={(e) => change('amount', e.target.value)}
                  error={Boolean(errors.amount)}
                  helperText={errors.amount}
                  InputProps={{
                    startAdornment: <InputAdornment position="start">{currency}</InputAdornment>,
                  }}
                  inputProps={{ inputMode: 'decimal', autoComplete: 'off' }}
                  disabled={receive.isPending}
                  fullWidth
                  autoFocus
                />
              </Stack>
              {!errors.amount && currencies.includes(currency) && (
                <IndicativeCredit currency={currency} amount={values.amount} />
              )}
              <TextField
                label="Sender name"
                value={values.senderName}
                onChange={(e) => change('senderName', e.target.value)}
                error={Boolean(errors.senderName)}
                helperText={
                  errors.senderName ?? 'Shown in the history as "Foreign remittance from …"'
                }
                inputProps={{ maxLength: SENDER_NAME_MAX }}
                disabled={receive.isPending}
              />
              {rates.error && <Alert severity="error">{errorMessage(rates.error)}</Alert>}
              {problem && <Alert severity="error">{problem}</Alert>}
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2.5 }}>
            <Button onClick={close} disabled={receive.isPending}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={receive.isPending || currencies.length === 0}
            >
              {receive.isPending ? 'Receiving…' : 'Receive'}
            </Button>
          </DialogActions>
        </form>
      )}
    </Dialog>
  );
};

export default RemittanceDialog;
