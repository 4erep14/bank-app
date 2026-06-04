// Story: US-019 | US-020
export type CustomerStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED' | 'CLOSED' | 'LOCKED' | 'INACTIVE';

export interface AdminCustomerSummary {
  id: string;
  fullName: string;
  email: string;
  phone: string;
  status: CustomerStatus;
  createdAt: string;
}

export type AuditActionType =
  | 'LOGIN_SUCCESS'
  | 'LOGIN_FAILURE'
  | 'LOGOUT'
  | 'ACCOUNT_OPENED'
  | 'ACCOUNT_DEACTIVATED'
  | 'ACCOUNT_ACTIVATED'
  | 'TRANSFER_SUBMITTED'
  | 'TRANSFER_BLOCKED'
  | 'TRANSFER_COMPLETED'
  | 'FRAUD_RULE_CREATED'
  | 'FRAUD_RULE_UPDATED'
  | 'FRAUD_RULE_DELETED'
  | 'TRANSACTION_UNBLOCKED'
  | 'TRANSACTION_REJECTED'
  | 'CUSTOMER_DEACTIVATED'
  | 'CUSTOMER_UNLOCKED';

export interface AuditLogEntry {
  id: string;
  actorId?: string | null;
  actorRole: string;
  actionType: AuditActionType;
  targetEntityType: string;
  targetEntityId?: string | null;
  timestamp: string;
  ipAddress?: string | null;
}

export interface SpringPage<T> {
  content: T[];
  number?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
  first?: boolean;
  last?: boolean;
  page?: {
    number: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

export interface AdminCustomerFilters {
  status?: CustomerStatus | '';
  page?: number;
  size?: number;
}

export interface AuditLogFilters {
  actorId?: string;
  actionType?: AuditActionType | '';
  dateFrom?: string;
  dateTo?: string;
  page?: number;
  size?: number;
}
