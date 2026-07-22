import "@testing-library/jest-dom/vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RenterDashboardPage } from "./RenterDashboardPage";
import { renderWithProviders } from "../../test/renderWithProviders";

const plan = {
  id: "018f6f8d-1111-7000-8000-000000000101",
  renterId: "renter-123",
  billingObligationId: "billing-demo-august-2026",
  rentAmount: 2425,
  initialCollectionAmount: 485,
  repaymentAmount: 1940,
  rentDueDate: "2026-08-01",
  repaymentDueDate: "2026-08-15",
  status: "ACTIVE",
  createdAt: "2026-07-20T12:00:00Z",
  updatedAt: "2026-07-20T12:00:00Z"
};

const movement = {
  id: "018f6f8d-2222-7000-8000-000000000201",
  paymentPlanId: plan.id,
  type: "RENTER_COLLECTION",
  state: "PROCESSING",
  amount: 485,
  currency: "USD",
  operationKey: "demo-renter-collection-august-2026",
  createdAt: "2026-07-20T12:05:00Z",
  updatedAt: "2026-07-20T12:05:00Z"
};

const completedPlan = {
  ...plan,
  id: "018f6f8d-1111-7000-8000-000000000102",
  billingObligationId: "billing-demo-july-2026",
  initialCollectionAmount: 479,
  status: "COMPLETED",
  rentDueDate: "2026-07-01",
  repaymentDueDate: "2026-07-15"
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

describe("RenterDashboardPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows populated renter plans and money movements", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockResolvedValueOnce(await jsonResponse(page([plan])))
        .mockResolvedValueOnce(await jsonResponse(page([movement])))
    );

    renderWithProviders(<RenterDashboardPage />);

    expect(await screen.findByText("billing-demo-august-2026")).toBeInTheDocument();
    expect(screen.getByText("renter collection")).toBeInTheDocument();
    expect(screen.getAllByText("$485.00").length).toBeGreaterThan(0);
    expect(screen.getByText("processing")).toBeInTheDocument();
  });

  it("counts only collection-eligible active plans as open plans", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockResolvedValueOnce(await jsonResponse(page([completedPlan, plan])))
        .mockResolvedValueOnce(await jsonResponse(page([movement])))
    );

    renderWithProviders(<RenterDashboardPage />);

    expect(await screen.findByText("billing-demo-august-2026")).toBeInTheDocument();
    expect(screen.getByText("billing-demo-july-2026")).toBeInTheDocument();
    expect(screen.getByLabelText("Open plans count")).toHaveTextContent("1");
  });

  it("uses the eligible active plan for collection even when a completed plan is listed first", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(await jsonResponse(page([completedPlan, plan])))
      .mockResolvedValueOnce(await jsonResponse(page([movement])))
      .mockResolvedValueOnce(await jsonResponse({
        moneyMovementId: "018f6f8d-2222-7000-8000-000000000301",
        paymentPlanId: plan.id,
        type: "RENTER_COLLECTION",
        state: "PROCESSING",
        amount: 485,
        currency: "USD",
        operationKey: "portal-collection-test",
        createdAt: "2026-07-22T10:00:00Z"
      }, 201))
      .mockResolvedValueOnce(await jsonResponse(page([completedPlan, plan])))
      .mockResolvedValueOnce(await jsonResponse(page([movement])));
    vi.stubGlobal("fetch", fetchMock);

    renderWithProviders(<RenterDashboardPage />);

    await userEvent.click(await screen.findByRole("button", { name: /start collection/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(5));
    const requestBody = JSON.parse(fetchMock.mock.calls[2][1].body as string) as { paymentPlanId: string };
    expect(requestBody.paymentPlanId).toBe(plan.id);
  });

  it("does not show a dashboard collection action when every plan is completed", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockResolvedValueOnce(await jsonResponse(page([completedPlan])))
        .mockResolvedValueOnce(await jsonResponse(page([])))
    );

    renderWithProviders(<RenterDashboardPage />);

    expect(await screen.findByText("billing-demo-july-2026")).toBeInTheDocument();
    expect(screen.getByLabelText("Open plans count")).toHaveTextContent("0");
    expect(screen.queryByRole("button", { name: /start collection/i })).not.toBeInTheDocument();
  });

  it("shows empty states when the renter has no data", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockResolvedValueOnce(await jsonResponse(page([])))
        .mockResolvedValueOnce(await jsonResponse(page([])))
    );

    renderWithProviders(<RenterDashboardPage />);

    expect(await screen.findByText("No payment plans found")).toBeInTheDocument();
    expect(screen.getByText("No money movements yet")).toBeInTheDocument();
  });

  it("starts a collection, shows success feedback, and refreshes dashboard data", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(await jsonResponse(page([plan])))
      .mockResolvedValueOnce(await jsonResponse(page([movement])))
      .mockResolvedValueOnce(await jsonResponse({
        moneyMovementId: "018f6f8d-2222-7000-8000-000000000301",
        paymentPlanId: plan.id,
        type: "RENTER_COLLECTION",
        state: "PROCESSING",
        amount: 485,
        currency: "USD",
        operationKey: "portal-collection-test",
        createdAt: "2026-07-22T10:00:00Z"
      }, 201))
      .mockResolvedValueOnce(await jsonResponse(page([plan])))
      .mockResolvedValueOnce(await jsonResponse(page([movement, { ...movement, id: "018f6f8d-2222-7000-8000-000000000301" }])));
    vi.stubGlobal("fetch", fetchMock);

    renderWithProviders(<RenterDashboardPage />);

    await userEvent.click(await screen.findByRole("button", { name: /start collection/i }));

    expect(await screen.findByText(/Collection started/)).toBeInTheDocument();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(5));
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      "/api/v1/renter-collections",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          Authorization: "Bearer dev:test-user:renter-123:RENTER",
          "Idempotency-Key": expect.stringMatching(/^portal-collection-/)
        })
      })
    );
  });

  it("paginates payment plans with Spring page params and disables first-page previous", async () => {
    const secondPlan = {
      ...plan,
      id: "018f6f8d-1111-7000-8000-000000000103",
      billingObligationId: "billing-demo-september-2026"
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(await jsonResponse(page([plan], { totalElements: 2, totalPages: 2, first: true, last: false })))
      .mockResolvedValueOnce(await jsonResponse(page([])))
      .mockResolvedValueOnce(await jsonResponse(page([secondPlan], {
        number: 1,
        totalElements: 2,
        totalPages: 2,
        first: false,
        last: true
      })));
    vi.stubGlobal("fetch", fetchMock);

    renderWithProviders(<RenterDashboardPage />);

    expect(await screen.findByText("billing-demo-august-2026")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Payment plans previous page" })).toBeDisabled();

    await userEvent.click(screen.getByRole("button", { name: "Payment plans next page" }));

    expect(await screen.findByText("billing-demo-september-2026")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      "/api/v1/me/payment-plans?page=1&size=20&sort=createdAt,desc",
      expect.any(Object)
    );
    expect(screen.getByRole("button", { name: "Payment plans next page" })).toBeDisabled();
  });

  it("paginates dashboard money movements independently", async () => {
    const secondMovement = {
      ...movement,
      id: "018f6f8d-2222-7000-8000-000000000302",
      amount: 777,
      operationKey: "demo-renter-collection-september-2026"
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(await jsonResponse(page([plan])))
      .mockResolvedValueOnce(await jsonResponse(page([movement], { totalElements: 2, totalPages: 2, first: true, last: false })))
      .mockResolvedValueOnce(await jsonResponse(page([secondMovement], {
        number: 1,
        totalElements: 2,
        totalPages: 2,
        first: false,
        last: true
      })));
    vi.stubGlobal("fetch", fetchMock);

    renderWithProviders(<RenterDashboardPage />);

    expect((await screen.findAllByText("$485.00")).length).toBeGreaterThan(0);
    await userEvent.click(screen.getByRole("button", { name: "Money movements next page" }));

    expect(await screen.findByText("$777.00")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      "/api/v1/me/money-movements?page=1&size=20&sort=createdAt%2Cdesc",
      expect.any(Object)
    );
  });

  it("shows collection error feedback from the backend", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockResolvedValueOnce(await jsonResponse(page([plan])))
        .mockResolvedValueOnce(await jsonResponse(page([movement])))
        .mockResolvedValueOnce(await jsonResponse({ code: "IDEMPOTENCY_CONFLICT", message: "Idempotency key already used." }, 409))
    );

    renderWithProviders(<RenterDashboardPage />);

    await userEvent.click(await screen.findByRole("button", { name: /start collection/i }));

    expect(await screen.findByText("Idempotency key already used.")).toBeInTheDocument();
  });
});
