import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest, ApiError } from "./http";

describe("apiRequest", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("sends the local dev token as a bearer Authorization header", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ content: [] }), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );
    vi.stubGlobal("fetch", fetchMock);

    await apiRequest("/api/v1/me/payment-plans", {
      token: "dev:test-user:renter-123:RENTER"
    });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/v1/me/payment-plans",
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: "Bearer dev:test-user:renter-123:RENTER"
        })
      })
    );
  });

  it("preserves backend authentication errors instead of masking them", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ code: "UNAUTHORIZED", message: "Authentication is required." }), {
          status: 401,
          headers: { "Content-Type": "application/json" }
        })
      )
    );

    await expect(apiRequest("/api/v1/me/payment-plans", { token: "dev:test-user:renter-123:RENTER" }))
      .rejects.toMatchObject(new ApiError(401, { code: "UNAUTHORIZED", message: "Authentication is required." }));
  });
});
