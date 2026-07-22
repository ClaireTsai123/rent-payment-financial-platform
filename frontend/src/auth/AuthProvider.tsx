import { useMemo, useState, type ReactNode } from "react";
import { AuthContext, type AuthContextValue } from "./authContext";

const TOKEN_STORAGE_KEY = "rent-payment.devBearerToken";

function parseDevToken(token: string) {
  if (!token.startsWith("dev:")) {
    return { subject: null, renterId: null, roles: [] };
  }

  const [, subject, renterId, roleSegment = ""] = token.split(":");
  return {
    subject: subject || null,
    renterId: renterId && renterId !== "-" ? renterId : null,
    roles: roleSegment.split(",").map((role) => role.trim()).filter(Boolean)
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_STORAGE_KEY));

  const value = useMemo<AuthContextValue>(() => {
    const parsed = token ? parseDevToken(token) : { subject: null, renterId: null, roles: [] };

    return {
      token,
      ...parsed,
      signIn(nextToken) {
        const normalizedToken = nextToken.trim();
        localStorage.setItem(TOKEN_STORAGE_KEY, normalizedToken);
        setToken(normalizedToken);
      },
      signOut() {
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        setToken(null);
      }
    };
  }, [token]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
