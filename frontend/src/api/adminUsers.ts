import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from './client';
import { queryKeys } from './queryKeys';
import type { PageResult, Role, UserProfile } from './types';

/** UserSortField.java */
export type UserSortField = 'USERNAME' | 'FULL_NAME' | 'EMAIL' | 'ROLE' | 'CREATED_AT';
export type SortDirection = 'ASC' | 'DESC';

export interface UserListParams {
  page: number;
  /** The backend caps it at 100. */
  size: number;
  sortBy: UserSortField;
  direction: SortDirection;
}

/** GET /api/admin/users (ADMIN only). */
export const listUsers = ({ page, size, sortBy, direction }: UserListParams) =>
  apiFetch<PageResult<UserProfile>>(
    `/api/admin/users?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`,
  );

export const useUsers = (params: UserListParams) =>
  useQuery({
    queryKey: queryKeys.admin.users.list(params),
    queryFn: () => listUsers(params),
    placeholderData: keepPreviousData,
  });

/** GET /api/admin/users/{id}. */
export const getUser = (id: string) =>
  apiFetch<UserProfile>(`/api/admin/users/${encodeURIComponent(id)}`);

export const useUser = (id: string | undefined) =>
  useQuery({
    queryKey: queryKeys.admin.user(id ?? ''),
    queryFn: () => getUser(id as string),
    enabled: Boolean(id),
  });

/** UserProfileRequestBody.java: the body for a new admin or a new customer. */
export interface NewUser {
  username: string;
  fullName: string;
  email: string;
  password: string;
}

/** POST /api/admin/users → 201: a new ADMIN account. */
export const createAdmin = (admin: NewUser) =>
  apiFetch<UserProfile>('/api/admin/users', { method: 'POST', body: admin });

/**
 * POST /api/admin/users/customers → 201: a new CUSTOMER. The only way customers are created (no sign-up);
 * the admin hands over the first password, and the customer changes it on the Profile page.
 */
export const createCustomer = (customer: NewUser) =>
  apiFetch<UserProfile>('/api/admin/users/customers', { method: 'POST', body: customer });

/** PATCH /api/admin/users/{id}/role. 409 for your own role or the last admin. */
export const changeRole = ({ id, role }: { id: string; role: Role }) =>
  apiFetch<UserProfile>(`/api/admin/users/${encodeURIComponent(id)}/role`, {
    method: 'PATCH',
    body: { role },
  });

/** DELETE /api/admin/users/{id} → 204. 409 for yourself or the last admin. */
export const deleteUser = (id: string) =>
  apiFetch<void>(`/api/admin/users/${encodeURIComponent(id)}`, { method: 'DELETE' });

/**
 * PUT /api/admin/users/{id}: another user's full name and email. The backend audits it as an edit by
 * another user. (For your own entry use PUT /api/users/me, like the profile page.)
 */
export const updateUser = ({
  id,
  fullName,
  email,
}: {
  id: string;
  fullName: string;
  email: string;
}) =>
  apiFetch<UserProfile>(`/api/admin/users/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body: { fullName, email },
  });

/** Mutations that change the user list re-fetch every page of it afterwards. */
const useUserMutation = <TArgs, TResult>(mutationFn: (args: TArgs) => Promise<TResult>) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: queryKeys.admin.users.all }),
  });
};

export const useCreateAdmin = () => useUserMutation(createAdmin);
export const useCreateCustomer = () => useUserMutation(createCustomer);
export const useChangeRole = () => useUserMutation(changeRole);
export const useDeleteUser = () => useUserMutation(deleteUser);
export const useUpdateUser = () => useUserMutation(updateUser);
