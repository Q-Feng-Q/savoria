# 旧版完整云托管打包方案

此目录保留上一版 `Nginx + Spring Boot` 双进程镜像，便于归档、对照或单独构建。根目录 `Dockerfile` 是当前推荐的纯后端单进程方案，两者互不依赖。

从仓库根目录执行：

```powershell
docker build -f docker/cloud-hosting-full/Dockerfile -t family-kitchen-cloud-hosting-full .
```

端口约定：

- Nginx：`PORT=8080`
- Spring Boot：`SERVER_PORT=8081`
- 云托管容器端口：`8080`

使用此归档方案时，控制台中的 `PORT` 必须是 `8080`，且不能与 `SERVER_PORT=8081` 相同。当前控制台若保持 `PORT=8081`，应继续使用根目录的纯后端 `Dockerfile`。
