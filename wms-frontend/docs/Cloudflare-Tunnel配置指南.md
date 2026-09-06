# WMS Cloudflare Tunnel 配置指南

用 Cloudflare Tunnel 把本机（Mac）的 WMS 前端服务暴露到公网，**无需公网 IP、无需端口映射、无需 DDNS**，适用于：

- 同事 / 客户跨网络访问演示
- 手机热点环境下对外提供服务（手机运营商多为 CGNAT，无公网 IP）
- 临时联调、远程验收

> 当前公网地址：**https://stocks-crest-lasting-episode.trycloudflare.com**（临时隧道，重启 cloudflared 后会变，见下）

---

## 一、原理速览

```
[外网用户] → https://xxx.trycloudflare.com
                    │
                    ▼
            [Cloudflare 边缘节点]
                    │ (出站隧道，本机主动连接)
                    ▼
            [本机 cloudflared 进程]
                    │
                    ▼
            [本机 Vite 前端 :3001] ──proxy──> [本机 Spring Boot :8081]
```

关键点：
- `cloudflared` 只发起**出站**连接到 Cloudflare 边缘，不需要在路由器/防火墙开入站端口。
- 公网用户访问 `trycloudflare.com` 子域，流量被 Cloudflare 转发到本机。
- Vite 代理 `/wms-api/**` 到 `127.0.0.1:8081`，所以**只需暴露前端 3001**，后端自动跟随。

---

## 二、一次性安装

### 1. 安装 cloudflared

```bash
brew install cloudflared
# 验证
cloudflared --version
# 期望输出类似：cloudflared version 2026.8.3 ...
```

### 2. 修改前端配置：放行公网域名

文件：`vite.config.ts`

```ts
server: {
  host: true,                  // 监听所有网卡（局域网也要）
  allowedHosts: true,          // ← 关键：允许所有主机名（含 trycloudflare.com 公网域名）
  port: 3001,
  proxy: {
    '/wms-api': {
      target: 'http://127.0.0.1:8081',
      changeOrigin: true
    }
  }
}
```

> ⚠️ **重要**：Vite 5.4+ 会**强制校验** `allowedHosts`。如果保留默认值或写成 `['wmsloc']`，公网访问会返回 403：
> ```
> Blocked request. This host ("xxx.trycloudflare.com") is not allowed.
> ```
> 必须设置 `allowedHosts: true`（放行所有）或将具体域名加进数组。临时隧道域名每次随机，所以用 `true` 最方便。

### 3. 防火墙

```bash
# 默认关闭即可（cloudflared 是出站连接，不需要开入站端口）
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --getglobalstate
# 如需关闭：sudo /usr/libexec/ApplicationFirewall/socketfilterfw --setglobalstate off
```

---

## 三、使用方式

### 前提：前端 + 后端已启动

参考 `docs/局域网访问指南.md` 启动服务，或用脚本：

```bash
./scripts/start-lan.sh   # 启动后端 8081 + 前端 3001
```

确认本地可访问：

```bash
curl -I http://localhost:3001/                                       # 前端
curl -I http://localhost:8081/wms-api/api/system/auth/captcha        # 后端
```

### 方式 A：临时 Quick Tunnel（最快，推荐临时用）

```bash
# 后台启动隧道
nohup cloudflared tunnel --url http://localhost:3001 > /tmp/cloudflared.log 2>&1 &

# 查看分配的公网域名
grep -oE "https://[a-z0-9-]+\.trycloudflare\.com" /tmp/cloudflared.log | head -1
```

输出示例：

```
https://stocks-crest-lasting-episode.trycloudflare.com
```

把这个地址发给任何人，他们打开即可用 admin/123456 登录。

### 方式 B：前台运行（调试用，可看实时日志）

```bash
cloudflared tunnel --url http://localhost:3001
```

`Ctrl+C` 即停止隧道。

---

## 四、验证隧道可用性

```bash
# 1. 读公网 URL
TUNNEL_URL=$(grep -oE "https://[a-z0-9-]+\.trycloudflare\.com" /tmp/cloudflared.log | head -1)
echo "公网地址: $TUNNEL_URL"

# 2. 访问前端首页
curl -sI "$TUNNEL_URL/" | head -3
# 期望: HTTP/2 200

# 3. 通过 Vite 代理访问后端验证码接口
curl -sI "$TUNNEL_URL/wms-api/api/system/auth/captcha" | head -3
# 期望: HTTP/2 200

# 4. 测登录接口
curl -s -X POST "$TUNNEL_URL/wms-api/api/system/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456","code":"","uuid":""}'
# 期望: 返回 {"code":200,...,"accessToken":"eyJ..."}
```

---

## 五、停止隧道

```bash
# 方式 1：按进程名
pkill -f "cloudflared tunnel"

# 方式 2：按端口（metrics 服务在 127.0.0.1:20241）
lsof -ti:20241 | xargs kill -9

# 方式 3：手动找 PID
ps aux | grep cloudflared | grep -v grep
kill <PID>
```

停止后公网地址立即失效，再次访问会 530 / 502。

---

## 六、常用排查命令

```bash
# 1. 查看 cloudflared 是否在跑
ps aux | grep cloudflared | grep -v grep
# 期望看到: cloudflared tunnel --url http://localhost:3001

# 2. 查看隧道实时日志（连接失败、URL 变更都在这）
tail -f /tmp/cloudflared.log

# 3. 查看 cloudflared 内部 metrics（连接数、健康状况）
curl http://127.0.0.1:20241/metrics | grep -E "tunnel|cloudflared"

# 4. 公网返回 403 → vite allowedHosts 没配
#    错误信息: Blocked request. This host ("xxx.trycloudflare.com") is not allowed.
#    修复: vite.config.ts 设 allowedHosts: true，重启 npm run dev

# 5. 公网返回 502 / 530 → 隧道断了或前端没起
#    检查: curl http://localhost:3001/ 是否 200
#    检查: ps aux | grep cloudflared 是否还在

# 6. 前端能打开但接口 404 → Vite 代理没生效
#    检查: curl http://localhost:3001/wms-api/api/system/auth/captcha 是否 200
#    若 404，确认 vite.config.ts 的 proxy 配置存在
```

---

## 七、临时隧道的限制

| 项目 | 临时 Quick Tunnel |
|------|-------------------|
| 域名是否固定 | ❌ 每次启动随机（`xxx.trycloudflare.com`） |
| 进程退出后 | ❌ 立即失效 |
| SLA / 稳定性 | ❌ 无保证，Cloudflare 可随时回收 |
| 是否需账号 | ❌ 不需要 |
| 适合场景 | 临时演示、联调验收 |
| 不适合 | 生产环境、长期服务 |

---

## 八、进阶：固定域名（命名隧道）

如需固定 URL 或绑定自有域名，需用 Cloudflare 账户创建**命名隧道**。

### 1. 登录 Cloudflare

```bash
cloudflared tunnel login
```

会打开浏览器，选择要绑定的域名（域名需先在 Cloudflare 托管 DNS），授权后本地生成 `~/.cloudflared/cert.pem`。

### 2. 创建命名隧道

```bash
cloudflared tunnel create wms-frontend
# 输出: Created tunnel wms-frontend with id xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
# 凭据文件: ~/.cloudflared/<UUID>.json
```

### 3. 绑定 DNS

```bash
# 用自有域名 → 自动创建 CNAME 指向隧道
cloudflared tunnel route dns wms-frontend wms.yourdomain.com
```

### 4. 写配置文件

`~/.cloudflared/config.yml`：

```yaml
tunnel: <上面输出的 UUID>
credentials-file: /Users/<你的用户名>/.cloudflared/<UUID>.json

ingress:
  - hostname: wms.yourdomain.com
    service: http://localhost:3001
  - service: http_status:404
```

### 5. 启动命名隧道

```bash
# 前台
cloudflared tunnel run wms-frontend

# 后台
nohup cloudflared tunnel run wms-frontend > /tmp/cloudflared.log 2>&1 &
```

之后访问 `https://wms.yourdomain.com` 即固定可用。

### 6. 开机自启（可选）

```bash
sudo cloudflared service install
# 服务文件: /etc/cloudflared/config.yml（需要把 ~/.cloudflared 下的配置拷过去）
```

---

## 九、与局域网访问的关系

| 场景 | 用哪个 |
|------|--------|
| 同 WiFi / 同办公室内联调 | 局域网访问（`http://172.20.10.6:3001`），无需 cloudflared |
| 跨网络 / 远程演示 / 手机热点 | Cloudflare Tunnel |
| 长期对外提供服务 | 命名隧道 + 自有域名（见第八节） |

两者共用同一套前端 + 后端，**不冲突**，可以同时开启。

---

## 十、一键脚本（可选）

把下面存为 `scripts/start-tunnel.sh`：

```bash
#!/usr/bin/env bash
# 启动 Cloudflare Tunnel，并打印公网地址
set -e

# 检查 cloudflared
command -v cloudflared >/dev/null 2>&1 || { echo "请先 brew install cloudflared"; exit 1; }

# 检查前端是否在跑
if ! curl -s -o /dev/null http://localhost:3001/; then
  echo "前端 3001 未启动，先执行 ./scripts/start-lan.sh"
  exit 1
fi

# 清掉旧隧道
pkill -f "cloudflared tunnel" 2>/dev/null && sleep 1 || true

# 启动隧道
nohup cloudflared tunnel --url http://localhost:3001 > /tmp/cloudflared.log 2>&1 &
echo "cloudflared PID: $!"
echo "等待公网地址分配..."

for i in $(seq 1 30); do
  URL=$(grep -oE "https://[a-z0-9-]+\.trycloudflare\.com" /tmp/cloudflared.log | head -1)
  [ -n "$URL" ] && break
  sleep 1
done

if [ -z "$URL" ]; then
  echo "隧道启动失败，查看日志: tail -f /tmp/cloudflared.log"
  exit 1
fi

echo ""
echo "================ 隧道就绪 ================"
echo "公网访问: $URL"
echo "日志:     tail -f /tmp/cloudflared.log"
echo "停止:     pkill -f 'cloudflared tunnel'"
echo "=========================================="
```

使用：

```bash
chmod +x scripts/start-tunnel.sh
./scripts/start-tunnel.sh
```

---

## 附：常用命令速查

```bash
# 启动临时隧道
nohup cloudflared tunnel --url http://localhost:3001 > /tmp/cloudflared.log 2>&1 &

# 查公网地址
grep -oE "https://[a-z0-9-]+\.trycloudflare\.com" /tmp/cloudflared.log | head -1

# 查日志
tail -f /tmp/cloudflared.log

# 查 metrics
curl http://127.0.0.1:20241/metrics

# 停止
pkill -f "cloudflared tunnel"

# 升级
brew upgrade cloudflared
```
