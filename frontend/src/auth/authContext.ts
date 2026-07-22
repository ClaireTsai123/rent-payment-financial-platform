import { createContext, useContext } from "react";

export type AuthContextValue = {
  token: string | null;
  subject: string | null;
  renterId: string | null;
  roles: string[];
  signIn: (token: string) => void;
  signOut: () => void;
};

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
