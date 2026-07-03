import type { NotificationResponse } from '@/api/types'

// The response DTO has no explicit "which side of the transfer is this"
// field, but the real message templates (verified against
// TransferEventListener source) are distinguishable: only the
// BloodTransferRequestedEvent notification — sent to the SOURCE hospital,
// which needs to approve/reject — starts with "New blood transfer request".
// Every other transfer notification (approved/rejected/completed/cancelled)
// goes to the REQUESTING hospital tracking its own request's status.
export function getNotificationLink(notification: NotificationResponse): string | null {
  if (notification.type === 'DONOR') {
    return '/donor'
  }
  if (notification.type === 'TRANSFER') {
    return notification.message.startsWith('New blood transfer request')
      ? '/transfers/pending'
      : '/transfers/my-requests'
  }
  return null
}
