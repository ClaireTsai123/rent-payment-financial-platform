import "@testing-library/jest-dom/vitest";
import { Route, Routes } from "react-router-dom";
import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { MoneyMovementDetailPage } from "./MoneyMovementDetailPage";
import { renderWithProviders } from "../../test/renderWithProviders";

const movement = {
  id: "018f6f8d-2222-7000-8000-000000000201",
  paymentPlanId: "018f6f8d-1111-7000-8000-000000000101",
  type: "RENTER_COLLECTION",
  state: "PROCESSING",
  amount: 485,
  currency: "USD",
  operationKey: "demo-renter-collection-august-2026",
  createdAt: "2026-07-20T12:05:00Z",
  updatedAt: "2026-07-20T12:05:00Z"
};

function jsonResponse(body: unknown, status = 200) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" }
    })
  );
}

describe("MoneyMovementDetailPage", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows money-movement detail", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValueOnce(await jsonResponse(movement)));

    renderWithProviders(
      <Routes>
        <Route path="/money-movements/:moneyMovementId" element={<MoneyMovementDetailPage />} />
      </Routes>,
      { initialEntries: [`/money-movements/${movement.id}`] }
    );

    expect(await screen.findByRole("heading", { name: "renter collection" })).toBeInTheDocument();
    expect(screen.getByText("$485.00")).toBeInTheDocument();
    expect(screen.getByText("demo-renter-collection-august-2026")).toBeInTheDocument();
    expect(screen.getAllByText("processing").length).toBeGreaterThan(0);
  });

  it("shows backend errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValueOnce(await jsonResponse({ code: "MONEY_MOVEMENT_NOT_FOUND", message: "Money movement not found." }, 404))
    );

    renderWithProviders(
      <Routes>
        <Route path="/money-movements/:moneyMovementId" element={<MoneyMovementDetailPage />} />
      </Routes>,
      { initialEntries: [`/money-movements/${movement.id}`] }
    );

    expect(await screen.findByText("Money movement not found.")).toBeInTheDocument();
  });
});
