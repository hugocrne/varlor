# ------------------------------------------------------------
# 🏗️  BUILD STAGE
# ------------------------------------------------------------
FROM node:20-alpine AS build

# Set working directory
WORKDIR /app

# Copy package files
COPY web-client/package*.json ./
COPY web-client/tsconfig*.json ./
COPY web-client/next.config.mjs ./
COPY web-client/tailwind.config.ts ./

# Install dependencies
RUN npm ci && npm cache clean --force

# Copy source code
COPY web-client/src ./src
COPY web-client/public ./public
COPY web-client/app ./app

# Build the application (production build)
RUN npm run build

# ------------------------------------------------------------
# 🚀  RUNTIME STAGE (Node.js)
# ------------------------------------------------------------
FROM node:20-alpine AS runtime

# Set working directory
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S nextjs
RUN adduser -S nextjs -u 1001 -G nextjs

# Copy package files and install only production dependencies
COPY web-client/package*.json ./
RUN npm ci --only=production && npm cache clean --force

# Copy built application from build stage
COPY --from=build --chown=nextjs:nextjs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nextjs /app/.next/static ./.next/static
COPY --from=build --chown=nextjs:nextjs /app/public ./public

# Switch to non-root user
USER nextjs

# Expose port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3000/api/health || exit 1

# Set environment variables
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# Start the application
CMD ["node", "server.js"]