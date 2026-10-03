export function validateNotebookMonths(value) {
  return Number.isInteger(value) && value >= 1 && value <= 36;
}
