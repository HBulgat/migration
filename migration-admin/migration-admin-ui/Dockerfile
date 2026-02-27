FROM node:20-alpine AS builder

WORKDIR /app

# npm install
COPY package*.json ./
RUN npm config set registry https://registry.npmmirror.com && npm install

# Build the app
COPY . .
RUN npm run build

# Production image
FROM nginx:alpine

# Copy custom nginx config if we had one. Let's create a default one below if needed or just use default.
# COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
