export const FEEDBACK_EVENT = 'shiguang:feedback';

export function notify(message, tone = 'info', duration = 2800) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new CustomEvent(FEEDBACK_EVENT, { detail: { message, tone, duration } }));
}
