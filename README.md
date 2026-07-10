# TrafficAndriod

交通事故与违章事件管理系统移动端。

该应用负责接收云端/指挥端工单，查看交通事件信息，提交现场处理报告和图片反馈。当前项目保留了本地模拟数据，后续可通过配置云端服务地址接入真实后端。

## 主要功能

- 事故与违章事件总览
- 事件上报
- 工单接收与处置反馈
- 现场照片拍摄/相册上传
- 智能处置建议
- 云端服务地址配置

## 工单接口

- `GET /api/v1/work-orders/`
- `GET /api/v1/staff/`
- `PUT /api/v1/work-orders/{work_order_id}/dispatch`
- `PATCH /api/v1/work-orders/{work_order_id}/status`

完整接口设计见：

```text
docs/traffic-data-interfaces.md
```

## 云端服务器配置

应用不依赖电脑本机地址。打开“我的 -> 云端服务器设置”，输入已部署后端的 HTTPS 根地址，例如：

```text
https://traffic-api.example.com
```

保存后，应用会检查 `/health`，后续工单、人员和图片请求都会使用保存的云端地址。

也可以在构建时写入默认云端地址：

```powershell
.\gradlew.bat :app:assembleDebug -PTRAFFIC_API_BASE_URL=https://traffic-api.example.com
```

## 构建

```powershell
.\gradlew.bat :app:assembleDebug
```

Debug APK 输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```
