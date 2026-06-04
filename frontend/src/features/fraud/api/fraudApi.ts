// Story: US-014 | US-017 | US-018
import apiClient from '@/shared/api/client';
import type {
  CreateFraudRuleRequest,
  FraudAlertDetail,
  FraudAlertFilters,
  FraudAlertPage,
  FraudRule,
  FraudRulePage,
  UpdateFraudRuleRequest,
} from '../types/fraud.types';

export async function getFraudRules(page = 0): Promise<FraudRulePage> {
  const { data } = await apiClient.get<FraudRulePage>('/api/v1/fraud/rules', {
    params: { page, size: 20 },
  });
  return data;
}

export async function createFraudRule(request: CreateFraudRuleRequest): Promise<FraudRule> {
  const { data } = await apiClient.post<FraudRule>('/api/v1/fraud/rules', request);
  return data;
}

export async function updateFraudRule(id: string, request: UpdateFraudRuleRequest): Promise<FraudRule> {
  const { data } = await apiClient.patch<FraudRule>(`/api/v1/fraud/rules/${id}`, request);
  return data;
}

export async function deleteFraudRule(id: string): Promise<void> {
  await apiClient.delete(`/api/v1/fraud/rules/${id}`);
}

export async function getFraudAlerts(filters: FraudAlertFilters = {}): Promise<FraudAlertPage> {
  const { data } = await apiClient.get<FraudAlertPage>('/api/v1/fraud/alerts', {
    params: buildParams(filters),
  });
  return data;
}

export async function getFraudAlert(alertId: string): Promise<FraudAlertDetail> {
  const { data } = await apiClient.get<FraudAlertDetail>(`/api/v1/fraud/alerts/${alertId}`);
  return data;
}

export async function approveFraudAlert(alertId: string): Promise<FraudAlertDetail> {
  const { data } = await apiClient.post<FraudAlertDetail>(`/api/v1/fraud/alerts/${alertId}/approve`);
  return data;
}

export async function rejectFraudAlert(alertId: string): Promise<FraudAlertDetail> {
  const { data } = await apiClient.post<FraudAlertDetail>(`/api/v1/fraud/alerts/${alertId}/reject`);
  return data;
}

function buildParams(filters: FraudAlertFilters): Record<string, string | number> {
  return Object.entries(filters).reduce<Record<string, string | number>>((params, [key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params[key] = value;
    }
    return params;
  }, {});
}
