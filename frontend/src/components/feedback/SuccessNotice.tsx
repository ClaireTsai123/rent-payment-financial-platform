import { CheckCircle2 } from "lucide-react";
import type { ReactNode } from "react";

export function SuccessNotice({ children }: { children: ReactNode }) {
  return (
    <div className="notice notice-success" role="status">
      <CheckCircle2 aria-hidden="true" size={18} />
      <span>{children}</span>
    </div>
  );
}
