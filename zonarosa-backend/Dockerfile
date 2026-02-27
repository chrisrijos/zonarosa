FROM node:20-alpine

RUN addgroup -S zonarosa && adduser -S zonarosa -G zonarosa

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production && npm cache clean --force

COPY server.js ./

RUN chown -R zonarosa:zonarosa /app
USER zonarosa

ENV NODE_ENV=production
EXPOSE 8080

CMD ["node", "server.js"]
