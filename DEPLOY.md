# ECS Docker 部署指南（frontend + backend + st）

## 1. 服务器准备

- ECS 推荐：2C4G
- 系统：Ubuntu 22.04
- 安全组放行：`22`、`80`、`443`

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y git curl vim ufw
sudo ufw allow OpenSSH
sudo ufw allow 80
sudo ufw allow 443
sudo ufw enable
```

## 2. 安装 Docker / Compose

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
newgrp docker
docker --version
docker compose version
```

## 3. 拉代码并配置环境变量

```bash
git clone <你的仓库地址> st-pro
cd st-pro
cp .env.example .env
vim .env
```

最少要改：

- `MYSQL_ROOT_PASSWORD`
- `DB_PASSWORD`（与 `MYSQL_ROOT_PASSWORD` 一致，当 `DB_USER=root` 时）
- `JWT_SECRET`
- `DEEPSEEK_API_KEY` / `DASHSCOPE_API_KEY`（按需）
- `FRONTEND_ORIGINS`（必须包含用户浏览器访问地址，例如 `http://公网IP`）
- `DOMAIN`（仅在使用 `docker-compose.https.yml` 签 HTTPS 证书时需要真实域名；纯 IP + HTTP 可写 IP 或留空）

### 仅公网 IP、无域名、HTTP

仓库根目录已提供可直接参考的 `.env.example`。`docker-compose.yml` 中 MySQL 仅创建 `root` + 业务库，不再额外创建 `MYSQL_USER`，避免与 `DB_USER=root` 冲突。

## 4. 启动（HTTP）

```bash
bash deploy/scripts/start-http.sh
```

访问：

- `http://yourdomain.com/`
- `http://yourdomain.com/api/...`
- `http://yourdomain.com/st/`

## 5. 启动（HTTP + HTTPS 同时可用）

```bash
bash deploy/scripts/start-https.sh
```

> HTTPS 网关配置文件路径：`nginx/nginx.https.conf`

首次签发证书（Let's Encrypt）：

```bash
docker run --rm \
  -v "$(pwd)/deploy/certbot/www:/var/www/certbot" \
  -v "$(pwd)/deploy/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d "$DOMAIN" \
  --email your-email@example.com \
  --agree-tos --no-eff-email
docker compose -f docker-compose.yml -f docker-compose.https.yml restart nginx
```

访问：

- `http://yourdomain.com/`
- `https://yourdomain.com/`
- `http://yourdomain.com/st/`
- `https://yourdomain.com/st/`

## 6. 快速恢复 / 运维

- 宕机恢复（HTTP）：
  ```bash
  bash deploy/scripts/recover.sh
  ```
- 宕机恢复（HTTPS）：
  ```bash
  bash deploy/scripts/recover.sh https
  ```
- 证书续签：
  ```bash
  bash deploy/scripts/renew-cert.sh
  ```

建议用 cron 每天执行一次续签脚本。
