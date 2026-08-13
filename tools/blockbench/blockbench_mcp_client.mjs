const MCP_URL = "http://127.0.0.1:2228/bb-mcp";
const PROTOCOL_VERSION = "2025-03-26";

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
    return;
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) {
    throw new Error(`Blockbench MCP ${method} returned unexpected content type: ${contentType || "missing"}`);
  }

  const payload = await readJson(response, method);
  if (payload.error) {
    throw new Error(`Blockbench MCP ${method} JSON-RPC error: ${formatJsonRpcError(payload.error)}`);
  }
}

async function postJson(payload, sessionId) {
  const headers = {
    Accept: "application/json, text/event-stream",
    "Content-Type": "application/json",
  };
  if (sessionId) {
    headers["mcp-session-id"] = sessionId;
  }

  let response;
  try {
    response = await fetch(MCP_URL, {
      method: "POST",
      headers,
      body: JSON.stringify({ jsonrpc: "2.0", ...payload }),
    });
  } catch (error) {
    throw new Error(`Could not reach Blockbench MCP at ${MCP_URL}: ${error.message}`);
  }

  if (!response.ok) {
    throw new Error(`Blockbench MCP HTTP ${response.status} ${response.statusText || "request failed"}`);
  }
  return response;
}

async function parseJsonRpcResponse(response, expectedId, method) {
  const payload = await readJson(response, method);
  if (payload.jsonrpc !== "2.0") {
    throw new Error(`Blockbench MCP ${method} returned an invalid JSON-RPC version`);
  }
  if (payload.id !== expectedId) {
    throw new Error(`Blockbench MCP ${method} returned unexpected JSON-RPC id`);
  }
  if (payload.error) {
    throw new Error(`Blockbench MCP ${method} JSON-RPC error: ${formatJsonRpcError(payload.error)}`);
  }
  if (!("result" in payload)) {
    throw new Error(`Blockbench MCP ${method} response did not include result`);
  }
  return payload.result;
}

async function readJson(response, method) {
  try {
    return await response.json();
  } catch (error) {
    throw new Error(`Blockbench MCP ${method} returned invalid JSON: ${error.message}`);
  }
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

if (import.meta.url === `file://${process.argv[1]}`) {
  const [command] = process.argv.slice(2);
  if (command !== "health") {
    console.error("Usage: node tools/blockbench/blockbench_mcp_client.mjs health");
    process.exitCode = 1;
  } else {
    runHealthCheck().catch((error) => {
      console.error(`Blockbench MCP health check failed: ${error.message}`);
      process.exitCode = 1;
    });
  }
}
