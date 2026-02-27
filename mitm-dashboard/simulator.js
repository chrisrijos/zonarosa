/**
 * ZonaRosa MITM Dashboard — Message Simulator
 *
 * Simulates a realistic multi-client ZonaRosa session with
 * randomized messages sent between fictional accounts.
 * Sends messages to the proxy server so they appear on the dashboard.
 *
 * Usage: node simulator.js
 */

const http = require("http");

const SERVER_HOST = "localhost";
const SERVER_PORT = 3737;

// Fictional student accounts
const ACCOUNTS = [
  { id: "alice.zonarosa", name: "Alice", device: "iOS" },
  { id: "bob.zonarosa", name: "Bob", device: "Android" },
  { id: "carol.zonarosa", name: "Carol", device: "Desktop" },
  { id: "david.zonarosa", name: "David", device: "Android" },
  { id: "eve.zonarosa", name: "Eve", device: "iOS" },
  { id: "frank.zonarosa", name: "Frank", device: "Desktop" },
];

const MESSAGES = [
  "Hey, are you there?",
  "Did you finish the lab assignment?",
  "The lecture was really good today",
  "Can you share the meeting notes?",
  "I'll be 5 minutes late, save me a seat",
  "What time does the session start?",
  "Got it, thanks!",
  "Make sure you don't share this with anyone 🔒",
  "The API key is: ZR-2049-XKCD-9999 — keep it secret!",
  "My password for the dev server is: zonarosa@devtest2026",
  "Meet me at the library at 3pm",
  "Did you see the vulnerability report?",
  "This message should be private if E2E works correctly",
  "Testing encryption — can the server see this?",
  "Let's schedule a call tonight",
  "Have you reviewed the pull request?",
  "Don't let MITM see this! 😅",
  "Sending a file attachment now...",
  "Voice message recorded — 0:23",
  "Love the new ZonaRosa branding btw",
];

// Generate a session ID
const SESSION_ID = `sim-${Date.now().toString(36)}`;

function randomFrom(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function sendIntercept(sender, recipient, message) {
  const body = JSON.stringify({
    sender: `${sender.name} <${sender.id}>`,
    recipient: `${recipient.name} <${recipient.id}>`,
    plaintextContent: message,
    sessionId: SESSION_ID,
    senderDevice: sender.device,
    recipientDevice: recipient.device,
  });

  const options = {
    hostname: SERVER_HOST,
    port: SERVER_PORT,
    path: "/api/intercept",
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Content-Length": Buffer.byteLength(body),
    },
  };

  const req = http.request(options, (res) => {
    if (res.statusCode !== 200) {
      console.error(`[Simulator] Server returned ${res.statusCode}`);
    }
  });

  req.on("error", (e) => {
    console.error(`[Simulator] Could not reach server: ${e.message}`);
    console.error("  → Make sure the dashboard is running: npm start");
    process.exit(1);
  });

  req.write(body);
  req.end();
}

// ─── Main loop: send messages at random intervals ────────────────────────────
let count = 0;
console.log("[ZonaRosa Simulator] Starting — Ctrl+C to stop");
console.log(`[ZonaRosa Simulator] Session: ${SESSION_ID}`);
console.log("[ZonaRosa Simulator] Dashboard: http://localhost:3737\n");

function scheduleNext() {
  const delay = 800 + Math.random() * 2200; // 0.8–3s between messages
  setTimeout(() => {
    const accounts = [...ACCOUNTS];
    const sender = randomFrom(accounts);
    const others = accounts.filter((a) => a.id !== sender.id);
    const recipient = randomFrom(others);
    const message = randomFrom(MESSAGES);

    count++;
    console.log(
      `[${count}] ${sender.name} → ${recipient.name}: "${message.substring(0, 50)}"`,
    );
    sendIntercept(sender, recipient, message);
    scheduleNext();
  }, delay);
}

scheduleNext();
