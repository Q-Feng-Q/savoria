# 纯后端云托管打包方案

此目录只构建并运行 Spring Boot JAR，不包含 Nginx、前端或 MySQL。该方案与当前云托管环境变量 `PORT=8081` 一致，是当前推荐方案。

从仓库根目录执行：

```powershell
docker build -f docker/cloud-hosting-backend/Dockerfile -t family-kitchen-cloud-hosting-backend .
```

端口约定：

- Spring Boot：直接监听 `PORT=8081`
- 云托管容器端口：`8081`
- 不设置 `SERVER_PORT`

根目录的 `Dockerfile` 与此目录保持相同运行模式，供默认的仓库部署入口使用。
