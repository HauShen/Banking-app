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

/** AccountType.java, AccountStatus.java */
export type AccountType = 'SAVINGS' | 'CURRENT';
export type AccountStatus = 'ACTIVE' | 'LOCKED';

/** AccountResponseBody.java. `balance` is a BigDecimal: display it, never calculate with it. */
export interface Account {
  accountId: number;
  userId: string;
  accountNumber: string;
  accountType: AccountType;
  accountStatus: AccountStatus;
  accountCurrency: string;
  balance: number;
  createdAt: string;
}

/** Body of /api/accounts/summary (AccountSummaryResponseBody.java): total of all the user's accounts. */
export interface AccountSummary {
  balance: number;
  accountType: string;
  accounts: Account[];
}

/** TransactionType.java, TransactionStatus.java */
export type TransactionType = 'TRANSFER' | 'CASH_DEPOSIT' | 'FOREIGN_REMITTANCE';
export type TransactionStatus = 'PENDING' | 'SUCCESS' | 'FAILED';

/**
 * TransactionResponseBody.java. `amount` is the MYR amount that moved. `fromAccountNumber` is
 * "Cash Deposit Machine" / "Foreign Remittance" when there is no sender account.
 * The three FX fields are set together for foreign-currency transactions, otherwise null.
 */
export interface Transaction {
  reference: string;
  type: TransactionType;
  fromAccountNumber: string;
  toAccountNumber: string;
  amount: number;
  currency: string;
  originalCurrency: string | null;
  originalAmount: number | null;
  exchangeRate: number | null;
  status: TransactionStatus;
  description: string | null;
  createdAt: string;
  postedAt: string | null;
}

/** The fields we use of Spring Data's serialized Page (PageImpl). */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** PageResult.java (the admin user list; not Spring's Page). */
export interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
}

/** NotificationType.java: what a notification is about. */
export type NotificationType =
  | 'MONEY_SENT'
  | 'MONEY_RECEIVED'
  | 'CASH_DEPOSITED'
  | 'REMITTANCE_RECEIVED';

/** NotificationResponseBody.java (GET /api/notifications). */
export interface AppNotification {
  id: number;
  type: NotificationType;
  /** Ready to show, e.g. "You received RM 30.00 from account ****0001." */
  message: string;
  reference: string;
  /** MYR. */
  amount: number;
  /** When the money moved. */
  createdAt: string;
  read: boolean;
}

/** AlertRule.java: which watch rule raised an alert. */
export type AlertRule = 'LARGE_TRANSFER' | 'RAPID_TRANSFERS';

/** AlertStatus.java */
export type AlertStatus = 'OPEN' | 'REVIEWED';

/** AlertResponseBody.java (GET /api/admin/alerts). */
export interface TransactionAlert {
  id: number;
  rule: AlertRule;
  status: AlertStatus;
  /** The sending account. */
  accountNumber: string;
  ownerId: string;
  /** The transfer's reference number. */
  reference: string;
  /** MYR, of the transfer that raised the alert. */
  amount: number;
  /** Why, in one sentence, e.g. "6 transfers from this account within 10 minutes (limit 5)". */
  detail: string;
  /** When the transfer happened. */
  createdAt: string;
  reviewedAt: string | null;
  /** The reviewing admin's user id. */
  reviewedBy: string | null;
}
