# توثيق واجهات HLRMS

يحتوي هذا المجلد على عقد واجهات البرمجة الخاصة بنظام إدارة الطلبات عالية الحمل  
**High-Load Request Management System (HLRMS)**.

## الملفات

- `API-SPECIFICATION.md`: القواعد العامة، المصادقة، الترويسات، الموارد، البحث، التصفية والترقيم.
- `ERROR-CATALOG.md`: تنسيق الأخطاء الموحد ورموز الأخطاء وحالات HTTP.
- `openapi.yaml`: مواصفة OpenAPI 3.1 قابلة للفتح في Swagger Editor أو Redoc.

## عنوان الواجهة

```text
/api/v1
```

مثال محلي:

```text
http://localhost:8080/api/v1
```

## الموارد الأساسية

```http
POST /requests
GET  /requests/{requestId}
GET  /requests/{requestId}/status
GET  /requests/{requestId}/history
GET  /requests
```

واجهات الإدارة:

```http
GET   /admin/requests
GET   /admin/queues
GET   /admin/workers
GET   /admin/configurations
PATCH /admin/configurations/{key}
```

واجهات التشغيل:

```http
GET /actuator/health
GET /actuator/prometheus
```

## المصادقة

تعتمد الواجهات العامة والإدارية على `Bearer JWT`:

```http
Authorization: Bearer <access-token>
```

كما يدعم إرسال الطلبات:

```http
Idempotency-Key: <unique-value>
X-Correlation-ID: <correlation-id>
```

## التحقق المقترح

```bash
npx @redocly/cli lint openapi.yaml
```

## ملاحظات

1. تمثل الوثائق عقدًا أوليًا بين Backend وAndroid.
2. يجب تحديث المواصفة عند تعديل أي Endpoint أو Schema.
3. جميع الأوقات بصيغة `ISO 8601` وفي `UTC`.
4. جميع المعرفات الأساسية من النوع `UUID`.
5. التغيير غير المتوافق يحتاج إصدارًا جديدًا مثل `/api/v2`.
