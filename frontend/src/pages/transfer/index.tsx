import { Alert, Button, Card, Step, StepLabel, Stepper, Typography } from '@mui/material';
import { useAccountSummary } from 'api/accounts';
import { NetworkError } from 'api/client';
import { TransferResult, useCreateTransfer } from 'api/transfers';
import TransferForm from 'components/sections/transfer/TransferForm';
import {
  TransferErrors,
  TransferField,
  TransferValues,
  ValidTransfer,
  transferProblem,
  validateTransfer,
} from 'components/sections/transfer/transferValidation';
import TransferReceipt from 'components/sections/transfer/TransferReceipt';
import TransferReview from 'components/sections/transfer/TransferReview';
import { errorMessage } from 'lib/errors';
import { newIdempotencyKey } from 'lib/ids';
import { useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';

/** What the dashboard's quick-transfer card passes in router state. */
export type TransferPrefill = Partial<
  Pick<TransferValues, 'fromAccountNumber' | 'toAccountNumber' | 'amount'>
>;

type StepName = 'details' | 'review' | 'done';
const STEPS: StepName[] = ['details', 'review', 'done'];
const STEP_LABEL: Record<StepName, string> = { details: 'Details', review: 'Review', done: 'Done' };

const TransferPage = () => {
  const summary = useAccountSummary();
  const prefill = (useLocation().state as TransferPrefill | null) ?? {};
  const activeAccounts = (summary.data?.accounts ?? []).filter((a) => a.accountStatus === 'ACTIVE');

  const [values, setValues] = useState<TransferValues>({
    fromAccountNumber: prefill.fromAccountNumber ?? '',
    toAccountNumber: prefill.toAccountNumber ?? '',
    amount: prefill.amount ?? '',
    description: '',
  });
  const [errors, setErrors] = useState<TransferErrors>({});
  const [step, setStep] = useState<StepName>('details');
  const [transfer, setTransfer] = useState<ValidTransfer>();
  const [problem, setProblem] = useState<string | null>(null);
  const [result, setResult] = useState<TransferResult>();
  const createTransfer = useCreateTransfer();

  // One key per transfer attempt (decision 4): kept when the same transfer is confirmed again after
  // a network error, so it can never be booked twice; dropped as soon as the details change.
  const idempotencyKey = useRef<string>();

  // The pre-filled or chosen account if it's active, otherwise the first active one.
  const fromAccountNumber = activeAccounts.some((a) => a.accountNumber === values.fromAccountNumber)
    ? values.fromAccountNumber
    : activeAccounts[0]?.accountNumber ?? '';
  const currentValues = { ...values, fromAccountNumber };

  const change = (field: TransferField, value: string) => {
    idempotencyKey.current = undefined;
    setValues({ ...currentValues, [field]: value });
    setErrors((current) => ({ ...current, [field]: undefined }));
  };

  const review = () => {
    const checked = validateTransfer(currentValues, activeAccounts);
    setErrors(checked.errors);
    if (!checked.transfer) return;
    setTransfer(checked.transfer);
    setProblem(null);
    setStep('review');
  };

  const confirm = () => {
    if (!transfer) return;
    idempotencyKey.current ??= newIdempotencyKey();
    setProblem(null);
    createTransfer.mutate(
      { ...transfer, idempotencyKey: idempotencyKey.current },
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
          if (found.field) {
            setErrors({ [found.field]: found.message });
            setStep('details');
          } else {
            setProblem(found.message);
          }
        },
      },
    );
  };

  const startOver = () => {
    idempotencyKey.current = undefined;
    setValues({ fromAccountNumber, toAccountNumber: '', amount: '', description: '' });
    setErrors({});
    setTransfer(undefined);
    setResult(undefined);
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
        onBack={() => setStep('details')}
        onConfirm={confirm}
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
