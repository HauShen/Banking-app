import { Alert, Button, Card, Step, StepLabel, Stepper, Typography } from '@mui/material';
import { useAccountSummary } from 'api/accounts';
import { NetworkError } from 'api/client';
import { LockedQuote, useLockQuote, useRates } from 'api/fx';
import { TransferResult, useCreateTransfer } from 'api/transfers';
import TransferForm from 'components/sections/transfer/TransferForm';
import TransferReceipt from 'components/sections/transfer/TransferReceipt';
import TransferReview from 'components/sections/transfer/TransferReview';
import {
  isForeign,
  TransferErrors,
  TransferField,
  transferProblem,
  TransferValues,
  validateTransfer,
  ValidTransfer,
} from 'components/sections/transfer/transferValidation';
import { errorMessage } from 'lib/errors';
import { newIdempotencyKey } from 'lib/ids';
import { DEFAULT_CURRENCY } from 'lib/money';
import { quoteDeadline, useCountdown } from 'lib/useCountdown';
import { useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';

/** What the dashboard's quick-transfer card passes in router state. */
export type TransferPrefill = Partial<
  Pick<TransferValues, 'fromAccountNumber' | 'toAccountNumber' | 'amount'>
>;

type StepName = 'details' | 'review' | 'done';
const STEPS: StepName[] = ['details', 'review', 'done'];
const STEP_LABEL: Record<StepName, string> = { details: 'Details', review: 'Review', done: 'Done' };

interface HeldQuote {
  quote: LockedQuote;
  /** When to treat it as expired, in this browser's clock. */
  deadline: number;
}

const TransferPage = () => {
  const summary = useAccountSummary();
  const rates = useRates();
  const prefill = (useLocation().state as TransferPrefill | null) ?? {};
  const activeAccounts = (summary.data?.accounts ?? []).filter((a) => a.accountStatus === 'ACTIVE');
  // MYR always works; foreign currencies only when the rates could be loaded.
  const currencies = [DEFAULT_CURRENCY, ...(rates.data ?? []).map((rate) => rate.currency)];

  const [values, setValues] = useState<TransferValues>({
    fromAccountNumber: prefill.fromAccountNumber ?? '',
    toAccountNumber: prefill.toAccountNumber ?? '',
    amount: prefill.amount ?? '',
    currency: DEFAULT_CURRENCY,
    description: '',
  });
  const [errors, setErrors] = useState<TransferErrors>({});
  const [step, setStep] = useState<StepName>('details');
  const [transfer, setTransfer] = useState<ValidTransfer>();
  const [problem, setProblem] = useState<string | null>(null);
  const [result, setResult] = useState<TransferResult>();
  const [held, setHeld] = useState<HeldQuote>();
  const createTransfer = useCreateTransfer();
  const lockQuote = useLockQuote();
  const secondsLeft = useCountdown(held?.deadline);

  // One key per transfer attempt (decision 4): kept when the same transfer is confirmed again after
  // a network error, so it can never be booked twice; dropped as soon as the details change.
  // Getting a new rate keeps it: the backend matches a retry by currency and amount, not by quote.
  const idempotencyKey = useRef<string>();

  // The pre-filled or chosen account if it's active, otherwise the first active one.
  const fromAccountNumber = activeAccounts.some((a) => a.accountNumber === values.fromAccountNumber)
    ? values.fromAccountNumber
    : activeAccounts[0]?.accountNumber ?? '';
  const currentValues = { ...values, fromAccountNumber };

  const change = (field: TransferField, value: string) => {
    idempotencyKey.current = undefined;
    setHeld(undefined);
    setValues({ ...currentValues, [field]: value });
    setErrors((current) => ({ ...current, [field]: undefined }));
  };

  const backToDetails = (fieldErrors: TransferErrors = {}) => {
    setErrors(fieldErrors);
    setHeld(undefined);
    setStep('details');
  };

  /** Locks the sell rate for this foreign transfer (POST /api/fx/quotes). */
  const lockRate = (target: ValidTransfer) => {
    setProblem(null);
    setHeld(undefined);
    lockQuote.mutate(
      { currency: target.currency, amount: target.amount },
      {
        onSuccess: (quote) => setHeld({ quote, deadline: quoteDeadline(quote.expiresAt) }),
        onError: (error) => {
          const found = transferProblem(error);
          if (found.field) backToDetails({ [found.field]: found.message });
          else setProblem(found.message);
        },
      },
    );
  };

  const review = () => {
    const checked = validateTransfer(currentValues, activeAccounts);
    setErrors(checked.errors);
    if (!checked.transfer) return;
    setTransfer(checked.transfer);
    setProblem(null);
    setStep('review');
    if (isForeign(checked.transfer)) lockRate(checked.transfer);
  };

  const confirm = () => {
    if (!transfer) return;
    const foreign = isForeign(transfer);
    if (foreign && (!held || secondsLeft === 0)) return;
    idempotencyKey.current ??= newIdempotencyKey();
    setProblem(null);
    createTransfer.mutate(
      {
        ...transfer,
        // MYR transfers send no currency (the backend's default); foreign ones send it with the locked quote.
        currency: foreign ? transfer.currency : undefined,
        quoteId: foreign ? held?.quote.quoteId : undefined,
        idempotencyKey: idempotencyKey.current,
      },
      {
        onSuccess: (data) => {
          idempotencyKey.current = undefined;
          setResult(data);
          setStep('done');
        },
        onError: (error) => {
          if (error instanceof NetworkError) {
            // The transfer may or may not have arrived: keep the key, so confirming again is safe.
            setProblem(
              `${errorMessage(error)} You can confirm again safely: it won't be sent twice.`,
            );
            return;
          }
          const found = transferProblem(error);
          if (found.newKeyNeeded) idempotencyKey.current = undefined;
          if (found.quoteExpired) {
            setHeld((current) => current && { ...current, deadline: Date.now() });
          }
          if (found.field) backToDetails({ [found.field]: found.message });
          else setProblem(found.message);
        },
      },
    );
  };

  const startOver = () => {
    idempotencyKey.current = undefined;
    setValues({
      fromAccountNumber,
      toAccountNumber: '',
      amount: '',
      currency: values.currency,
      description: '',
    });
    setErrors({});
    setTransfer(undefined);
    setResult(undefined);
    setHeld(undefined);
    setProblem(null);
    setStep('details');
  };

  let body;
  if (summary.error) {
    body = <Alert severity="error">{errorMessage(summary.error)}</Alert>;
  } else if (summary.isLoading) {
    body = <Typography sx={{ color: 'text.secondary' }}>Loading your accounts…</Typography>;
  } else if (activeAccounts.length === 0) {
    body = (
      <Alert severity="info">
        You need an active account to send money. Accounts are opened (and unlocked) by an
        administrator.
      </Alert>
    );
  } else if (step === 'details') {
    body = (
      <TransferForm
        accounts={activeAccounts}
        currencies={currencies}
        values={currentValues}
        errors={errors}
        onChange={change}
        onSubmit={review}
      />
    );
  } else if (step === 'review' && transfer) {
    body = (
      <TransferReview
        transfer={transfer}
        from={
          activeAccounts.find((a) => a.accountNumber === transfer.fromAccountNumber) ??
          activeAccounts[0]
        }
        sending={createTransfer.isPending}
        problem={problem}
        onBack={() => backToDetails()}
        onConfirm={confirm}
        quote={{
          locked: held?.quote,
          locking: lockQuote.isPending,
          secondsLeft,
          onRelock: () => lockRate(transfer),
        }}
      />
    );
  } else if (result && transfer) {
    body = <TransferReceipt result={result} transfer={transfer} onNewTransfer={startOver} />;
  }

  return (
    <Card sx={{ p: { xs: 3, md: 5 }, maxWidth: 640, mb: 3 }}>
      <Typography variant="h4" sx={{ mb: 3 }}>
        Send money
      </Typography>
      <Stepper activeStep={STEPS.indexOf(step)} sx={{ mb: 4 }}>
        {STEPS.map((name) => (
          <Step key={name} completed={STEPS.indexOf(step) > STEPS.indexOf(name) || step === 'done'}>
            <StepLabel>{STEP_LABEL[name]}</StepLabel>
          </Step>
        ))}
      </Stepper>
      {body}
      {summary.error && (
        <Button sx={{ mt: 2 }} onClick={() => void summary.refetch()}>
          Try again
        </Button>
      )}
    </Card>
  );
};

export default TransferPage;
