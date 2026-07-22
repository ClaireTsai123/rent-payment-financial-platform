import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "../components/layout/AppShell";
import { ProtectedRoute } from "../auth/ProtectedRoute";
import { HomeRoute } from "./HomeRoute";
import { DevTokenPage } from "../pages/renter/DevTokenPage";
import { MoneyMovementDetailPage } from "../pages/renter/MoneyMovementDetailPage";
import { OperationsDetailPage } from "../pages/operations/OperationsDetailPage";
import { OperationsListPage } from "../pages/operations/OperationsListPage";
import { PaymentPlanDetailPage } from "../pages/renter/PaymentPlanDetailPage";

const operationsRoles = ["SUPPORT", "FINOPS", "ADMIN"];

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
        <Route index element={<HomeRoute />} />
        <Route path="payment-plans/:paymentPlanId" element={<PaymentPlanDetailPage />} />
        <Route path="money-movements/:moneyMovementId" element={<MoneyMovementDetailPage />} />
        <Route
          path="ops"
          element={
            <ProtectedRoute allowedRoles={operationsRoles}>
              <Navigate to="/ops/money-movements" replace />
            </ProtectedRoute>
          }
        />
        <Route
          path="ops/:resourceKey"
          element={
            <ProtectedRoute allowedRoles={operationsRoles}>
              <OperationsListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="ops/:resourceKey/:id"
          element={
            <ProtectedRoute allowedRoles={operationsRoles}>
              <OperationsDetailPage />
            </ProtectedRoute>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
