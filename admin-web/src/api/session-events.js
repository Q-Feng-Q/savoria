export const SESSION_INVALID_EVENT = 'shiguang:session-invalid';

export function emitSessionInvalid(error) {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new CustomEvent(SESSION_INVALID_EVENT, { detail: { error } }));
}
