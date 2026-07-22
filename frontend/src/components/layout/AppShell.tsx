import { BriefcaseBusiness, CreditCard, Database, LogOut, ReceiptText } from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../auth/authContext";

export function AppShell() {
  const auth = useAuth();
  const canUseOperations = auth.roles.some((role) => ["SUPPORT", "FINOPS", "ADMIN"].includes(role));
  const canUseRenter = auth.roles.includes("RENTER");

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-mark">RP</div>
          <div>
            <div className="brand-title">Rent Payment</div>
            <div className="brand-subtitle">{canUseOperations ? "Operations portal" : "Renter portal"}</div>
          </div>
        </div>

        <nav className="side-nav" aria-label="Primary navigation">
          {canUseRenter ? (
            <>
              <NavLink to="/" end>
                <CreditCard aria-hidden="true" size={18} />
                Dashboard
              </NavLink>
              <NavLink to="/#movements">
                <ReceiptText aria-hidden="true" size={18} />
                Activity
              </NavLink>
            </>
          ) : null}
          {canUseOperations ? (
            <>
              <NavLink to="/ops/money-movements">
                <BriefcaseBusiness aria-hidden="true" size={18} />
                Money
              </NavLink>
              <NavLink to="/ops/provider-transactions">
                <Database aria-hidden="true" size={18} />
                Provider Txns
              </NavLink>
              <NavLink to="/ops/provider-webhook-events">
                <Database aria-hidden="true" size={18} />
                Webhooks
              </NavLink>
              <NavLink to="/ops/outbox-events">
                <Database aria-hidden="true" size={18} />
                Outbox
              </NavLink>
              <NavLink to="/ops/settlement-records">
                <Database aria-hidden="true" size={18} />
                Settlements
              </NavLink>
              <NavLink to="/ops/reconciliation-runs">
                <Database aria-hidden="true" size={18} />
                Runs
              </NavLink>
              <NavLink to="/ops/reconciliation-exceptions">
                <Database aria-hidden="true" size={18} />
                Exceptions
              </NavLink>
            </>
          ) : null}
        </nav>

        <div className="principal-card">
          <span className="label">Signed in as</span>
          <strong>{auth.subject ?? "dev user"}</strong>
          <span>{auth.renterId ?? "No renter scope"}</span>
          <span>{auth.roles.join(", ")}</span>
          <button type="button" className="icon-text-button" onClick={auth.signOut}>
            <LogOut aria-hidden="true" size={16} />
            Sign out
          </button>
        </div>
      </aside>

      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
