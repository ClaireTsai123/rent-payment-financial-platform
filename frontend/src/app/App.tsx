import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "../components/layout/AppShell";
import { ProtectedRoute } from "../auth/ProtectedRoute";
import { DevTokenPage } from "../pages/renter/DevTokenPage";
import { MoneyMovementDetailPage } from "../pages/renter/MoneyMovementDetailPage";
import { PaymentPlanDetailPage } from "../pages/renter/PaymentPlanDetailPage";
import { RenterDashboardPage } from "../pages/renter/RenterDashboardPage";

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<DevTokenPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AppShell />
          </ProtectedRoute>
        }
      >
        <Route index element={<RenterDashboardPage />} />
        <Route path="payment-plans/:paymentPlanId" element={<PaymentPlanDetailPage />} />
        <Route path="money-movements/:moneyMovementId" element={<MoneyMovementDetailPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
