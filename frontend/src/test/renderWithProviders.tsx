import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { MemoryRouter } from "react-router-dom";
import { AuthContext, type AuthContextValue } from "../auth/authContext";

const defaultAuth: AuthContextValue = {
  token: "dev:test-user:renter-123:RENTER",
  subject: "test-user",
  renterId: "renter-123",
  roles: ["RENTER"],
  signIn: () => undefined,
  signOut: () => undefined
};

type RenderOptions = {
  auth?: Partial<AuthContextValue>;
  initialEntries?: string[];
};

export function renderWithProviders(ui: ReactElement, options: RenderOptions = {}) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false }
    }
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <AuthContext.Provider value={{ ...defaultAuth, ...options.auth }}>
        <MemoryRouter initialEntries={options.initialEntries}>{ui}</MemoryRouter>
      </AuthContext.Provider>
    </QueryClientProvider>
  );
}
