import dayjs from 'dayjs';

export function formatDateTime(value) {
  if (!value) {
    return '-';
  }
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss');
}

export function safeParseJson(value, fallback = {}) {
  if (!value) {
    return fallback;
  }
  try {
    return JSON.parse(value);
  } catch (error) {
    return fallback;
  }
}

export function formatStatus(status, options = {}) {
  return options[status] || String(status || '-');
}