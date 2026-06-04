// Story: US-016
import type { PageResponse } from '@/features/transactions/types/transaction.types';

export type NotificationType = 'TRANSACTION_BLOCKED';
export type NotificationStatus = 'SENT';

export interface NotificationItem {
  id: string;
  type: NotificationType;
  transactionId: string;
  amount: string;
  timestamp: string;
  triggeredRuleName: string;
  status: NotificationStatus;
}

export type NotificationPage = PageResponse<NotificationItem>;
