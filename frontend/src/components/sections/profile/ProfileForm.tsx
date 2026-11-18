import { Alert, Box, Button, Stack, TextField } from '@mui/material';
import type { UserProfile } from 'api/types';
import { errorMessage } from 'lib/errors';
import { useAuth } from 'providers/AuthProvider';
import { FormEvent, useState } from 'react';
import {
  ProfileErrors,
  profileServerErrors,
  ProfileValues,
  validateProfile,
} from './profileValidation';

/** Edit the signed-in user's full name and email. */
const ProfileForm = ({ user }: { user: UserProfile }) => {
  const { updateProfile } = useAuth();
  const [values, setValues] = useState<ProfileValues>({
    fullName: user.fullName,
    email: user.email,
  });
  const [errors, setErrors] = useState<ProfileErrors>({});
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState<{ severity: 'success' | 'error'; text: string }>();

  const trimmed = { fullName: values.fullName.trim(), email: values.email.trim() };
  const unchanged = trimmed.fullName === user.fullName && trimmed.email === user.email;

  const change = (field: keyof ProfileValues, value: string) => {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setStatus(undefined);
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setStatus(undefined);
    const clientErrors = validateProfile(values);
    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      return;
    }
    setSaving(true);
    try {
      const updated = await updateProfile(trimmed);
      // The backend stores the email in lower case; show what was saved.
      setValues({ fullName: updated.fullName, email: updated.email });
      setStatus({ severity: 'success', text: 'Your profile has been saved.' });
    } catch (err) {
      const fieldErrors = profileServerErrors(err);
      if (fieldErrors) setErrors(fieldErrors);
      else setStatus({ severity: 'error', text: errorMessage(err) });
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box component="form" noValidate onSubmit={handleSubmit}>
      <Stack gap={3}>
        <TextField
          label="Full name"
          value={values.fullName}
          onChange={(e) => change('fullName', e.target.value)}
          error={Boolean(errors.fullName)}
          helperText={errors.fullName ?? 'Shown on your account cards'}
          autoComplete="name"
          disabled={saving}
          required
        />
        <TextField
          label="Email address"
          type="email"
          value={values.email}
          onChange={(e) => change('email', e.target.value)}
          error={Boolean(errors.email)}
          helperText={errors.email}
          autoComplete="email"
          disabled={saving}
          required
        />
        {status && <Alert severity={status.severity}>{status.text}</Alert>}
        <Box>
          <Button type="submit" variant="contained" disabled={saving || unchanged}>
            {saving ? 'Saving…' : 'Save changes'}
          </Button>
        </Box>
      </Stack>
    </Box>
  );
};

export default ProfileForm;
