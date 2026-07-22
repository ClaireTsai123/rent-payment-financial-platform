import { FormEvent, useState } from "react";
import { KeyRound, LogIn } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/authContext";

const DEFAULT_TOKEN = "dev:test-user:renter-123:RENTER";

export function DevTokenPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [token, setToken] = useState(auth.token ?? DEFAULT_TOKEN);

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    auth.signIn(token);
    const destination = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? "/";
    navigate(destination, { replace: true });
  }

  return (
    <main className="login-page">
      <form className="login-panel" onSubmit={onSubmit}>
        <div className="brand-block">
          <div className="brand-mark">RP</div>
          <div>
            <h1>Rent Payment</h1>
            <p>Local renter portal access</p>
          </div>
        </div>

        <label className="field">
          <span>
            <KeyRound aria-hidden="true" size={16} />
            Dev bearer token
          </span>
          <input value={token} onChange={(event) => setToken(event.target.value)} spellCheck={false} />
        </label>

        <button type="submit" className="primary-button">
          <LogIn aria-hidden="true" size={18} />
          Continue
        </button>
      </form>
    </main>
  );
}
