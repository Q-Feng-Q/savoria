import { listNotifications } from './notifications';
import { listMerchantOrders } from './orders';
import { getPurchaseSummary } from './purchases';

export async function getDashboardSnapshot(date) {
  const [orders, purchases, notifications] = await Promise.all([
    listMerchantOrders(),
    getPurchaseSummary({ date, includePending: true }),
    listNotifications({ receiverScope: 'merchant', pageSize: 8 })
  ]);

  return {
    orders,
    purchases,
    notifications
  };
}
