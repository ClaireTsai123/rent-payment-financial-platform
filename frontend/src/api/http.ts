import type { ApiErrorResponse } from "./types";

export class ApiError extends Error {
  readonly status: number;
  readonly body: ApiErrorResponse | string | null;

  constructor(status: number, body: ApiErrorResponse | string | null) {
    const message =
      typeof body === "object" && body?.message
        ? body.message
        : typeof body === "string" && body.length > 0
          ? body
          : `Request failed with status ${status}`;
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

type RequestOptions = {
  token: string;
  idempotencyKey?: string;
  method?: "GET" | "POST";
  body?: unknown;
};

export async function apiRequest<T>(path: string, options: RequestOptions): Promise<T> {
  const response = await fetch(path, {
    method: options.method ?? "GET",
    headers: {
      Authorization: `Bearer ${options.token}`,
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.idempotencyKey ? { "Idempotency-Key": options.idempotencyKey } : {})
    },
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  if (!response.ok) {
    const contentType = response.headers.get("content-type") ?? "";
    const body = contentType.includes("application/json") ? await response.json() : await response.text();
    throw new ApiError(response.status, body);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}
