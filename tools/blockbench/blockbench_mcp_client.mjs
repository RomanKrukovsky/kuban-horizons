const MCP_URL = "http://127.0.0.1:2228/bb-mcp";
const PROTOCOL_VERSION = "2025-03-26";
const REQUEST_TIMEOUT_MS = 10_000;
const responseTimeouts = new WeakMap();

/**
 * Клиент Streamable HTTP для локального Blockbench MCP.
 */
export class BlockbenchClient {
  #sessionId;
  #nextRequestId;

  constructor(sessionId, serverInfo) {
    this.#sessionId = sessionId;
    this.serverInfo = serverInfo;
    this.#nextRequestId = 2;
  }

  /**
   * Вызывает инструмент MCP и возвращает его result.
   *
   * @param {string} name
   * @param {object} args
   * @returns {Promise<object>}
   */
  async callTool(name, args) {
    if (typeof name !== "string" || name.length === 0) {
      throw new Error("MCP tool name must be a non-empty string");
    }
    if (args === null || typeof args !== "object" || Array.isArray(args)) {
      throw new Error("MCP tool arguments must be an object");
    }

    const result = await sendRequest({
      id: this.#nextRequestId++,
      method: "tools/call",
      params: { name, arguments: args },
    }, this.#sessionId);

    if (result.isError === true) {
      throw new Error(`Blockbench MCP tool ${name} failed: ${formatResultError(result)}`);
    }
    return result;
  }
}

/**
 * Открывает MCP-сессию с локальным Blockbench.
 *
 * @returns {Promise<BlockbenchClient>}
 */
export async function connectBlockbench() {
  const response = await postJson({
    id: 1,
    method: "initialize",
    params: {
      protocolVersion: PROTOCOL_VERSION,
      capabilities: {},
      clientInfo: {
        name: "kuban-horizons-blockbench-client",
        version: "1.0.0",
      },
    },
  });

  const sessionId = response.headers.get("mcp-session-id");
  if (!sessionId) {
    releaseResponseTimeout(response);
    throw new Error("Blockbench MCP initialize response did not include mcp-session-id");
  }

  const initializeResult = await parseJsonRpcResponse(response, 1, "initialize");
  if (initializeResult.protocolVersion !== PROTOCOL_VERSION) {
    throw new Error(
      `Blockbench MCP selected unsupported protocol version: ${String(initializeResult.protocolVersion)}`,
    );
  }
  if (!initializeResult.serverInfo || typeof initializeResult.serverInfo !== "object") {
    throw new Error("Blockbench MCP initialize response did not include serverInfo");
  }

  await sendNotification("notifications/initialized", {}, sessionId);
  return new BlockbenchClient(sessionId, initializeResult.serverInfo);
}

async function sendRequest(request, sessionId) {
  const response = await postJson(request, sessionId);
  return parseJsonRpcResponse(response, request.id, request.method);
}

async function sendNotification(method, params, sessionId) {
  const response = await postJson({ method, params }, sessionId);
  if (response.status === 202 || response.status === 204 || response.headers.get("content-length") === "0") {
    releaseResponseTimeout(response);
    return;
  }

  const messages = await readJsonRpcMessages(response, method, true);
  for (const message of messages) {
    if (message.error) {
      throw new Error(`Blockbench MCP ${method} JSON-RPC error: ${formatJsonRpcError(message.error)}`);
    }
  }
}

async function postJson(payload, sessionId, options = {}) {
  const timeoutMs = options.timeoutMs ?? REQUEST_TIMEOUT_MS;
  const fetchImpl = options.fetchImpl ?? fetch;
  const headers = {
    Accept: "application/json, text/event-stream",
    "Content-Type": "application/json",
  };
  if (sessionId) {
    headers["mcp-session-id"] = sessionId;
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  let response;
  try {
    response = await fetchImpl(MCP_URL, {
      method: "POST",
      headers,
      body: JSON.stringify({ jsonrpc: "2.0", ...payload }),
      signal: controller.signal,
    });
  } catch (error) {
    clearTimeout(timeout);
    if (controller.signal.aborted) {
      throw requestTimeoutError(timeoutMs);
    }
    throw new Error(`Could not reach Blockbench MCP at ${MCP_URL}: ${error.message}`);
  }

  if (!response.ok) {
    clearTimeout(timeout);
    throw new Error(`Blockbench MCP HTTP ${response.status} ${response.statusText || "request failed"}`);
  }
  responseTimeouts.set(response, { controller, timeout, timeoutMs });
  return response;
}

async function parseJsonRpcResponse(response, expectedId, method) {
  const messages = await readJsonRpcMessages(response, method);
  const payload = messages.find((message) => message.id === expectedId);
  if (!payload) {
    throw new Error(`Blockbench MCP ${method} returned unexpected JSON-RPC id`);
  }
  if (payload.jsonrpc !== "2.0") {
    throw new Error(`Blockbench MCP ${method} returned an invalid JSON-RPC version`);
  }
  if (payload.error) {
    throw new Error(`Blockbench MCP ${method} JSON-RPC error: ${formatJsonRpcError(payload.error)}`);
  }
  if (!("result" in payload)) {
    throw new Error(`Blockbench MCP ${method} response did not include result`);
  }
  return payload.result;
}

async function readJsonRpcMessages(response, method, allowEmpty = false) {
  const contentType = response.headers.get("content-type")?.toLowerCase() ?? "";
  const body = await readResponseBody(response, method);
  if (body.trim().length === 0 && allowEmpty) {
    return [];
  }

  if (contentType.includes("application/json")) {
    return normalizeJsonRpcMessages(parseJson(body, method), method);
  }
  if (contentType.includes("text/event-stream")) {
    return parseEventStream(body, method);
  }
  throw new Error(
    `Blockbench MCP ${method} returned unexpected content type: ${contentType || "missing"}`,
  );
}

async function readResponseBody(response, method) {
  const timeoutState = responseTimeouts.get(response);
  try {
    return await response.text();
  } catch (error) {
    if (timeoutState?.controller.signal.aborted) {
      throw requestTimeoutError(timeoutState.timeoutMs);
    }
    throw new Error(`Blockbench MCP ${method} response body failed: ${error.message}`);
  } finally {
    releaseResponseTimeout(response);
  }
}

function parseEventStream(body, method) {
  const messages = [];
  let dataLines = [];

  const finishEvent = () => {
    if (dataLines.length === 0) {
      return;
    }
    const eventData = dataLines.join("\n");
    dataLines = [];
    messages.push(...normalizeJsonRpcMessages(parseJson(eventData, method), method));
  };

  for (const line of body.replaceAll("\r\n", "\n").replaceAll("\r", "\n").split("\n")) {
    if (line.length === 0) {
      finishEvent();
      continue;
    }
    if (line.startsWith(":")) {
      continue;
    }

    const separator = line.indexOf(":");
    const field = separator === -1 ? line : line.slice(0, separator);
    let value = separator === -1 ? "" : line.slice(separator + 1);
    if (value.startsWith(" ")) {
      value = value.slice(1);
    }
    if (field === "data") {
      dataLines.push(value);
    }
  }
  finishEvent();

  if (messages.length === 0) {
    throw new Error(`Blockbench MCP ${method} returned an empty SSE response`);
  }
  return messages;
}

function parseJson(text, method) {
  try {
    return JSON.parse(text);
  } catch (error) {
    throw new Error(`Blockbench MCP ${method} returned invalid JSON: ${error.message}`);
  }
}

function normalizeJsonRpcMessages(payload, method) {
  const messages = Array.isArray(payload) ? payload : [payload];
  if (messages.length === 0 || messages.some((message) => message === null || typeof message !== "object")) {
    throw new Error(`Blockbench MCP ${method} returned an invalid JSON-RPC payload`);
  }
  return messages;
}

function releaseResponseTimeout(response) {
  const timeoutState = responseTimeouts.get(response);
  if (timeoutState) {
    clearTimeout(timeoutState.timeout);
    responseTimeouts.delete(response);
  }
}

function requestTimeoutError(timeoutMs) {
  return new Error(`Blockbench MCP request timed out after ${timeoutMs} ms`);
}

function formatJsonRpcError(error) {
  return `${error.code ?? "unknown"}: ${error.message ?? "unknown error"}`;
}

function formatResultError(result) {
  if (typeof result.error === "string") {
    return result.error;
  }
  if (Array.isArray(result.content)) {
    return result.content
      .filter((item) => item && item.type === "text" && typeof item.text === "string")
      .map((item) => item.text)
      .join(" ") || "tool returned isError=true";
  }
  return "tool returned isError=true";
}

async function runHealthCheck() {
  const client = await connectBlockbench();
  const { name, version } = client.serverInfo;
  if (typeof name !== "string" || typeof version !== "string") {
    throw new Error("Blockbench MCP serverInfo is missing name or version");
  }
  console.log(`${name} ${version}`);
  console.log("ready=true");
}

async function runSelfTests() {
  const jsonMessages = await readJsonRpcMessages(new Response(
    JSON.stringify({ jsonrpc: "2.0", id: 7, result: { transport: "json" } }),
    { headers: { "content-type": "application/json" } },
  ), "self-test JSON");
  assertEqual(jsonMessages[0].result.transport, "json", "JSON response parsing");

  const sseMessages = await readJsonRpcMessages(new Response(
    "event: message\ndata: {\"jsonrpc\":\"2.0\",\"id\":8,\"result\":{\"transport\":\"sse\"}}\n\n",
    { headers: { "content-type": "text/event-stream" } },
  ), "self-test SSE");
  assertEqual(sseMessages[0].result.transport, "sse", "SSE response parsing");

  const startedAt = Date.now();
  await assertRejects(
    () => postJson({ id: 9, method: "ping" }, undefined, {
      timeoutMs: 20,
      fetchImpl: (_url, options) => new Promise((_resolve, reject) => {
        options.signal.addEventListener("abort", () => reject(options.signal.reason), { once: true });
      }),
    }),
    "timed out after 20 ms",
    "request timeout",
  );
  if (Date.now() - startedAt > 500) {
    throw new Error("Self-test request timeout did not fail fast");
  }

  console.log("self-test: JSON parsing passed");
  console.log("self-test: SSE parsing passed");
  console.log("self-test: request timeout passed");
}

function assertEqual(actual, expected, testName) {
  if (actual !== expected) {
    throw new Error(`${testName} failed: expected ${expected}, received ${actual}`);
  }
}

async function assertRejects(operation, expectedMessage, testName) {
  try {
    await operation();
  } catch (error) {
    if (error.message.includes(expectedMessage)) {
      return;
    }
    throw new Error(`${testName} failed with unexpected error: ${error.message}`);
  }
  throw new Error(`${testName} failed: operation resolved successfully`);
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const [command] = process.argv.slice(2);
  if (command !== "health" && command !== "self-test") {
    console.error("Usage: node tools/blockbench/blockbench_mcp_client.mjs <health|self-test>");
    process.exitCode = 1;
  } else {
    const operation = command === "health" ? runHealthCheck : runSelfTests;
    operation().catch((error) => {
      console.error(`Blockbench MCP ${command} failed: ${error.message}`);
      process.exitCode = 1;
    });
  }
}
