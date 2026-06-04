// Story: US-001 | US-002 | US-003 | US-004 | US-005 | US-006 | US-007 | US-008 | US-010 | US-011 | US-012 | US-013
import { createBrowserRouter, Navigate } from 'react-router-dom';
import RegistrationPage from '@/features/registration/pages/RegistrationPage';
import RegistrationSuccess from '@/features/registration/components/RegistrationSuccess';
import LoginPage from '@/features/auth/pages/LoginPage';
import OtpVerificationPage from '@/features/auth/pages/OtpVerificationPage';
import ForgotPasswordPage from '@/features/auth/pages/ForgotPasswordPage';
import ForgotPasswordSentPage from '@/features/auth/pages/ForgotPasswordSentPage';
import ResetPasswordPage from '@/features/auth/pages/ResetPasswordPage';
import ResetPasswordSuccessPage from '@/features/auth/pages/ResetPasswordSuccessPage';
import ResetTokenErrorPage from '@/features/auth/pages/ResetTokenErrorPage';
// US-005: Profile & Dashboard
import ProfilePage from '@/features/profile/pages/ProfilePage';
import DashboardPage from '@/features/dashboard/pages/DashboardPage';
// US-006 | US-007 | US-008: Account pages
import { AccountDetailPage } from '@/features/accounts/AccountDetailPage';
// US-010: Internal fund transfer
import TransferPage from '@/features/transactions/pages/TransferPage';
import TransactionHistoryPage from '@/features/transactions/pages/TransactionHistoryPage';
import TransactionDetailPage from '@/features/transactions/pages/TransactionDetailPage';
import AdminTransactionsPage from '@/features/transactions/pages/AdminTransactionsPage';
import AdminTransactionDetailPage from '@/features/transactions/pages/AdminTransactionDetailPage';
import FraudRulesPage from '@/features/fraud/pages/FraudRulesPage';
import FraudAlertsPage from '@/features/fraud/pages/FraudAlertsPage';
import FraudAlertDetailPage from '@/features/fraud/pages/FraudAlertDetailPage';
import NotificationsPage from '@/features/notifications/pages/NotificationsPage';

export const router = createBrowserRouter([
  {
    // Unauthenticated root → send users to login first (US-002).
    // Once auth guards are in place (future story), this will check session state.
    path: '/',
    element: <Navigate to="/login" replace />,
  },
  {
    // US-002: Customer Login
    path: '/login',
    element: <LoginPage />,
  },
  {
    // US-001: Customer Registration
    path: '/register',
    element: <RegistrationPage />,
  },
  {
    path: '/register/success',
    element: <RegistrationSuccess />,
  },
  {
    // US-003: OTP Verification (Two-Factor Authentication via SMS)
    path: '/verify-otp',
    element: <OtpVerificationPage />,
  },
  {
    // US-005: Authenticated dashboard landing page
    // useVerifyOtp (US-003) navigates here on successful OTP verification.
    path: '/dashboard',
    element: <DashboardPage />,
  },
  // ── US-005: Customer Profile ────────────────────────────────────────────────
  {
    // View and update the authenticated customer's profile (US-005)
    path: '/profile',
    element: <ProfilePage />,
  },
  // ── US-004: Password Reset ──────────────────────────────────────────────────
  {
    // Screen 1: Email submission form
    path: '/forgot-password',
    element: <ForgotPasswordPage />,
  },
  {
    // Screen 2: Anti-enumeration confirmation + resend
    path: '/forgot-password/sent',
    element: <ForgotPasswordSentPage />,
  },
  {
    // Screen 3: New password form (reads ?token= from query string)
    // Screen 5 is rendered inline when no token is present.
    path: '/reset-password',
    element: <ResetPasswordPage />,
  },
  {
    // Screen 4: Success confirmation (AC4/AC5)
    path: '/reset-password/success',
    element: <ResetPasswordSuccessPage />,
  },
  {
    // Screen 5: Expired/invalid token error (AC6/AC7)
    path: '/reset-password/error',
    element: <ResetTokenErrorPage />,
  },
  // ── US-008: Account detail page ─────────────────────────────────────────────
  {
    // Navigated to from AccountList (US-007) when a customer taps an account card.
    // accountId is the UUID of the BankAccount entity.
    path: '/accounts/:accountId',
    element: <AccountDetailPage />,
  },
  {
    // US-010: Transfer funds between own active accounts.
    path: '/transfer',
    element: <TransferPage />,
  },
  {
    // US-011: Customer transaction history.
    path: '/transactions',
    element: <TransactionHistoryPage />,
  },
  {
    // US-012: Customer transaction details.
    path: '/transactions/:transactionId',
    element: <TransactionDetailPage />,
  },
  {
    // US-013: Admin transaction overview.
    path: '/admin/transactions',
    element: <AdminTransactionsPage />,
  },
  {
    // US-013: Admin transaction detail.
    path: '/admin/transactions/:transactionId',
    element: <AdminTransactionDetailPage />,
  },
  {
    // US-014: Fraud rule management.
    path: '/fraud/rules',
    element: <FraudRulesPage />,
  },
  {
    // US-017: Fraud alert review queue.
    path: '/fraud/alerts',
    element: <FraudAlertsPage />,
  },
  {
    // US-017 / US-018: Fraud alert detail and resolution.
    path: '/fraud/alerts/:alertId',
    element: <FraudAlertDetailPage />,
  },
  {
    // US-016: Customer notification inbox.
    path: '/notifications',
    element: <NotificationsPage />,
  },
]);
