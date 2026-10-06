FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
COPY packages ./packages
COPY services/frontend ./services/frontend
RUN npm install
RUN npm run build --workspace=packages/contracts
RUN npm run build --workspace=services/frontend

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/package*.json ./
COPY --from=builder /app/services/frontend/.next ./services/frontend/.next
COPY --from=builder /app/services/frontend/public ./services/frontend/public
COPY --from=builder /app/services/frontend/package.json ./services/frontend/package.json
RUN npm install --omit=dev

EXPOSE 3000
CMD ["npm", "run", "start", "--workspace=services/frontend"]
