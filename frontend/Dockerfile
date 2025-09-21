# ---- Build stage ----
FROM node:20-alpine AS builder
WORKDIR /app

# Install deps first (leverage Docker layer cache)
COPY package*.json ./
# Use npm ci when lockfile exists, fall back to npm install
RUN if [ -f package-lock.json ]; then npm ci; else npm install; fi

# Copy source and build
COPY . .
RUN npm run build

# ---- Runtime stage ----
FROM nginx:1.27-alpine AS runtime

# Copy custom nginx config (SPA routing + caching)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Static files
COPY --from=builder /app/dist /usr/share/nginx/html

# Non-root (optional): nginx image already uses nginx user in some variants; keeping default for simplicity
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
