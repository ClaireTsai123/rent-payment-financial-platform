import "@testing-library/jest-dom/vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { App } from "../../app/App";
import { renderWithProviders } from "../../test/renderWithProviders";

const supportAuth = {
  token: "dev:support-user:-:SUPPORT",
  subject: "support-user",
  renterId: null,
  roles: ["SUPPORT"]
};

const movement = {
  id: "018f6f8d-2222-7000-8000-000000000201",
  paymentPlanId: "018f6f8d-1111-7000-8000-000000000101",
  renterId: "renter-123",
  type: "RENTER_COLLECTION",
  state: "PROCESSING",
  amount: 485,
  currency: "USD",
  operationKey: "ops-demo-collection",
  createdAt: "2026-07-20T12:05:00Z",
  updatedAt: "2026-07-20T12:05:00Z"
};

const providerTransaction = {
  id: "018f6f8d-3333-7000-8000-000000000301",
  moneyMovementId: movement.id,
  paymentAttemptId: "018f6f8d-3333-7000-8000-000000000302",
  provider: "mock-provider",
  providerTransactionId: "mock-txn-123",
  providerIdempotencyKey: "ops-demo-collection",
  normalizedStatus: "PROCESSING",
  rawStatus: "processing",
  settlementReference: null,
  createdAt: "2026-07-20T12:06:00Z",
  updatedAt: "2026-07-20T12:06:00Z"
};

function page<T>(content: T[], overrides: Partial<ReturnType<typeof pageBase>> = {}) {
  return {
    ...pageBase(),
    content,
    totalElements: content.length,
    totalPages: content.length ? 1 : 0,
    empty: content.length === 0,
    ...overrides
  };
}

function pageBase() {
  return {
    size: 20,
    number: 0,
    first: true,
    last: true,
    totalElements: 0,
    totalPages: 0,
    empty: true
  };
}

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" }
    })
  );
}

describe("Operations portal", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("blocks renters from operations routes", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    renderWithProviders(<App />, { initialEntries: ["/ops/money-movements"] });

    expect(await screen.findByText("Access is denied.")).toBeInTheDocument();
    expect(screen.getByText(/SUPPORT, FINOPS, ADMIN/)).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("renders an operations list with support authorization", async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(await jsonResponse(page([movement])));
    vi.stubGlobal("fetch", fetchMock);

    renderWithProviders(<App />, { auth: supportAuth, initialEntries: ["/ops/money-movements"] });

    expect(await screen.findByText("ops-demo-collection")).toBeInTheDocument();
    expect(screen.getByText("renter collection")).toBeInTheDocument();
    expect(screen.getByText("processing")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/ops/money-movements?page=0&size=20",
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer dev:support-user:-:SUPPORT" })
      })
    );
  });

  it("applies filters and resets to the first page", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(await jsonResponse(page([])))
      .mockResolvedValueOnce(await jsonResponse(page([movement])));
    vi.stubGlobal("fetch", fetchMock);

    renderWithProviders(<App />, { auth: supportAuth, initialEntries: ["/ops/money-movements"] });

    await userEvent.selectOptions(await screen.findByLabelText("State"), "PROCESSING");
    await userEvent.type(screen.getByLabelText("Renter ID"), "renter-123");
    await userEvent.type(screen.getByLabelText("Created from"), "2026-08-01T10:30");
    await userEvent.click(screen.getByRole("button", { name: "Apply" }));

    expect(await screen.findByText("ops-demo-collection")).toBeInTheDocument();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2));
    const createdFrom = encodeURIComponent(new Date("2026-08-01T10:30").toISOString());
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      `/api/v1/ops/money-movements?page=0&size=20&state=PROCESSING&renterId=renter-123&createdFrom=${createdFrom}`,
      expect.any(Object)
    );
  });

  it("paginates operations lists", async () => {
    const secondMovement = { ...movement, id: "018f6f8d-2222-7000-8000-000000000202", operationKey: "ops-second-page" };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(await jsonResponse(page([movement], { totalElements: 2, totalPages: 2, first: true, last: false })))
      .mockResolvedValueOnce(await jsonResponse(page([secondMovement], { number: 1, totalElements: 2, totalPages: 2, first: false, last: true })));
    vi.stubGlobal("fetch", fetchMock);

    renderWithProviders(<App />, { auth: supportAuth, initialEntries: ["/ops/money-movements"] });

    expect(await screen.findByText("ops-demo-collection")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Money movements pagination next page" }));

    expect(await screen.findByText("ops-second-page")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenNthCalledWith(2, "/api/v1/ops/money-movements?page=1&size=20", expect.any(Object));
  });

  it("renders money movement detail with state history", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValueOnce(await jsonResponse({
        ...movement,
        version: 2,
        stateHistory: [
          {
            id: "018f6f8d-4444-7000-8000-000000000401",
            fromState: null,
            toState: "PROCESSING",
            reason: "PROVIDER_SUBMITTED",
            changedAt: "2026-07-20T12:06:00Z"
          },
          {
            id: "018f6f8d-4444-7000-8000-000000000402",
            fromState: "PROCESSING",
            toState: "SUCCEEDED",
            reason: "WEBHOOK_SUCCEEDED",
            changedAt: "2026-07-20T12:07:00Z"
          }
        ]
      }))
    );

    renderWithProviders(<App />, { auth: supportAuth, initialEntries: [`/ops/money-movements/${movement.id}`] });

    expect(await screen.findByText("ops-demo-collection")).toBeInTheDocument();
    expect(screen.getByText("State history")).toBeInTheDocument();
    expect(screen.getByText("PROVIDER_SUBMITTED")).toBeInTheDocument();
    expect(screen.getByText("WEBHOOK_SUCCEEDED")).toBeInTheDocument();
  });

  it("renders provider transaction detail", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValueOnce(await jsonResponse(providerTransaction)));

    renderWithProviders(<App />, {
      auth: supportAuth,
      initialEntries: [`/ops/provider-transactions/${providerTransaction.id}`]
    });

    expect(await screen.findByText("mock-txn-123")).toBeInTheDocument();
    expect(screen.getByText("mock-provider")).toBeInTheDocument();
  });

  it("shows backend error feedback", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValueOnce(await jsonResponse({ code: "INVALID_FILTER", message: "createdFrom must be before createdTo." }, 400))
    );

    renderWithProviders(<App />, { auth: supportAuth, initialEntries: ["/ops/money-movements"] });

    expect(await screen.findByText("createdFrom must be before createdTo.")).toBeInTheDocument();
  });
});
