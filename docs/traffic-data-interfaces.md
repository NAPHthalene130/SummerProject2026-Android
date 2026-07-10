# 交通事故与违章事件管理系统数据接口设计

## 1. 总体链路

```text
RTSP 摄像头 -> YOLO 视频分析 -> 风险预测模型 -> 多模态事件识别 -> 指挥端研判/工单智能体 -> 移动端处置反馈
```

## 2. 服务端输入输出

### 2.1 RTSP 流接入

`POST /api/v1/cameras`

输入：

```json
{
  "cameraId": "cam-jiefang-north",
  "name": "解放路北口摄像头",
  "roadSegmentId": "seg-jiefang-001",
  "rtspUrl": "rtsp://user:pass@host/live/1",
  "location": {"lng": 116.3912, "lat": 39.9075},
  "direction": "north_to_south"
}
```

输出：

```json
{
  "cameraId": "cam-jiefang-north",
  "status": "ONLINE",
  "createdAt": "2026-07-09T14:00:00+08:00"
}
```

### 2.2 YOLO 实时检测结果

`POST /api/v1/vision/detections`

输入：

```json
{
  "cameraId": "cam-jiefang-north",
  "roadSegmentId": "seg-jiefang-001",
  "timestamp": "2026-07-09T14:20:31+08:00",
  "trafficFlowPerHour": 1840,
  "averageSpeedKmh": 18.5,
  "vehicles": [
    {
      "trackId": "trk-1001",
      "type": "car",
      "speedKmh": 16.2,
      "trajectory": [[120, 340], [130, 350], [145, 367]]
    }
  ]
}
```

输出：

```json
{
  "accepted": true,
  "batchId": "det-20260709-142031"
}
```

### 2.3 路段风险预测

`POST /api/v1/risk/predict`

输入：

```json
{
  "roadSegmentId": "seg-jiefang-001",
  "trafficFlowPerHour": 1840,
  "averageSpeedKmh": 18.5,
  "weather": {"condition": "rain", "visibilityMeters": 1200},
  "historicalAccidentCount30d": 7,
  "timeWindow": "2026-07-09T14:00:00+08:00/2026-07-09T15:00:00+08:00"
}
```

输出：

```json
{
  "roadSegmentId": "seg-jiefang-001",
  "riskScore": 87,
  "riskLevel": "HIGH",
  "factors": [
    {"name": "trafficFlowPerHour", "weight": 0.32},
    {"name": "rain", "weight": 0.18},
    {"name": "historicalAccidentCount30d", "weight": 0.21}
  ]
}
```

### 2.4 多模态事件识别

`POST /api/v1/events/analyze-frame-batch`

输入：

```json
{
  "cameraId": "cam-jiefang-north",
  "roadSegmentId": "seg-jiefang-001",
  "frameBatchUrl": "s3://traffic/frame-batch/event-1/",
  "videoClipUrl": "https://traffic.local/replay/event-1.mp4",
  "context": {
    "averageSpeedKmh": 18.5,
    "trafficFlowPerHour": 1840,
    "riskScore": 87
  }
}
```

输出：

```json
{
  "eventId": "event-1",
  "eventType": "REAR_END",
  "confidence": 0.91,
  "severity": "HIGH",
  "summary": "疑似两车追尾，后车急停后发生接触。",
  "evidence": {
    "videoReplayUrl": "https://traffic.local/replay/event-1.mp4",
    "keyFrameUrls": ["https://traffic.local/frame/event-1/001.jpg"]
  }
}
```

## 3. 管理页面端输入输出

### 页面一：道路路线图

输入：

`GET /api/v1/map/road-segments?bbox=...`

```json
{
  "segments": [
    {
      "roadSegmentId": "seg-jiefang-001",
      "geometry": {"type": "LineString", "coordinates": [[116.39, 39.90], [116.40, 39.91]]},
      "riskScore": 87,
      "trafficFlowPerHour": 1840,
      "activeEventId": "event-1"
    }
  ]
}
```

输出动作：

- `mode=risk`：Mapbox 路段颜色深浅按 `riskScore`。
- `mode=flow`：Mapbox 路段颜色深浅按 `trafficFlowPerHour`。
- 点击事故标志：请求 `GET /api/v1/events/{eventId}` 并播放 `videoReplayUrl`。

### 页面二：摄像头实时监控

输入：

`GET /api/v1/cameras/live-dashboard`

```json
{
  "cameras": [
    {
      "cameraId": "cam-jiefang-north",
      "name": "解放路北口摄像头",
      "streamPreviewUrl": "webrtc://traffic/cam-jiefang-north",
      "riskScore": 87,
      "trafficFlowPerHour": 1840,
      "averageSpeedKmh": 18.5,
      "weather": "小雨",
      "historicalAccidentCount30d": 7
    }
  ]
}
```

输出动作：

- 视频边框颜色按 `riskScore`。
- 报表展示车流量、车速、天气、历史事故数量。
- 点击摄像头进入事件列表或回放。

### 页面三：工单智能体与反馈处理

输入：

`POST /api/v1/work-orders/generate`

```json
{
  "eventId": "event-1",
  "eventType": "REAR_END",
  "severity": "HIGH",
  "summary": "疑似两车追尾",
  "assigneeGroup": "事故违章处置组"
}
```

输出：

```json
{
  "workOrderId": "wo-traffic-1",
  "title": "追尾事故现场处置",
  "suggestion": "设置警示区域，确认人员安全，拍摄事故全景和碰撞点。",
  "status": "PENDING_REPAIR"
}
```

反馈处理：

`GET /api/v1/work-orders?status=WAITING_DESKTOP_REVIEW`

`POST /api/v1/work-orders/{workOrderId}/review`

```json
{
  "reviewResult": "ACCEPTED",
  "reviewNote": "现场已恢复通行，工单闭环。"
}
```

## 4. 移动端输入输出

### 4.1 拉取工单

`GET /api/v1/mobile/work-orders?assignee=当前用户`

输出字段对应 Android `InspectionTask`：

```json
{
  "items": [
    {
      "id": "wo-traffic-1",
      "cameraId": "cam-jiefang-north",
      "roadSegmentId": "seg-jiefang-001",
      "title": "追尾事故现场处置",
      "zone": "解放路-人民大道北进口",
      "dueTime": "今天 14:35",
      "source": "视频事件智能体",
      "eventType": "REAR_END",
      "riskLevel": "HIGH",
      "anomaly": "疑似两车追尾，车流量 1840 辆/小时。",
      "videoReplayUrl": "https://traffic.local/replay/event-1.mp4",
      "status": "PENDING_REPAIR"
    }
  ]
}
```

### 4.2 提交处置反馈

`POST /api/v1/mobile/work-orders/{workOrderId}/feedback`

输入：

```json
{
  "resultStatus": "已现场处置",
  "resultNote": "已设置警示区域，事故车辆已靠边，通行恢复。",
  "photoUrls": [
    "https://traffic.local/upload/wo-traffic-1/photo-1.jpg"
  ],
  "submittedAt": "2026-07-09T14:45:00+08:00"
}
```

输出：

```json
{
  "workOrderId": "wo-traffic-1",
  "status": "WAITING_DESKTOP_REVIEW"
}
```

### 4.3 手动上报事件

`POST /api/v1/mobile/events`

输入字段对应 Android `AlertItem`：

```json
{
  "cameraId": "manual-mobile",
  "roadSegmentId": "manual-road",
  "title": "现场发现轻微刮擦",
  "location": "人民大道东向西辅路",
  "detail": "两车轻微刮擦，占用一条车道。",
  "eventType": "COLLISION",
  "severity": "MEDIUM",
  "photoUrls": []
}
```

输出：

```json
{
  "eventId": "event-mobile-1",
  "syncStatus": "SENT_TO_COMMAND_CENTER"
}
```
