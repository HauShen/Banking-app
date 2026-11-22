import { Alert, Box, Button, Stack, TextField } from '@mui/material';
import { changePassword } from 'api/auth';
import { errorMessage } from 'lib/errors';
import { FormEvent, useState } from 'react';
import {
  EMPTY_PASSWORDS,
  passwordServerErrors,
  PasswordErrors,
  PasswordValues,
  validatePasswordChange,
} from './passwordValidation';

/** The signed-in user changes their password; every other device is signed out. */
const ChangePasswordForm = () => {
  const [values, setValues] = useState<PasswordValues>(EMPTY_PASSWORDS);
  const [errors, setErrors] = useState<PasswordErrors>({});
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<{ severity: 'success' | 'error'; text: string }>();

  const change = (field: keyof PasswordValues, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setStatus(undefined);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setStatus(undefined);
    const clientErrors = validatePasswordChange(values);
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }
    setSaving(true);
    try {
      await changePassword(values.currentPassword, values.newPassword);
      setValues(EMPTY_PASSWORDS);
      setStatus({
        severity: 'success',
        text: 'Your password has been changed. You have been signed out on your other devices.',
      });
    } catch (err) {
      const fieldErrors = passwordServerErrors(err);
      if (fieldErrors) setErrors(fieldErrors);
      else setStatus({ severity: 'error', text: errorMessage(err) });
    } finally {
      setSaving(false);
    }
  };

  const field = (
    name: keyof PasswordValues,
    label: string,
    autoComplete: string,
    hint?: string,
  ) => (
    <TextField
      label={label}
      type="password"
      value={values[name]}
      onChange={(e) => change(name, e.target.value)}
      error={Boolean(errors[name])}
      helperText={errors[name] ?? hint}
      autoComplete={autoComplete}
      disabled={saving}
      required
    />
  );

  return (
    <Box component="form" noValidate onSubmit={handleSubmit}>
      <Stack gap={3}>
        {field('currentPassword', 'Current password', 'current-password')}
        {field('newPassword', 'New password', 'new-password', 'At least 8 characters')}
        {field('confirmPassword', 'Confirm new password', 'new-password')}
        {status && <Alert severity={status.severity}>{status.text}</Alert>}
        <Box>
          <Button type="submit" variant="contained" disabled={saving}>
            {saving ? 'Changing…' : 'Change password'}
          </Button>
        </Box>
      </Stack>
    </Box>
  );
};

export default ChangePasswordForm;
