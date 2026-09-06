#!/usr/bin/env bash
# WMS Cloudflare Tunnel 一键启动脚本
# 将本地前端(3001)暴露到公网，打印公网访问地址
set -e

FE_PORT=3001
TUNNEL_LOG=/tmp/cloudflared.log

# 检查 cloudflared 是否安装
command -v cloudflared >/dev/null 2>&1 || {
  echo "未安装 cloudflared，请先执行: brew install cloudflared"
  exit 1
}

# 检查前端是否在跑
if ! curl -s -o /dev/null --max-time 3 http://localhost:$FE_PORT/; then
  echo "前端 $FE_PORT 未启动，请先执行 ./scripts/start-lan.sh"
  exit 1
fi

# 清掉旧隧道
pkill -f "cloudflared tunnel" 2>/dev/null && sleep 1 || true

# 启动隧道
nohup cloudflared tunnel --url http://localhost:$FE_PORT > "$TUNNEL_LOG" 2>&1 &
TUNNEL_PID=$!
echo "cloudflared PID: $TUNNEL_PID"
echo "等待公网地址分配..."

# 等待公网 URL 出现
URL=""
for i in $(seq 1 30); do
  URL=$(grep -oE "https://[a-z0-9-]+\.trycloudflare\.com" "$TUNNEL_LOG" 2>/dev/null | head -1)
  [ -n "$URL" ] && break
  sleep 1
done

if [ -z "$URL" ]; then
  echo "隧道启动失败，查看日志: tail -f $TUNNEL_LOG"
  exit 1
fi

echo ""
echo "================ 隧道就绪 ================"
echo "公网访问: $URL"
echo "日志:     tail -f $TUNNEL_LOG"
echo "停止:     pkill -f 'cloudflared tunnel'"
echo "=========================================="
