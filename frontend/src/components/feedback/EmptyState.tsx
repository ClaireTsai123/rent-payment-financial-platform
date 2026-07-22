import { Inbox } from "lucide-react";

export function EmptyState({ title, detail }: { title: string; detail?: string }) {
  return (
    <div className="empty-state">
      <Inbox aria-hidden="true" size={22} />
      <strong>{title}</strong>
      {detail ? <span>{detail}</span> : null}
    </div>
  );
}
