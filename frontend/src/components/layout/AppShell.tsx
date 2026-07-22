import { CreditCard, LogOut, ReceiptText } from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../auth/authContext";

export function AppShell() {
  const auth = useAuth();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand-block">
          <div className="brand-mark">RP</div>
          <div>
            <div className="brand-title">Rent Payment</div>
            <div className="brand-subtitle">Renter portal</div>
          </div>
        </div>

        <nav className="side-nav" aria-label="Primary navigation">
          <NavLink to="/" end>
            <CreditCard aria-hidden="true" size={18} />
            Dashboard
          </NavLink>
          <NavLink to="/#movements">
            <ReceiptText aria-hidden="true" size={18} />
            Activity
          </NavLink>
        </nav>

        <div className="principal-card">
          <span className="label">Signed in as</span>
          <strong>{auth.subject ?? "dev user"}</strong>
          <span>{auth.renterId ?? "No renter scope"}</span>
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
