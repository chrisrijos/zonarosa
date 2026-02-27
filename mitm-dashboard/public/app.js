/**
 * ZonaRosa MITM Dashboard — Frontend Logic
 * Connects via WebSocket, renders messages, handles mode toggle.
 */

/* ── State ──────────────────────────────────────────────── */
let mode = "MITM";
let messages = [];
let totalDecrypted = 0;
let totalEncrypted = 0;
const sessions = new Set();
let ws;

/* ── DOM refs ───────────────────────────────────────────── */
const $ = (id) => document.getElementById(id);
const msgBody = $("msgBody");
const emptyState = $("emptyState");
const modeCheckbox = $("modeCheckbox");
const modeLabel = $("modeLabel");
const modeBanner = $("modeBanner");
const connStatus = $("connStatus");
const connText = $("connText");
const expTitle = $("expTitle");
const expDesc = $("expDesc");
const pathMITM = $("pathMITM");
const pathE2E = $("pathE2E");
const explainerIcon = document.querySelector(".exp-icon");

const statTotal = $("statTotal");
const statDecrypted = $("statDecrypted");
const statEncrypted = $("statEncrypted");
const statSessions = $("statSessions");

const cipherDrawer = $("cipherDrawer");
const drawerOverlay = $("drawerOverlay");
const drawerClose = $("drawerClose");
const drawerCiphertext = $("drawerCiphertext");
const drawerMeta = $("drawerMeta");

/* ── SSE connection (primary — works on Cloud Run) ──────── */
let es;

function connect() {
  if (es) {
    es.close();
  }

  es = new EventSource("/api/events");

  es.onopen = () => {
    connStatus.classList.add("connected");
    connText.textContent = "Connected";
  };

  es.onerror = () => {
    connStatus.classList.remove("connected");
    connText.textContent = "Reconnecting…";
    // EventSource auto-reconnects — no manual setTimeout needed
  };

  es.onmessage = (e) => {
    const payload = JSON.parse(e.data);
    switch (payload.type) {
      case "INIT":
        mode = payload.mode;
        applyMode(mode, false);
        messages = payload.messages || [];
        renderAllMessages();
        break;
      case "NEW_MESSAGE":
        prependMessage(payload.message);
        break;
      case "MODE_CHANGE":
        mode = payload.mode;
        applyMode(mode, true);
        break;
      case "CLEAR":
        clearMessages();
        break;
    }
  };
}

/* ── Mode toggle ────────────────────────────────────────── */
modeCheckbox.addEventListener("change", () => {
  const newMode = modeCheckbox.checked ? "E2E" : "MITM";
  fetch("/api/mode", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ newMode }),
  }).catch(console.error);
});

function applyMode(m, animate) {
  mode = m;
  const isE2E = m === "E2E";

  modeCheckbox.checked = isE2E;

  document.body.classList.toggle("e2e-active", isE2E);
  document.body.classList.toggle("mitm-active", !isE2E);

  modeLabel.textContent = isE2E ? "ENCRYPTING (E2E)" : "INTERCEPTING";
  modeLabel.classList.toggle("e2e-mode", isE2E);

  if (animate) {
    modeBanner.textContent = isE2E
      ? "🔒 E2E Mode Activated — Messages are end-to-end encrypted. Dashboard cannot decrypt."
      : "🔓 MITM Mode Activated — Bastion intercept layer active. All messages visible in plaintext.";
    modeBanner.className = "mode-banner visible" + (isE2E ? " e2e-mode" : "");
    setTimeout(() => (modeBanner.className = "mode-banner"), 4000);
  } else {
    modeBanner.className = "mode-banner";
  }

  // Explainer
  if (isE2E) {
    expTitle.textContent = "E2E Mode — Encrypted";
    expDesc.textContent =
      "End-to-end encryption is active. The Double Ratchet session keys exist only on sender and recipient devices. The ZonaRosa server (and this intercept proxy) sees only encrypted ciphertext — decryption is impossible without the private keys.";
    explainerIcon.textContent = "🔒";
    pathE2E.classList.add("active");
    pathMITM.classList.remove("active");
  } else {
    expTitle.textContent = "MITM Mode Active";
    expDesc.textContent =
      "The ZonaRosa bastion proxy is sitting between clients and the server. The Double Ratchet session key is not established end-to-end — messages pass through this layer in plaintext. An attacker (or instructor) can read every message below.";
    explainerIcon.textContent = "🔓";
    pathMITM.classList.add("active");
    pathE2E.classList.remove("active");
  }
}

/* ── Message rendering ──────────────────────────────────── */
function prependMessage(msg) {
  messages.unshift(msg);
  if (msg.sessionId) sessions.add(msg.sessionId);

  if (msg.status === "DECRYPTED") totalDecrypted++;
  else totalEncrypted++;

  updateStats();

  const row = buildRow(msg, messages.length);
  if (msgBody.firstChild) {
    msgBody.insertBefore(row, msgBody.firstChild);
  } else {
    msgBody.appendChild(row);
  }
  // Re-number visible rows
  renumberRows();
  updateEmpty();
}

function renderAllMessages() {
  msgBody.innerHTML = "";
  totalDecrypted = 0;
  totalEncrypted = 0;
  sessions.clear();

  messages.forEach((msg) => {
    if (msg.sessionId) sessions.add(msg.sessionId);
    if (msg.status === "DECRYPTED") totalDecrypted++;
    else totalEncrypted++;
    msgBody.appendChild(buildRow(msg, 0));
  });

  renumberRows();
  updateStats();
  updateEmpty();
}

function buildRow(msg, fallbackIdx) {
  const isDecrypted = msg.status === "DECRYPTED";
  const [senderName, senderId] = parsePerson(msg.sender);
  const [recipName, recipId] = parsePerson(msg.recipient);

  const row = document.createElement("tr");
  row.className = "msg-row";
  row.dataset.id = msg.id;
  row.dataset.ciphertext = msg.ciphertext || "";
  row.dataset.sender = msg.sender;
  row.dataset.recipient = msg.recipient;
  row.dataset.timestamp = msg.timestamp;
  row.dataset.status = msg.status;

  const ts = formatTimestamp(msg.timestamp);

  const statusBadge = isDecrypted
    ? `<span class="status-badge decrypted"><span class="status-dot"></span>Decrypted</span>`
    : `<span class="status-badge error"><span class="status-dot"></span>E2E Error</span>`;

  let contentCell;
  if (isDecrypted) {
    contentCell = `<div class="content-plaintext">${escapeHtml(msg.plaintext)}</div>`;
  } else {
    const cipherSnip = (msg.ciphertext || "").substring(0, 28) + "…";
    contentCell = `
      <div class="content-error">
        <span class="error-icon">🔐</span>
        <span class="error-msg">${escapeHtml(msg.error || "ERR_DECRYPT_FAILED")}</span>
        <span class="cipher-peek" data-id="${msg.id}">${cipherSnip}</span>
      </div>`;
  }

  row.innerHTML = `
    <td class="td-idx">—</td>
    <td class="td-time">${ts}</td>
    <td class="td-sender">
      <span class="account-name">${escapeHtml(senderName)}</span>
      <span class="account-id">${escapeHtml(senderId)}</span>
    </td>
    <td class="td-recipient">
      <span class="account-name">${escapeHtml(recipName)}</span>
      <span class="account-id">${escapeHtml(recipId)}</span>
    </td>
    <td>${statusBadge}</td>
    <td class="td-content">${contentCell}</td>
  `;

  // Click row → open drawer for ciphertext
  row.addEventListener("click", (e) => {
    if (e.target.classList.contains("cipher-peek")) return; // handled separately
    openDrawer(msg);
  });

  // Cipher peek button
  row.querySelector(".cipher-peek")?.addEventListener("click", (e) => {
    e.stopPropagation();
    openDrawer(msg);
  });

  return row;
}

function renumberRows() {
  const rows = msgBody.querySelectorAll("tr.msg-row .td-idx");
  rows.forEach((td, i) => {
    td.textContent = i + 1;
  });
}

/* ── Ciphertext drawer ──────────────────────────────────── */
function openDrawer(msg) {
  const [senderName] = parsePerson(msg.sender);
  const [recipName] = parsePerson(msg.recipient);
  const ts = formatTimestamp(msg.timestamp);

  drawerMeta.innerHTML = `
    <span><span class="dm-key">SENDER</span>      <span class="dm-val">${escapeHtml(msg.sender)}</span></span>
    <span><span class="dm-key">RECIPIENT</span>   <span class="dm-val">${escapeHtml(msg.recipient)}</span></span>
    <span><span class="dm-key">TIMESTAMP</span>   <span class="dm-val">${ts}</span></span>
    <span><span class="dm-key">MODE</span>        <span class="dm-val">${msg.mode}</span></span>
    <span><span class="dm-key">STATUS</span>      <span class="dm-val">${msg.status}</span></span>
    ${
      msg.status === "DECRYPTED"
        ? `<span><span class="dm-key">PLAINTEXT</span>  <span class="dm-val">"${escapeHtml(msg.plaintext)}"</span></span>`
        : `<span><span class="dm-key">ERROR</span>      <span class="dm-val" style="color:var(--e2e)">${escapeHtml(msg.error)}</span></span>`
    }
  `;

  // Format ciphertext with line breaks every 64 chars
  const raw = msg.ciphertext || "";
  const formatted = raw.match(/.{1,64}/g)?.join("\n") || raw;
  drawerCiphertext.textContent = formatted;

  cipherDrawer.classList.add("open");
  drawerOverlay.classList.add("visible");
}

drawerClose.addEventListener("click", closeDrawer);
drawerOverlay.addEventListener("click", closeDrawer);

function closeDrawer() {
  cipherDrawer.classList.remove("open");
  drawerOverlay.classList.remove("visible");
}

/* ── Clear ──────────────────────────────────────────────── */
$("clearBtn").addEventListener("click", () => {
  fetch("/api/messages", { method: "DELETE" }).catch(console.error);
});

function clearMessages() {
  messages = [];
  totalDecrypted = 0;
  totalEncrypted = 0;
  sessions.clear();
  msgBody.innerHTML = "";
  updateStats();
  updateEmpty();
}

/* ── Stats ──────────────────────────────────────────────── */
function updateStats() {
  statTotal.textContent = messages.length;
  statDecrypted.textContent = totalDecrypted;
  statEncrypted.textContent = totalEncrypted;
  statSessions.textContent = sessions.size;
}

function updateEmpty() {
  emptyState.classList.toggle("visible", messages.length === 0);
}

/* ── Helpers ────────────────────────────────────────────── */
function parsePerson(str) {
  // "Alice <alice.zonarosa>"  →  ['Alice', 'alice.zonarosa']
  const m = (str || "").match(/^(.+?)\s*<(.+?)>$/);
  return m ? [m[1].trim(), m[2].trim()] : [str || "?", ""];
}

function formatTimestamp(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, "0")}`;
}

function escapeHtml(s) {
  return String(s || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/* ── Init ───────────────────────────────────────────────── */
document.body.classList.add("mitm-active");
connect();
