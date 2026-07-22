import "@testing-library/jest-dom/vitest";
import { Route, Routes } from "react-router-dom";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { PaymentPlanDetailPage } from "./PaymentPlanDetailPage";
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
  status: "COMPLETED"
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

function renderDetail() {
  renderWithProviders(
    <Routes>
      <Route path="/payment-plans/:paymentPlanId" element={<PaymentPlanDetailPage />} />
    </Routes>,
    { initialEntries: [`/payment-plans/${plan.id}`] }
  );
}

describe("PaymentPlanDetailPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows payment-plan detail and plan-scoped movement history", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockResolvedValueOnce(await jsonResponse(plan))
        .mockResolvedValueOnce(await jsonResponse(page([movement])))
    );

    renderDetail();

    expect(await screen.findByRole("heading", { name: "billing-demo-august-2026" })).toBeInTheDocument();
    expect(screen.getByText("Repayment due")).toBeInTheDocument();
    expect(screen.getByText("renter collection")).toBeInTheDocument();
    expect(screen.getByText("processing")).toBeInTheDocument();
  });

  it("does not enable collection for completed plans", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockResolvedValueOnce(await jsonResponse(completedPlan))
        .mockResolvedValueOnce(await jsonResponse(page([movement])))
    );

    renderDetail();

    expect(await screen.findByRole("heading", { name: "billing-demo-august-2026" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /start collection/i })).toBeDisabled();
  });

  it("starts a collection from the detail page and refreshes plan activity", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(await jsonResponse(plan))
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
      .mockResolvedValueOnce(await jsonResponse(plan))
      .mockResolvedValueOnce(await jsonResponse(page([movement])));
    vi.stubGlobal("fetch", fetchMock);

    renderDetail();

    await userEvent.click(await screen.findByRole("button", { name: /start collection/i }));

    expect(await screen.findByText(/Collection started/)).toBeInTheDocument();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(5));
  });

  it("paginates plan-scoped money movements with the paymentPlanId filter", async () => {
    const secondMovement = {
      ...movement,
      id: "018f6f8d-2222-7000-8000-000000000302",
      amount: 777,
      operationKey: "demo-renter-collection-september-2026"
    };
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(await jsonResponse(plan))
      .mockResolvedValueOnce(await jsonResponse(page([movement], { totalElements: 2, totalPages: 2, first: true, last: false })))
      .mockResolvedValueOnce(await jsonResponse(page([secondMovement], {
        number: 1,
        totalElements: 2,
        totalPages: 2,
        first: false,
        last: true
      })));
    vi.stubGlobal("fetch", fetchMock);

    renderDetail();

    expect((await screen.findAllByText("$485.00")).length).toBeGreaterThan(0);
    await userEvent.click(screen.getByRole("button", { name: "Plan money movements next page" }));

    expect(await screen.findByText("$777.00")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      `/api/v1/me/money-movements?page=1&size=20&sort=createdAt%2Cdesc&paymentPlanId=${plan.id}`,
      expect.any(Object)
    );
  });

  it("shows backend error feedback", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockResolvedValueOnce(await jsonResponse({ code: "PAYMENT_PLAN_NOT_FOUND", message: "Payment plan not found." }, 404))
        .mockResolvedValueOnce(await jsonResponse(page([])))
    );

    renderDetail();

    expect(await screen.findByText("Payment plan not found.")).toBeInTheDocument();
  });
});
