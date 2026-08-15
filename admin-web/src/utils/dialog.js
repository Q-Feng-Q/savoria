export const ACTION_DIALOG_EVENT = 'shiguang:action-dialog';

function openDialog(config) {
  if (typeof window === 'undefined') return Promise.resolve(null);
  return new Promise((resolve) => {
    window.dispatchEvent(new CustomEvent(ACTION_DIALOG_EVENT, { detail: { ...config, resolve } }));
  });
}

export function promptAction(config = {}) {
  return openDialog({ mode: 'prompt', inputType: 'text', required: true, ...config });
}

export function confirmAction(config = {}) {
  return openDialog({ mode: 'confirm', danger: false, ...config });
}
