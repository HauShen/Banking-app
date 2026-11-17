/** Body of /api/auth/register, /login and /refresh (AuthenticationResponse.java). */
export interface AuthenticationResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

/** UserRole.java */
export type Role = 'CUSTOMER' | 'ADMIN';

/** Body of /api/users/me (UserProfileResponseBody.java). */
export interface UserProfile {
  id: string;
  username: string;
  fullName: string;
  email: string;
  role: Role;
  createdAt: string;
  updatedAt: string;
}

/** Error body from GlobalExceptionHandler / JsonSecurityErrorHandler (ApiError.java). */
export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string> | null;
}
