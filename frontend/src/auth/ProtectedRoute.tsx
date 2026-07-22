import type { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "./authContext";

export function ProtectedRoute({ children, allowedRoles }: { children: ReactNode; allowedRoles?: string[] }) {
  const auth = useAuth();
  const location = useLocation();

  if (!auth.token) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (allowedRoles && !allowedRoles.some((role) => auth.roles.includes(role))) {
    return (
      <div className="auth-denied" role="alert">
        <strong>Access is denied.</strong>
        <span>This page requires one of: {allowedRoles.join(", ")}.</span>
      </div>
    );
  }

  return children;
}
