# HTTPS 部署（HTTP/HTTPS 并存）

> 网关配置文件：`nginx/nginx.https.conf`

## 1) 启动编排

```bash
cp .env.example .env
mkdir -p deploy/certbot/www deploy/certbot/conf
docker compose -f docker-compose.yml -f docker-compose.https.yml up -d --build
```

## 2) 申请证书

```bash
docker run --rm \
  -v "$(pwd)/deploy/certbot/www:/var/www/certbot" \
  -v "$(pwd)/deploy/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certonly --webroot \
  -w /var/www/certbot \
  -d "$DOMAIN" \
  --email your-email@example.com \
  --agree-tos --no-eff-email
```

## 3) 重载网关

```bash
docker compose -f docker-compose.yml -f docker-compose.https.yml restart nginx
```

## 4) 验证

- `http://yourdomain.com`
- `https://yourdomain.com`
- `http://yourdomain.com/api/...` 与 `https://yourdomain.com/api/...`
- `http://yourdomain.com/st/` 与 `https://yourdomain.com/st/`

## 5) 续签

```bash
bash deploy/scripts/renew-cert.sh
```
