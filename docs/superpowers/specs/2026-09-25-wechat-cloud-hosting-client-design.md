# 微信云托管小程序请求接入设计

## 目标

小程序生产环境通过微信云托管环境 `prod-d5g0vleyp9ed264bf` 调用服务 `springboot-6dl0`，页面和业务 service 不直接依赖微信云托管 API；本地开发仍可通过关闭开关使用现有 HTTP 请求。

## 设计

- `frontend/app.js` 负责初始化 `wx.cloud` 并声明唯一的云托管运行配置。
- `frontend/utils/cloud-hosting.js` 负责把现有请求参数转换成 `callContainer` 参数，并为 HTTP 文件传输补充服务路由头。
- `frontend/utils/api-runtime.js` 根据配置选择云托管或原 HTTP 适配器，业务 service 与页面保持不变。
- 普通 JSON API 使用 `wx.cloud.callContainer`；multipart 上传和临时文件下载继续使用 `wx.uploadFile` / `wx.downloadFile`，走云环境 HTTP 访问域名。
- 所有请求继续携带现有 JWT 和家庭、商户、成员身份头。

## 错误处理

云能力不可用、环境或服务未配置时立即返回可读错误。后端 HTTP 状态码和业务 `code` 仍由现有 API 客户端统一转换为异常。

## 验证

新增云托管适配器单元测试，覆盖路径归一化、环境/服务头、身份头和 HTTP 回退；再运行全部小程序测试。
