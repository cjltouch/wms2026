#!/usr/bin/env bash
# WMS 局域网一键启动脚本
# 启动后端(8081) + 前端(3001)，并打印局域网访问地址
set -e

FE_DIR="$HOME/Documents/trae_projects/wms-frontend"
BE_DIR="$HOME/Documents/trae_projects/wms-backend"

# 端口配置（与 vite.config.ts / application.yml 保持一致）
FE_PORT=3001
BE_PORT=8081

echo "=== 获取局域网 IP ==="
LAN_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null)
echo "局域网 IP: $LAN_IP"

# 清掉可能残留的旧进程
for port in $FE_PORT $BE_PORT; do
  pids=$(lsof -ti:$port 2>/dev/null)
  [ -n "$pids" ] && kill -9 $pids 2>/dev/null && echo "已清理 $port 端口旧进程"
done
sleep 2

echo "=== 启动后端 $BE_PORT ==="
cd "$BE_DIR"
xattr -rc target/ 2>/dev/null || true
LOG_BASE=/tmp/wms-backend-logs nohup mvn spring-boot:run > /tmp/wms-backend.log 2>&1 &
echo "后端 PID: $!  日志: /tmp/wms-backend.log"

echo "=== 启动前端 $FE_PORT ==="
cd "$FE_DIR"
nohup npm run dev > /tmp/wms-frontend.log 2>&1 &
echo "前端 PID: $!  日志: /tmp/wms-frontend.log"

echo "=== 等待服务就绪 ==="
for i in $(seq 1 60); do
  be=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:$BE_PORT/wms-api/api/system/auth/captcha 2>/dev/null)
  fe=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:$FE_PORT/ 2>/dev/null)
  [ "$be" = "200" ] && [ "$fe" = "200" ] && break
  sleep 2
done

echo ""
echo "================ 就绪 ================"
echo "前端:  http://$LAN_IP:$FE_PORT"
echo "后端:  http://$LAN_IP:$BE_PORT"
echo "======================================="
