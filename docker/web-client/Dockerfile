# ------------------------------------------------------------
# 🏗️  BUILD STAGE
# ------------------------------------------------------------
FROM node:20-alpine AS build

# Set working directory
WORKDIR /app

# Copy package files
COPY web-client/package*.json ./
COPY web-client/angular.json ./
COPY web-client/tsconfig*.json ./

# Install dependencies
RUN npm ci && npm cache clean --force

# Copy source code
COPY web-client/src ./src
COPY web-client/public ./public

# Build the application (production build)
RUN npm run build

# ------------------------------------------------------------
# 🚀  RUNTIME STAGE (Nginx)
# ------------------------------------------------------------
FROM nginx:alpine AS runtime

# Copy custom nginx configuration
COPY config/web-client/nginx.conf /etc/nginx/nginx.conf

# Copy built application from build stage
COPY --from=build /app/dist/web-client /usr/share/nginx/html

# Change ownership of nginx directories for the existing nginx user
RUN chown -R nginx:nginx /usr/share/nginx/html
RUN chown -R nginx:nginx /var/cache/nginx
RUN chown -R nginx:nginx /var/log/nginx
RUN chown -R nginx:nginx /etc/nginx/conf.d
RUN touch /var/run/nginx.pid
RUN chown -R nginx:nginx /var/run/nginx.pid

# Switch to non-root user (nginx user already exists)
USER nginx

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]