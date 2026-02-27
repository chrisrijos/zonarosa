# ── ZonaRosa MITM Dashboard — Dockerfile ──────────────────────
# Optimised for Cloud Run free tier:
#   - node:20-alpine keeps image small (~50MB)
#   - npm ci --only=production skips devDeps
#   - Non-root user for security

FROM node:20-alpine

# Create non-root user
RUN addgroup -S zonarosa && adduser -S zonarosa -G zonarosa

WORKDIR /app

# Install deps first (layer cache)
COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force

# Copy app source
COPY server.js   ./
COPY public/     ./public/
COPY simulator.js ./

# Own files
RUN chown -R zonarosa:zonarosa /app
USER zonarosa

# Cloud Run injects PORT env var (default 8080)
ENV NODE_ENV=production
EXPOSE 8080

CMD ["node", "server.js"]
