const MAX_BODY_BYTES = 64 * 1024;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

type InviteEmail = {
  to: string;
  subject: string;
  html: string;
  text: string;
};

class RequestError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
  }
}

function json(status: number, body: Record<string, unknown>): Response {
  return Response.json(body, {
    status,
    headers: { "Cache-Control": "no-store" },
  });
}

async function secretsMatch(provided: string, expected: string): Promise<boolean> {
  const encoder = new TextEncoder();
  const [left, right] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(provided)),
    crypto.subtle.digest("SHA-256", encoder.encode(expected)),
  ]);
  const leftBytes = new Uint8Array(left);
  const rightBytes = new Uint8Array(right);
  let difference = 0;
  for (let index = 0; index < leftBytes.byteLength; index += 1) {
    difference |= leftBytes[index] ^ rightBytes[index];
  }
  return difference === 0;
}

async function readJson(request: Request): Promise<unknown> {
  const declaredLength = Number(request.headers.get("content-length") ?? "0");
  if (Number.isFinite(declaredLength) && declaredLength > MAX_BODY_BYTES) {
    throw new RequestError(413, "Request body is too large");
  }
  if (!request.body) throw new RequestError(400, "Request body is required");

  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let total = 0;
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      total += value.byteLength;
      if (total > MAX_BODY_BYTES) throw new RequestError(413, "Request body is too large");
      chunks.push(value);
    }
  } finally {
    reader.releaseLock();
  }

  const bytes = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    bytes.set(chunk, offset);
    offset += chunk.byteLength;
  }

  try {
    return JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    throw new RequestError(400, "Request body must be valid JSON");
  }
}

function requireString(value: unknown, field: string, maxLength: number): string {
  if (typeof value !== "string") throw new RequestError(400, `${field} is required`);
  const cleaned = value.trim();
  if (!cleaned || cleaned.length > maxLength) throw new RequestError(400, `${field} is invalid`);
  return cleaned;
}

function validate(payload: unknown): InviteEmail {
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    throw new RequestError(400, "Request body must be an object");
  }
  const body = payload as Record<string, unknown>;
  const to = requireString(body.to, "to", 320);
  if (!EMAIL_PATTERN.test(to)) throw new RequestError(400, "to is invalid");
  return {
    to,
    subject: requireString(body.subject, "subject", 200),
    html: requireString(body.html, "html", 48 * 1024),
    text: requireString(body.text, "text", 16 * 1024),
  };
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") {
      return json(200, { status: "ok" });
    }
    if (request.method !== "POST" || url.pathname !== "/send") {
      return json(404, { error: "Not found" });
    }

    const providedSecret = request.headers.get("X-Mailer-Secret") ?? "";
    if (!providedSecret || !(await secretsMatch(providedSecret, env.MAILER_SECRET))) {
      return json(401, { error: "Unauthorized" });
    }

    try {
      const message = validate(await readJson(request));
      const result = await env.EMAIL.send({
        ...message,
        from: { email: "invite@deploro.com", name: "JAVA_CORE" },
      });
      console.log(JSON.stringify({ event: "invite_email_sent", messageId: result.messageId }));
      return json(202, { sent: true, messageId: result.messageId });
    } catch (error) {
      if (error instanceof RequestError) return json(error.status, { error: error.message });
      console.error(JSON.stringify({ event: "invite_email_failed" }));
      return json(502, { error: "Email delivery failed" });
    }
  },
} satisfies ExportedHandler<Env>;
