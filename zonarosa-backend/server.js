/**
 * ZonaRosa Relay Backend
 *
 * MIT License — Copyright (c) 2026 ZonaRosa Platform
 *
 * Lightweight Signal-protocol-compatible relay server for educational use.
 * Handles: account registration, pre-key distribution, message queuing,
 * and real-time WebSocket delivery.
 *
 * Deployed as the upstream for the MITM proxy:
 *   ZONAROSA_UPSTREAM=https://zonarosa-backend-xxxx.run.app
 *
 * Architecture:
 *   Mobile Client → MITM Proxy (intercepts) → THIS SERVER → Mobile Client
 */

const express = require("express");
const http = require("http");
const WebSocket = require("ws");
const crypto = require("crypto");
const { v4: uuidv4 } = require("uuid");

const app = express();
const server = http.createServer(app);
const PORT = process.env.PORT || 4000;

// ─── In-memory store (sufficient for classroom demo) ─────────────────────────
// Structure: users[uuid] = { uuid, number, name, deviceId, registrationId,
//                            identityKey, preKeys[], signedPreKey, password }
const users = new Map(); // uuid → user record
const byNumber = new Map(); // e164 → uuid
const byName = new Map(); // username → uuid

// Pending messages per device (if not yet connected via WS)
// messageQueue[uuid] = [{ ...messageEnvelope }]
const messageQueue = new Map();

// Active WebSocket connections per device
// deviceSockets[uuid] = WebSocket
const deviceSockets = new Map();

// ─── Middleware ───────────────────────────────────────────────────────────────
app.use(express.json({ limit: "10mb" }));
app.use(express.raw({ type: "*/*", limit: "10mb" }));

app.use((req, res, next) => {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader(
    "Access-Control-Allow-Headers",
    "Content-Type,Authorization,X-Signal-Agent,X-ZonaRosa-Agent,X-ZonaRosa-Sender",
  );
  res.setHeader(
    "Access-Control-Allow-Methods",
    "GET,POST,PUT,DELETE,PATCH,OPTIONS",
  );
  if (req.method === "OPTIONS") return res.sendStatus(200);
  next();
});

app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

// ─── Auth helper ──────────────────────────────────────────────────────────────
function authenticate(req) {
  const auth = req.headers["authorization"] || "";
  if (auth.startsWith("Basic ")) {
    try {
      const decoded = Buffer.from(auth.slice(6), "base64").toString();
      const [username, password] = decoded.split(":");
      return { username, password };
    } catch (_) {}
  }
  return null;
}

function getCallerUuid(req) {
  const creds = authenticate(req);
  if (!creds) return null;
  return byName.get(creds.username) || null;
}

// ─── Account Registration ─────────────────────────────────────────────────────

// POST /v1/accounts/sms/code/:number  — request verification code
app.post("/v1/accounts/sms/code/:number", (req, res) => {
  const number = decodeURIComponent(req.params.number);
  console.log(`[REG] SMS code requested for ${number}`);
  // In real Signal, this sends an SMS. We just acknowledge.
  res.sendStatus(200);
});

// POST /v1/accounts/code/:code  — verify and register
app.post("/v1/accounts/code/:code", (req, res) => {
  const body = req.body;
  const creds = authenticate(req);
  if (!creds) return res.status(401).json({ error: "Missing credentials" });

  const uuid = uuidv4();
  const user = {
    uuid,
    number: creds.username.includes("+") ? creds.username : `+1${Date.now()}`,
    name: creds.username,
    password: creds.password,
    deviceId: 1,
    registrationId:
      body.registrationId || Math.floor(Math.random() * 16383) + 1,
    identityKey: body.identityKey || crypto.randomBytes(33).toString("base64"),
    preKeys: body.preKeys || [],
    signedPreKey: body.signedPreKey || {
      keyId: 1,
      publicKey: crypto.randomBytes(32).toString("base64"),
      signature: crypto.randomBytes(64).toString("base64"),
    },
    storageCapable: true,
    createdAt: new Date().toISOString(),
  };

  users.set(uuid, user);
  byName.set(creds.username, uuid);
  messageQueue.set(uuid, []);

  console.log(`[REG] Registered: ${creds.username} → uuid ${uuid}`);
  res.json({ uuid, storageCapable: true });
});

// PUT /v1/accounts/keys  — upload pre-keys
app.put("/v1/accounts/keys", (req, res) => {
  const uuid = getCallerUuid(req);
  if (!uuid || !users.has(uuid))
    return res.status(401).json({ error: "Unauthorized" });

  const user = users.get(uuid);
  const body = req.body;

  if (body.preKeys) user.preKeys = body.preKeys;
  if (body.signedPreKey) user.signedPreKey = body.signedPreKey;
  if (body.identityKey) user.identityKey = body.identityKey;
  users.set(uuid, user);

  console.log(
    `[KEYS] Pre-keys uploaded for ${user.name} (${body.preKeys?.length || 0} one-time keys)`,
  );
  res.sendStatus(200);
});

// GET /v1/accounts/whoami
app.get("/v1/accounts/whoami", (req, res) => {
  const uuid = getCallerUuid(req);
  if (!uuid) return res.status(401).json({ error: "Unauthorized" });
  const user = users.get(uuid);
  res.json({ uuid, number: user.number, username: user.name });
});

// DELETE /v1/accounts/me  — unregister
app.delete("/v1/accounts/me", (req, res) => {
  const uuid = getCallerUuid(req);
  if (uuid) {
    const user = users.get(uuid);
    if (user) {
      byName.delete(user.name);
      byNumber.delete(user.number);
    }
    users.delete(uuid);
    messageQueue.delete(uuid);
    deviceSockets.delete(uuid);
  }
  res.sendStatus(200);
});

// ─── Profile ──────────────────────────────────────────────────────────────────

// GET /v1/profile/:identifier  — by uuid or phone number
app.get("/v1/profile/:identifier", (req, res) => {
  const id = decodeURIComponent(req.params.identifier);
  let user = users.get(id) || null;
  if (!user)
    user = Array.from(users.values()).find(
      (u) => u.number === id || u.name === id,
    );
  if (!user) return res.status(404).json({ error: "Not found" });

  res.json({
    uuid: user.uuid,
    name: user.name,
    identityKey: user.identityKey,
    unrestrictedUnidentifiedAccess: false,
    capabilities: { gv2: true, storage: true },
  });
});

// ─── Pre-key distribution ─────────────────────────────────────────────────────

// GET /v1/keys/:identifier/:deviceId
app.get("/v1/keys/:identifier/:deviceId", (req, res) => {
  const id = decodeURIComponent(req.params.identifier);
  let user = users.get(id) || null;
  if (!user)
    user = Array.from(users.values()).find(
      (u) => u.number === id || u.name === id,
    );
  if (!user) return res.status(404).json({ error: "User not registered" });

  // Pop one one-time pre-key (or fall back to signed pre-key)
  const preKey = user.preKeys.shift() || {
    keyId: 1,
    publicKey: crypto.randomBytes(32).toString("base64"),
  };

  res.json({
    identityKey: user.identityKey,
    devices: [
      {
        deviceId: user.deviceId || 1,
        registrationId: user.registrationId,
        preKey,
        signedPreKey: user.signedPreKey || {
          keyId: 1,
          publicKey: crypto.randomBytes(32).toString("base64"),
          signature: crypto.randomBytes(64).toString("base64"),
        },
      },
    ],
  });
});

// ─── Message delivery ─────────────────────────────────────────────────────────

// PUT /v1/messages/:destination  — send sealed message
app.put("/v1/messages/:destination", (req, res) => {
  const dest = decodeURIComponent(req.params.destination);
  const body = req.body;
  const sender = getCallerUuid(req) || "unknown";
  const senderUser = users.get(sender);

  // Find recipient
  let recipUuid = users.has(dest) ? dest : null;
  if (!recipUuid)
    recipUuid =
      Array.from(users.values()).find(
        (u) => u.number === dest || u.name === dest,
      )?.uuid || null;

  if (!recipUuid) {
    console.log(`[MSG] Recipient not found: ${dest}`);
    return res.json({ needsSync: false, uuids: [] });
  }

  const recipUser = users.get(recipUuid);
  const envelopes = (body.messages || []).map((msg) => ({
    id: uuidv4(),
    type: msg.type || 1,
    source: senderUser?.name || sender,
    sourceUuid: sender,
    sourceDevice: msg.sourceDevice || 1,
    destination: dest,
    destinationDeviceId: msg.destinationDeviceId || 1,
    timestamp: Date.now(),
    content: msg.content, // encrypted envelope bytes (base64)
    serverTimestamp: Date.now(),
  }));

  const enqueue = messageQueue.get(recipUuid) || [];
  envelopes.forEach((e) => enqueue.push(e));
  messageQueue.set(recipUuid, enqueue);

  // Push immediately if recipient has a live WebSocket
  const recipWs = deviceSockets.get(recipUuid);
  if (recipWs && recipWs.readyState === WebSocket.OPEN) {
    envelopes.forEach((env) => {
      recipWs.send(JSON.stringify({ type: "ENVELOPE", envelope: env }));
    });
    // Clear queue since we pushed live
    messageQueue.set(recipUuid, []);
    console.log(
      `[MSG] Delivered live to ${recipUser.name} (${envelopes.length} envelope(s))`,
    );
  } else {
    console.log(
      `[MSG] Queued for ${recipUser?.name || recipUuid} (${envelopes.length} envelope(s), offline)`,
    );
  }

  res.json({ needsSync: false, uuids: [] });
});

// ─── WebSocket delivery channel ───────────────────────────────────────────────
// Clients connect here to receive messages in real time.
// Signal protocol: authenticate with Basic auth in the WS handshake.
const wss = new WebSocket.Server({ server, path: "/v1/websocket" });

wss.on("connection", (ws, req) => {
  const auth = req.headers["authorization"] || "";
  let uuid = null;

  if (auth.startsWith("Basic ")) {
    try {
      const decoded = Buffer.from(auth.slice(6), "base64").toString();
      const [username] = decoded.split(":");
      uuid = byName.get(username) || null;
    } catch (_) {}
  }

  if (!uuid) {
    // Try query param (some clients pass Basic auth as query)
    try {
      const url = new URL("http://x" + req.url);
      const u = url.searchParams.get("login");
      if (u) uuid = byName.get(u) || null;
    } catch (_) {}
  }

  const userLabel = uuid ? users.get(uuid)?.name || uuid : "unknown";
  console.log(`[WS] Client connected: ${userLabel}`);

  if (uuid) {
    deviceSockets.set(uuid, ws);

    // Drain any queued messages
    const queued = messageQueue.get(uuid) || [];
    if (queued.length > 0) {
      console.log(
        `[WS] Draining ${queued.length} queued message(s) to ${userLabel}`,
      );
      queued.forEach((env) =>
        ws.send(JSON.stringify({ type: "ENVELOPE", envelope: env })),
      );
      messageQueue.set(uuid, []);
    }

    // Send connected acknowledgement (Signal-compatible)
    ws.send(
      JSON.stringify({
        type: "CONNECTED",
        id: uuid,
        server: "ZonaRosa Backend",
      }),
    );
  }

  ws.on("message", (data) => {
    // Clients may send WebSocketRequestMessage (acks, sync requests).
    // For the demo we just log and ack.
    try {
      const msg = JSON.parse(data.toString());
      if (msg.type === "REQUEST") {
        ws.send(JSON.stringify({ type: "RESPONSE", id: msg.id, status: 200 }));
      }
    } catch (_) {}
  });

  ws.on("close", () => {
    if (uuid) deviceSockets.delete(uuid);
    console.log(`[WS] Client disconnected: ${userLabel}`);
  });

  // Heartbeat
  const hb = setInterval(() => {
    if (ws.readyState === WebSocket.OPEN) ws.ping();
  }, 30000);
  ws.on("close", () => clearInterval(hb));
});

// ─── Admin / health endpoints ─────────────────────────────────────────────────

// GET /  — health check
app.get("/", (req, res) => {
  res.json({
    service: "ZonaRosa Backend",
    version: "1.0.0",
    status: "ok",
    users: users.size,
    online: deviceSockets.size,
    queued: Array.from(messageQueue.values()).reduce((s, a) => s + a.length, 0),
  });
});

// GET /admin/users  — list registered users (instructor view)
app.get("/admin/users", (req, res) => {
  const list = Array.from(users.values()).map((u) => ({
    uuid: u.uuid,
    name: u.name,
    number: u.number,
    online: deviceSockets.has(u.uuid),
    queued: (messageQueue.get(u.uuid) || []).length,
    createdAt: u.createdAt,
  }));
  res.json({ users: list });
});

// ─── Start ────────────────────────────────────────────────────────────────────
server.listen(PORT, "0.0.0.0", () => {
  console.log("");
  console.log("  ┌──────────────────────────────────────────────────────────┐");
  console.log("  │   ZonaRosa Backend Relay v1.0                            │");
  console.log("  ├──────────────────────────────────────────────────────────┤");
  console.log(
    `  │   Listening on port ${PORT}                                  │`,
  );
  console.log("  ├──────────────────────────────────────────────────────────┤");
  console.log(
    "  │   Endpoints:                                              │",
  );
  console.log(
    "  │     POST /v1/accounts/code/:code   register               │",
  );
  console.log(
    "  │     PUT  /v1/accounts/keys         upload pre-keys        │",
  );
  console.log(
    "  │     GET  /v1/profile/:id           fetch profile          │",
  );
  console.log(
    "  │     GET  /v1/keys/:id/:dev         fetch pre-key bundle   │",
  );
  console.log(
    "  │     PUT  /v1/messages/:dest        send encrypted message │",
  );
  console.log(
    "  │     WS   /v1/websocket             real-time delivery     │",
  );
  console.log(
    "  │     GET  /admin/users              registered users       │",
  );
  console.log("  └──────────────────────────────────────────────────────────┘");
  console.log("");
});
