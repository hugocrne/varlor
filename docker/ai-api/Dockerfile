# ------------------------------------------------------------
# 🏗️  BUILD STAGE
# ------------------------------------------------------------
FROM node:18-alpine AS build

# Set working directory
WORKDIR /app

# Copy package files
COPY ai-api/package*.json ./
COPY ai-api/tsconfig*.json ./
COPY ai-api/nest-cli.json ./

# Install all dependencies (including dev dependencies for build)
RUN npm ci && npm cache clean --force

# Copy source code
COPY ai-api/src ./src

# Build the application
RUN npm run build

# Remove dev dependencies after build
RUN npm prune --production

# ------------------------------------------------------------
# 🚀  RUNTIME STAGE
# ------------------------------------------------------------
FROM node:18-alpine AS runtime

# Set working directory
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S nodejs
RUN adduser -S nestjs -u 1001

# Copy package files
COPY ai-api/package*.json ./

# Install only production dependencies
RUN npm ci --only=production && npm cache clean --force

# Copy built application from build stage
COPY --from=build --chown=nestjs:nodejs /app/dist ./dist

# Switch to non-root user
USER nestjs

# Expose port
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD node dist/main || exit 1

# Set environment variables
ENV NODE_ENV=production
ENV PORT=3000

# Start the application
CMD ["npm", "run", "start:prod"]