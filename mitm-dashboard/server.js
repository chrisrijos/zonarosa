/**
 * ZonaRosa MITM Educational Dashboard — Proxy Server (v2)
 *
 * MIT License — Copyright (c) 2026 ZonaRosa Platform
 *
 * Architecture:
 *
 *   iOS / Android / Desktop
 *         │
 *         ▼  (points to THIS server's LAN IP:3737)
 *   ┌───────────────────────────────┐
 *   │  ZonaRosa MITM Proxy Server   │  ← intercepts all API traffic
 *   │  /v1/messages  /v1/profile …  │
 *   └──────────────┬────────────────┘
 *                  │  forwards to real server (optional)
 *                  ▼
 *        ZonaRosa Backend Server
 *
 * Mobile clients connect by setting:
 *   Android: zonarosa.server.url = http://<LAN_IP>:3737
 *   iOS:     ZonaRosaServerURL    = http://<LAN_IP>:3737
 *
 * No upstream server needed for the demo — the proxy self-mocks.
 */

const express = require("express");
const http = require("http");
const https = require("https");
const WebSocket = require("ws");
const crypto = require("crypto");
const path = require("path");
const os = require("os");
const { URL } = require("url");

const app = express();
const server = http.createServer(app);
const PORT = process.env.PORT || 3737;

// ─── Upstream server (optional — MITM still works without it) ───────────────
// Set ZONAROSA_UPSTREAM env var to forward to the real server, e.g.:
//   ZONAROSA_UPSTREAM=https://chat.zonarosa.io node server.js
const UPSTREAM = process.env.ZONAROSA_UPSTREAM || null;

// ─── State ───────────────────────────────────────────────────────────────────
let mode = "MITM"; // 'MITM' | 'E2E'
const interceptedMessages = [];
const MAX_MESSAGES = 500;

// ─── Middleware ───────────────────────────────────────────────────────────────
app.use(express.static(path.join(__dirname, "public")));
app.use(express.json({ limit: "10mb" }));
app.use(express.raw({ type: "*/*", limit: "10mb" }));

// CORS — mobile clients on the LAN need this
app.use((req, res, next) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader(
    "Access-Control-Allow-Headers",
    "Content-Type,Authorization,X-Signal-Agent,X-ZonaRosa-Agent",
  );
  res.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
  if (req.method === "OPTIONS") return res.sendStatus(200);
  next();
});

// ─── Request logger (API traffic only) ───────────────────────────────────────
app.use((req, res, next) => {
  if (req.path.startsWith("/v1/") || req.path.startsWith("/v2/")) {
    const sender = extractSender(req);
    console.log(`[${mode}] ${req.method} ${req.path} ← ${sender}`);
  }
  next();
});

// ─── ZonaRosa Protocol Proxy Routes ───────────────────────────────────────────

/**
 * PUT /v1/messages/:destination
 * Mobile clients call this to send a message to another user.
 * In MITM mode we decode and display. In E2E mode we show the encrypted blob.
 */
app.put("/v1/messages/:destination", (req, res) => {
  const destination = decodeURIComponent(req.params.destination);
  const sender = extractSender(req);
  const body = req.body;

  const entry = interceptMessage({
    sender,
    recipient: destination,
    rawBody: body,
    path: req.path,
    method: req.method,
  });

  console.log(
    entry.status === "DECRYPTED"
      ? `[MITM] ${entry.sender} → ${entry.recipient}: "${entry.plaintext?.substring(0, 60)}"`
      : `[E2E]  ${entry.sender} → ${entry.recipient}: [encrypted — cannot read]`,
  );

  // If upstream configured, forward the request
  if (UPSTREAM) {
    forwardRequest(req, res, UPSTREAM, entry);
  } else {
    // Mock a successful ZonaRosa server response
    res.json({ needsSync: false, uuids: [] });
  }
});

/**
 * GET /v1/profile/:uuid  — Profile fetch (interceptable)
 */
app.get("/v1/profile/:uuid", (req, res) => {
  logApiCall(req, "PROFILE_FETCH");
  if (UPSTREAM) return forwardRequest(req, res, UPSTREAM);
  // Mock profile
  res.json({
    uuid: req.params.uuid,
    name: `User-${req.params.uuid.substring(0, 6)}`,
    identityKey: crypto.randomBytes(32).toString("base64"),
    unrestrictedUnidentifiedAccess: false,
  });
});

/**
 * GET /v1/keys/:uuid/:deviceId  — Pre-key bundle (interceptable)
 */
app.get("/v1/keys/:uuid/:deviceId", (req, res) => {
  logApiCall(req, "KEY_FETCH");
  if (UPSTREAM) return forwardRequest(req, res, UPSTREAM);
  // Mock pre-key bundle
  res.json({
    identityKey: crypto.randomBytes(33).toString("base64"),
    devices: [
      {
        deviceId: parseInt(req.params.deviceId) || 1,
        registrationId: Math.floor(Math.random() * 16383) + 1,
        preKey: {
          keyId: 1,
          publicKey: crypto.randomBytes(32).toString("base64"),
        },
        signedPreKey: {
          keyId: 1,
          publicKey: crypto.randomBytes(32).toString("base64"),
          signature: crypto.randomBytes(64).toString("base64"),
        },
      },
    ],
  });
});

/**
 * POST /v1/accounts/…  — Account registration (log only)
 */
app.post("/v1/accounts/*", (req, res) => {
  logApiCall(req, "ACCOUNT_OP");
  if (UPSTREAM) return forwardRequest(req, res, UPSTREAM);
  res.json({ uuid: crypto.randomUUID(), storageCapable: true });
});

/**
 * Generic catch-all for other protocol paths
 */
app.all(["/v1/*", "/v2/*"], (req, res) => {
  logApiCall(req, "OTHER");
  if (UPSTREAM) return forwardRequest(req, res, UPSTREAM);
  res.json({ ok: true });
});

// ─── Dashboard REST API ────────────────────────────────────────────────────────
app.get("/api/state", (req, res) => {
  res.json({
    mode,
    messages: interceptedMessages.slice().reverse().slice(0, 100),
  });
});

app.post("/api/mode", (req, res) => {
  const { newMode } = req.body;
  if (!["MITM", "E2E"].includes(newMode))
    return res.status(400).json({ error: "Invalid mode" });
  mode = newMode;
  broadcastToDashboardAll({ type: "MODE_CHANGE", mode });
  console.log(`\n[ZonaRosa] ⇄  Mode switched → ${mode}\n`);
  res.json({ mode });
});

app.post("/api/intercept", (req, res) => {
  // Simulator injection endpoint (keeps backward compat)
  const { sender, recipient, plaintextContent, sessionId } = req.body;
  if (!sender || !recipient || !plaintextContent)
    return res.status(400).json({ error: "Missing fields" });
  const entry = buildEntry({
    sender,
    recipient,
    plaintext: plaintextContent,
    sessionId,
    source: "simulator",
  });
  storeAndBroadcast(entry);
  res.json({ ok: true, entry });
});

app.delete("/api/messages", (req, res) => {
  interceptedMessages.length = 0;
  broadcastToDashboardAll({ type: "CLEAR" });
  res.json({ ok: true });
});

// Expose LAN IP for mobile clients to configure
app.get("/api/lan", (req, res) => {
  res.json({ lanIp: getLanIp(), port: PORT, mode });
});

// ─── SSE — dashboard clients ──────────────────────────────────────────────────
// Server-Sent Events work reliably on Cloud Run without WebSocket timeout issues.
// The browser EventSource API auto-reconnects on drop.
const sseClients = new Set();

app.get("/api/events", (req, res) => {
  res.setHeader("Content-Type", "text/event-stream");
  res.setHeader("Cache-Control", "no-cache");
  res.setHeader("Connection", "keep-alive");
  res.setHeader("X-Accel-Buffering", "no"); // disable nginx buffering
  res.flushHeaders();

  // Send current state on connect
  const init = JSON.stringify({
    type: "INIT",
    mode,
    messages: interceptedMessages.slice().reverse().slice(0, 100),
  });
  res.write(`data: ${init}\n\n`);

  // Heartbeat every 25s keeps Cloud Run connection alive
  const heartbeat = setInterval(() => {
    res.write(`: heartbeat\n\n`);
  }, 25000);

  sseClients.add(res);
  req.on("close", () => {
    clearInterval(heartbeat);
    sseClients.delete(res);
  });
});

function broadcastToDashboard(payload) {
  const data = `data: ${JSON.stringify(payload)}\n\n`;
  for (const client of sseClients) {
    try {
      client.write(data);
    } catch (_) {
      sseClients.delete(client);
    }
  }
}

// ─── WebSocket — dashboard legacy (keep for local use) ────────────────────────
// SSE is now primary. WS /ws/dashboard kept for local dev compatibility.
const wss = new WebSocket.Server({ server, path: "/ws/dashboard" });
const dashboardWsClients = new Set();

wss.on("connection", (ws) => {
  dashboardWsClients.add(ws);
  ws.send(
    JSON.stringify({
      type: "INIT",
      mode,
      messages: interceptedMessages.slice().reverse().slice(0, 100),
    }),
  );
  ws.on("close", () => dashboardWsClients.delete(ws));
});

// Also broadcast to any WS dashboard clients still connected
const _origBroadcast = broadcastToDashboard;
const broadcastToDashboardAll = (payload) => {
  _origBroadcast(payload);
  const data = JSON.stringify(payload);
  for (const client of dashboardWsClients) {
    if (client.readyState === WebSocket.OPEN) client.send(data);
  }
};
// Override so storeAndBroadcast uses the combined version
Object.defineProperty(module.exports || {}, "_broadcast", {
  get: () => broadcastToDashboardAll,
});

// ─── WebSocket — mobile client proxy ─────────────────────────────────────────
// Mobile clients open a WebSocket to /v1/websocket for real-time delivery.
// In MITM mode we decode frames; in E2E mode we show the raw envelope.
const wssMobile = new WebSocket.Server({ server, path: "/v1/websocket" });

wssMobile.on("connection", (ws, req) => {
  const sender = extractSender(req);
  console.log(`[ZonaRosa WS] Mobile client connected: ${sender}`);

  ws.on("message", (data) => {
    // ZonaRosa uses protobuf-encoded WebSocketMessage.
    // We capture the raw frame and present bytes for E2E or try to read for MITM.
    const hex = Buffer.isBuffer(data)
      ? data.toString("hex")
      : Buffer.from(data).toString("hex");
    const entry = buildEntry({
      sender,
      recipient: "SERVER",
      plaintext: mode === "MITM" ? `[WS Frame] ${hex.substring(0, 80)}…` : null,
      ciphertext: hex.toUpperCase(),
      sessionId: req.headers["x-zonarosa-session"] || "ws-session",
      source: "mobile-ws",
    });
    storeAndBroadcast(entry);
  });

  ws.on("close", () => {
    console.log(`[ZonaRosa WS] Mobile client disconnected: ${sender}`);
  });

  // Acknowledge connection (Signal-compatible handshake)
  ws.send(JSON.stringify({ type: "CONNECTED", server: "ZonaRosa MITM Proxy" }));
});

// ─── Core intercept logic ──────────────────────────────────────────────────
function interceptMessage({ sender, recipient, rawBody, path, method }) {
  let plaintext = null;
  let ciphertext = null;

  if (rawBody && typeof rawBody === "object") {
    // Signal PUT /v1/messages body: { messages: [{content: base64EnvelopeBytes}] }
    const msgs = rawBody.messages || rawBody.data?.messages || [];
    const firstMsg = msgs[0] || {};

    // The `content` field is base64-encoded encrypted envelope bytes.
    // In MITM mode we show the decoded representation.
    // In E2E mode we cannot decrypt — the session keys live on devices only.
    if (firstMsg.content) {
      const rawBytes = Buffer.from(firstMsg.content, "base64");
      ciphertext = rawBytes.toString("hex").toUpperCase();
      if (mode === "MITM") {
        // For demo: show structured info about the envelope
        plaintext =
          `[Envelope] type=${firstMsg.type || "UNKNOWN"} ` +
          `destinationDeviceId=${firstMsg.destinationDeviceId || "?"} ` +
          `size=${rawBytes.length}B ` +
          `content=${rawBytes.toString("base64").substring(0, 40)}…`;
      }
    } else if (rawBody.message || rawBody.text || rawBody.body) {
      // Unencrypted/debug message fields (dev builds may send these)
      const text = rawBody.message || rawBody.text || rawBody.body;
      ciphertext = Buffer.from(text).toString("hex").toUpperCase();
      if (mode === "MITM") plaintext = text;
    } else {
      ciphertext = generateFakeCiphertext(JSON.stringify(rawBody));
      if (mode === "MITM")
        plaintext = `[API] ${method} ${path} — ${JSON.stringify(rawBody).substring(0, 80)}`;
    }
  } else {
    ciphertext = generateFakeCiphertext("unknown");
    if (mode === "MITM") plaintext = `[API] ${method} ${path}`;
  }

  return buildEntry({
    sender,
    recipient,
    plaintext,
    ciphertext,
    source: "mobile-http",
  });
}

function buildEntry({
  sender,
  recipient,
  plaintext,
  ciphertext,
  sessionId,
  source,
}) {
  const id = crypto.randomBytes(6).toString("hex");
  const timestamp = new Date().toISOString();

  if (!ciphertext) ciphertext = generateFakeCiphertext(plaintext || "unknown");

  return {
    id,
    sessionId: sessionId || source || "unknown",
    source: source || "unknown",
    sender: formatPerson(sender),
    recipient: formatPerson(recipient),
    timestamp,
    mode,
    status: mode === "MITM" && plaintext ? "DECRYPTED" : "DECRYPT_ERROR",
    plaintext: mode === "MITM" ? plaintext : null,
    ciphertext,
    error:
      mode === "E2E"
        ? "ERR_DECRYPT_FAILED: Double Ratchet session key not available at intercept layer. Message is end-to-end encrypted."
        : null,
    errorCode: mode === "E2E" ? "E2E_PROTECTED" : null,
  };
}

function storeAndBroadcast(entry) {
  interceptedMessages.push(entry);
  if (interceptedMessages.length > MAX_MESSAGES) interceptedMessages.shift();
  broadcastToDashboardAll({ type: "NEW_MESSAGE", message: entry });
}

function logApiCall(req, type) {
  const sender = extractSender(req);
  console.log(`[${mode}] ${type} — ${req.method} ${req.path} from ${sender}`);
}

// ─── Upstream forwarding ──────────────────────────────────────────────────
function forwardRequest(req, res, upstream, _entry) {
  try {
    const target = new URL(req.url, upstream);
    const isHttps = target.protocol === "https:";
    const lib = isHttps ? https : http;
    const options = {
      hostname: target.hostname,
      port: target.port || (isHttps ? 443 : 80),
      path: target.pathname + target.search,
      method: req.method,
      headers: { ...req.headers, host: target.hostname },
    };

    const proxy = lib.request(options, (proxyRes) => {
      res.status(proxyRes.statusCode);
      Object.entries(proxyRes.headers).forEach(([k, v]) => res.setHeader(k, v));
      proxyRes.pipe(res);
    });

    proxy.on("error", () =>
      res.status(502).json({ error: "upstream_unavailable" }),
    );
    if (req.body && Buffer.isBuffer(req.body)) proxy.write(req.body);
    proxy.end();
  } catch (e) {
    res.status(502).json({ error: "upstream_error", detail: e.message });
  }
}

// ─── Helpers ──────────────────────────────────────────────────────────────
function extractSender(req) {
  // ZonaRosa/Signal clients send username in Authorization header as:
  // Basic base64(username:password) or Bearer token
  const auth = req.headers["authorization"] || "";
  if (auth.startsWith("Basic ")) {
    try {
      const decoded = Buffer.from(auth.slice(6), "base64").toString();
      return decoded.split(":")[0] || "mobile-client";
    } catch (_) {}
  }
  return (
    req.headers["x-zonarosa-sender"] ||
    req.headers["x-signal-sender"] ||
    req.socket?.remoteAddress ||
    "mobile-client"
  );
}

function formatPerson(str) {
  if (!str) return "Unknown <unknown>";
  if (str.includes("<")) return str;
  return `${str} <${str}>`;
}

function generateFakeCiphertext(plaintext) {
  const version = "33";
  const mac = crypto
    .createHmac("sha256", "zonarosa-demo")
    .update(String(plaintext))
    .digest("hex")
    .substring(0, 16);
  const iv = crypto.randomBytes(16).toString("hex");
  const body = Buffer.from(String(plaintext))
    .toString("base64")
    .split("")
    .map((c) => c.charCodeAt(0).toString(16).padStart(2, "0"))
    .join("");
  return `${version}${mac}${iv}${body}`.toUpperCase();
}

function getLanIp() {
  const interfaces = os.networkInterfaces();
  for (const iface of Object.values(interfaces)) {
    for (const alias of iface) {
      if (alias.family === "IPv4" && !alias.internal) return alias.address;
    }
  }
  return "127.0.0.1";
}

// ─── Start ────────────────────────────────────────────────────────────────
server.listen(PORT, "0.0.0.0", () => {
  const LAN = getLanIp();
  console.log("");
  console.log("  ┌──────────────────────────────────────────────────────────┐");
  console.log("  │   ZonaRosa MITM Educational Dashboard v2                 │");
  console.log("  ├──────────────────────────────────────────────────────────┤");
  console.log(
    `  │   Dashboard  → http://localhost:${PORT}                       │`,
  );
  console.log(`  │   LAN IP     → http://${LAN}:${PORT}                  │`);
  console.log("  ├──────────────────────────────────────────────────────────┤");
  console.log("  │   Mobile client config:                                  │");
  console.log(`  │     Android  zonarosa.server.url = http://${LAN}:${PORT} │`);
  console.log(`  │     iOS      ZonaRosaServerURL   = http://${LAN}:${PORT} │`);
  console.log("  ├──────────────────────────────────────────────────────────┤");
  console.log("  │   Protocol intercept endpoints:                          │");
  console.log("  │     PUT  /v1/messages/:dest   (outbound messages)        │");
  console.log("  │     GET  /v1/profile/:uuid    (profile lookups)          │");
  console.log("  │     GET  /v1/keys/:uuid/:dev  (pre-key bundles)          │");
  console.log("  │     WS   /v1/websocket        (real-time delivery)       │");
  console.log("  ├──────────────────────────────────────────────────────────┤");
  console.log(
    "  │   Upstream forward: " + (UPSTREAM || "disabled (mock mode)") + "   │",
  );
  console.log("  └──────────────────────────────────────────────────────────┘");
  console.log("");
  console.log(`  Current mode: ${mode}`);
  console.log('  Toggle via:  POST /api/mode  { "newMode": "E2E" }');
  console.log("");
});
