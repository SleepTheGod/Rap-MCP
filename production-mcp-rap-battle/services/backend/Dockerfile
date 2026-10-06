FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
COPY packages ./packages
COPY services/backend ./services/backend
RUN npm install
RUN npm run build --workspace=packages/contracts
RUN npm run build --workspace=services/backend

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/package*.json ./
COPY --from=builder /app/packages/contracts/package.json ./packages/contracts/package.json
COPY --from=builder /app/packages/contracts/dist ./packages/contracts/dist
COPY --from=builder /app/services/backend/package.json ./services/backend/package.json
COPY --from=builder /app/services/backend/dist ./services/backend/dist
RUN npm install --omit=dev

EXPOSE 4000
CMD ["node", "services/backend/dist/server.js"]
