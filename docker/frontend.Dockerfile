# ============ 多阶段构建:Node 编译出静态文件,nginx 托管 ============
# 第一阶段:构建前端
FROM node:22-alpine AS build
WORKDIR /build
# 先只拷 package 并安装依赖,利用 Docker 层缓存
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
# 再拷源码构建
COPY frontend/ ./
RUN npm run build

# 第二阶段:nginx 托管静态文件 + 反向代理后端
FROM nginx:alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
