import { StatusPill } from "../../components/status/StatusPill";
import { formatDateTime, formatMoney, humanizeEnum } from "../../utils/formatters";

export function readValue(row: unknown, key: string) {
  if (!row || typeof row !== "object") {
    return null;
  }
  return (row as Record<string, unknown>)[key] ?? null;
}

export function renderOperationsValue(value: unknown, kind?: string, currency?: string) {
  if (value === null || value === undefined || value === "") {
    return <span className="muted">...</span>;
  }
  if (kind === "status") {
    return <StatusPill value={String(value)} />;
  }
  if (kind === "date") {
    return formatDateTime(String(value));
  }
  if (kind === "money") {
    return typeof value === "number" ? formatMoney(value, currency ?? "USD") : String(value);
  }
  if (kind === "code") {
    return <code>{String(value)}</code>;
  }
  if (kind === "multiline") {
    return <pre className="detail-pre">{String(value)}</pre>;
  }
  if (typeof value === "string" && /^[A-Z0-9_]+$/.test(value)) {
    return humanizeEnum(value);
  }
  return String(value);
}
