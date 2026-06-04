// Story: US-014 | US-017 | US-018
import type { PageResponse, TransactionDetail, TransactionStatus } from '@/features/transactions/types/transaction.types';

export type FraudRuleConditionType = 'AMOUNT_EXCEEDS' | 'TRANSACTION_FREQUENCY' | 'UNUSUAL_HOUR';
export type FraudRuleStatus = 'ACTIVE' | 'DELETED';
export type FraudAlertReviewStatus = 'PENDING_REVIEW' | 'REVIEWED';

export interface FraudRule {
  id: string;
  name: string;
  conditionType: FraudRuleConditionType;
  thresholdValue: string;
  active: boolean;
  status: FraudRuleStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFraudRuleRequest {
  name: string;
  conditionType: FraudRuleConditionType;
  thresholdValue: string;
  active: boolean;
}

export interface UpdateFraudRuleRequest {
  name?: string;
  thresholdValue?: string;
  active?: boolean;
}

export interface FraudAlertSummary {
  alertId: string;
  transactionId: string;
  amount: string;
  customerFullName: string;
  accountNumber: string;
  triggeredRuleName: string;
  ruleConditionType: FraudRuleConditionType;
  timestamp: string;
  reviewStatus: FraudAlertReviewStatus;
}

export interface FraudAlertDetail {
  alertId: string;
  summary: FraudAlertSummary;
  thresholdValue: string;
  actualValue: string;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  transaction: TransactionDetail & { status: TransactionStatus };
}

export interface FraudAlertFilters {
  dateFrom?: string;
  dateTo?: string;
  reviewStatus?: FraudAlertReviewStatus;
  ruleConditionType?: FraudRuleConditionType;
  page?: number;
  size?: number;
}

export type FraudRulePage = PageResponse<FraudRule>;
export type FraudAlertPage = PageResponse<FraudAlertSummary>;
