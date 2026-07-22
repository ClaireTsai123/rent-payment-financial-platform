import { AlertTriangle } from "lucide-react";

export function ErrorNotice({ message }: { message: string }) {
  return (
    <div className="notice notice-error" role="alert">
      <AlertTriangle aria-hidden="true" size={18} />
      <span>{message}</span>
    </div>
  );
}
