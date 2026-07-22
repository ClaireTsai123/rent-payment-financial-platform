import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/authContext";
import { RenterDashboardPage } from "../pages/renter/RenterDashboardPage";

export function HomeRoute() {
  const auth = useAuth();
  const canUseOperations = auth.roles.some((role) => ["SUPPORT", "FINOPS", "ADMIN"].includes(role));

  if (!auth.roles.includes("RENTER") && canUseOperations) {
    return <Navigate to="/ops/money-movements" replace />;
  }

  return <RenterDashboardPage />;
}
