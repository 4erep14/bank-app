// Story: US-019 | US-020
import apiClient from '@/shared/api/client';
import type {
  AdminCustomerFilters,
  AdminCustomerSummary,
  AuditLogEntry,
  AuditLogFilters,
  SpringPage,
} from '../types/admin.types';

export async function getAdminCustomers(
  filters: AdminCustomerFilters = {},
): Promise<SpringPage<AdminCustomerSummary>> {
  const { data } = await apiClient.get<SpringPage<AdminCustomerSummary>>(
    '/api/v1/admin/customers',
    { params: buildParams(filters) },
  );
  return data;
}

export async function deactivateCustomer(id: string): Promise<AdminCustomerSummary> {
  const { data } = await apiClient.patch<AdminCustomerSummary>(
    `/api/v1/admin/customers/${id}/deactivate`,
  );
  return data;
}

export async function unlockCustomer(id: string): Promise<AdminCustomerSummary> {
  const { data } = await apiClient.patch<AdminCustomerSummary>(
    `/api/v1/admin/customers/${id}/unlock`,
  );
  return data;
}

export async function getAuditLogs(
  filters: AuditLogFilters = {},
): Promise<SpringPage<AuditLogEntry>> {
  const { data } = await apiClient.get<SpringPage<AuditLogEntry>>(
    '/api/v1/admin/audit-logs',
    { params: buildParams(filters) },
  );
  return data;
}

function buildParams(filters: object): Record<string, string | number> {
  return Object.entries(filters).reduce<Record<string, string | number>>((params, [key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = typeof value === 'number' ? value : String(value);
    }
    return params;
  }, {});
}
