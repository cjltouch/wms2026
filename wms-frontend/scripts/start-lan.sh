#!/usr/bin/env bash
# WMS 局域网一键启动脚本
# 启动后端(8080) + 前端(3000)，并打印局域网访问地址
set -e

FE_DIR="$HOME/Documents/trae_projects/wms-frontend"
BE_DIR="$HOME/Documents/trae_projects/wms-backend"

echo "=== 获取局域网 IP ==="
LAN_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null)
echo "局域网 IP: $LAN_IP"

# 清掉可能残留的旧进程
for port in 3000 8080; do
  pids=$(lsof -ti:$port 2>/dev/null)
  [ -n "$pids" ] && kill -9 $pids 2>/dev/null && echo "已清理 $port 端口旧进程"
done
sleep 2

echo "=== 启动后端 8080 ==="
cd "$BE_DIR"
xattr -rc target/ 2>/dev/null || true
LOG_BASE=/tmp/wms-backend-logs nohup mvn spring-boot:run > /tmp/wms-backend.log 2>&1 &
echo "后端 PID: $!  日志: /tmp/wms-backend.log"

echo "=== 启动前端 3000 ==="
cd "$FE_DIR"
nohup npm run dev > /tmp/wms-frontend.log 2>&1 &
echo "前端 PID: $!  日志: /tmp/wms-frontend.log"

echo "=== 等待服务就绪 ==="
for i in $(seq 1 60); do
  be=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/wms-api/api/system/auth/captcha 2>/dev/null)
  fe=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:3000/ 2>/dev/null)
  [ "$be" = "200" ] && [ "$fe" = "200" ] && break
  sleep 2
done

echo ""
echo "================ 就绪 ================"
echo "前端:  http://$LAN_IP:3000"
echo "后端:  http://$LAN_IP:8080"
echo "======================================="
