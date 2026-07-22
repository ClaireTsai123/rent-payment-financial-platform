import { humanizeEnum } from "../../utils/formatters";

const toneByStatus: Record<string, string> = {
  ACTIVE: "tone-success",
  CREATED: "tone-neutral",
  COMPLETED: "tone-neutral",
  CANCELLED: "tone-warning",
  SUBMITTED: "tone-info",
  PROCESSING: "tone-info",
  SUCCEEDED: "tone-success",
  FAILED: "tone-danger",
  RETURNED: "tone-warning",
  REVERSED: "tone-neutral"
};

export function StatusPill({ value }: { value: string }) {
  return <span className={`status-pill ${toneByStatus[value] ?? "tone-neutral"}`}>{humanizeEnum(value)}</span>;
}
