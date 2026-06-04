// Story: US-016
import apiClient from '@/shared/api/client';
import type { NotificationPage } from '../types/notification.types';

export async function getNotifications(page = 0): Promise<NotificationPage> {
  const { data } = await apiClient.get<NotificationPage>('/api/v1/notifications', {
    params: { page, size: 20 },
  });
  return data;
}
