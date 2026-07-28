# مواصفات واجهات HLRMS
## API Specification

## 1. المقدمة

تحدد هذه الوثيقة عقد واجهات البرمجة لنظام إدارة الطلبات عالية الحمل  
**High-Load Request Management System (HLRMS)**.

الهدف هو توحيد الاتصال بين Backend وAndroid والأنظمة العميلة وأدوات الاختبار والإدارة.

تمثل هذه الوثيقة المرجع الوظيفي، بينما يمثل `openapi.yaml` المرجع القابل للقراءة آليًا.

---

# 2. المبادئ العامة

- النمط: `REST`.
- التنسيق: `application/json`.
- الإصدار: `/api/v1`.
- الوقت: `ISO 8601` و`UTC`.
- المعرفات: `UUID`.
- أسماء حقول JSON: `camelCase`.

مثال:

```json
{
  "requestId": "7f39b30d-2395-4ef9-b669-e04d48d99456",
  "requestType": "DATA_PROCESSING",
  "createdAt": "2026-07-23T10:15:30Z"
}
```

---

# 3. المصادقة والصلاحيات

## 3.1 Bearer JWT

```http
Authorization: Bearer <access-token>
```

## 3.2 الأدوار

| الدور | الوصف |
|---|---|
| `CLIENT` | إرسال الطلبات وعرض الطلبات التابعة للنظام العميل. |
| `ADMIN` | عرض جميع الطلبات والطوابير وWorkers والإعدادات. |
| `MONITORING` | الوصول إلى بيانات Health وMetrics وفق إعداد النشر. |

لا يجوز لـ`CLIENT` الوصول إلى طلب تابع لنظام عميل آخر.

---

# 4. الترويسات المشتركة

## `X-Correlation-ID`

معرف لتتبع العملية عبر الخدمات والسجلات. إذا لم يرسله العميل، ينشئه النظام ويعيده في الاستجابة.

## `Idempotency-Key`

مطلوب عند إرسال طلب جديد.

القواعد:

- فريد داخل نطاق Client System.
- الحد الأقصى 120 محرفًا.
- تكرار المفتاح مع نفس البيانات يعيد الطلب السابق.
- تكراره مع بيانات مختلفة يعيد `409 Conflict`.

## `Retry-After`

قد يظهر مع `429` أو `503`.

---

# 5. تنسيق الاستجابة

## مورد واحد

```json
{
  "requestId": "7f39b30d-2395-4ef9-b669-e04d48d99456",
  "status": "QUEUED"
}
```

## قائمة مرقمة

```json
{
  "items": [],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true
  }
}
```

## خطأ

```json
{
  "timestamp": "2026-07-23T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "فشل التحقق من بيانات الطلب.",
  "path": "/api/v1/requests",
  "correlationId": "87d761f6-7e85-4c8b-b3eb-d70f939e933a",
  "fieldErrors": [
    {
      "field": "requestType",
      "code": "NOT_BLANK",
      "message": "نوع الطلب مطلوب."
    }
  ]
}
```

---

# 6. حالات الطلب

```text
RECEIVED
VALIDATING
ACCEPTED
REJECTED
QUEUED
PROCESSING
SUCCEEDED
FAILED
RETRY_SCHEDULED
DEAD_LETTERED
CANCELLED
```

الحالات النهائية:

```text
REJECTED
SUCCEEDED
DEAD_LETTERED
CANCELLED
```

`FAILED` ليست نهائية بالضرورة، فقد تنتقل إلى `RETRY_SCHEDULED`.

---

# 7. إرسال طلب جديد

```http
POST /api/v1/requests
```

الصلاحية: `CLIENT`.

الترويسات المطلوبة:

```http
Authorization: Bearer <token>
Idempotency-Key: <unique-key>
Content-Type: application/json
```

جسم الطلب:

```json
{
  "requestType": "DATA_PROCESSING",
  "priority": "NORMAL",
  "payload": {
    "source": "android-demo",
    "operation": "calculate",
    "value": 42
  },
  "metadata": {
    "locale": "ar",
    "deviceId": "android-emulator-01"
  }
}
```

| الحقل | النوع | مطلوب | الوصف |
|---|---|---:|---|
| `requestType` | String | نعم | نوع الطلب، من 1 إلى 80 محرفًا. |
| `priority` | Enum | لا | `HIGH` أو `NORMAL` أو `LOW`، والافتراضي `NORMAL`. |
| `payload` | Object | نعم | بيانات الطلب المرنة. |
| `metadata` | Object | لا | بيانات إضافية. |

استجابة النجاح:

```http
202 Accepted
```

```json
{
  "requestId": "7f39b30d-2395-4ef9-b669-e04d48d99456",
  "status": "QUEUED",
  "requestType": "DATA_PROCESSING",
  "priority": "NORMAL",
  "receivedAt": "2026-07-23T10:15:30Z",
  "statusUrl": "/api/v1/requests/7f39b30d-2395-4ef9-b669-e04d48d99456/status"
}
```

| الحالة | المعنى |
|---:|---|
| `202` | تم قبول الطلب للمعالجة غير المتزامنة. |
| `400` | بيانات غير صحيحة. |
| `401` | مصادقة غير صحيحة. |
| `403` | لا توجد صلاحية. |
| `409` | تعارض Idempotency. |
| `413` | Payload كبير. |
| `429` | تجاوز Rate Limit. |
| `503` | تعذر قبول الطلب مؤقتًا. |

---

# 8. عرض تفاصيل طلب

```http
GET /api/v1/requests/{requestId}
```

```json
{
  "requestId": "7f39b30d-2395-4ef9-b669-e04d48d99456",
  "requestType": "DATA_PROCESSING",
  "priority": "NORMAL",
  "status": "SUCCEEDED",
  "payload": {
    "source": "android-demo",
    "operation": "calculate",
    "value": 42
  },
  "metadata": {
    "locale": "ar"
  },
  "receivedAt": "2026-07-23T10:15:30Z",
  "queuedAt": "2026-07-23T10:15:30.120Z",
  "processingStartedAt": "2026-07-23T10:15:30.400Z",
  "completedAt": "2026-07-23T10:15:30.930Z",
  "attemptCount": 1,
  "result": {
    "outcome": "SUCCESS",
    "data": {
      "computedValue": 84
    },
    "processingTimeMs": 530
  }
}
```

قد يكون `result` فارغًا قبل اكتمال المعالجة.

---

# 9. الاستعلام عن حالة الطلب

```http
GET /api/v1/requests/{requestId}/status
```

```json
{
  "requestId": "7f39b30d-2395-4ef9-b669-e04d48d99456",
  "status": "PROCESSING",
  "attemptNumber": 2,
  "updatedAt": "2026-07-23T10:16:00Z",
  "terminal": false
}
```

هذه الواجهة أخف من واجهة التفاصيل ومناسبة للاستعلام الدوري من Android.

---

# 10. عرض سجل الحالات

```http
GET /api/v1/requests/{requestId}/history
```

```json
{
  "requestId": "7f39b30d-2395-4ef9-b669-e04d48d99456",
  "items": [
    {
      "fromStatus": null,
      "toStatus": "RECEIVED",
      "reasonCode": "REQUEST_RECEIVED",
      "reasonMessage": null,
      "changedAt": "2026-07-23T10:15:30Z"
    }
  ]
}
```

---

# 11. البحث في الطلبات

```http
GET /api/v1/requests
```

| المعامل | النوع | الوصف |
|---|---|---|
| `status` | Enum متعدد | التصفية حسب حالة أو عدة حالات. |
| `requestType` | String | نوع الطلب. |
| `priority` | Enum | الأولوية. |
| `createdFrom` | DateTime | تاريخ البداية شامل. |
| `createdTo` | DateTime | تاريخ النهاية غير شامل. |
| `page` | Integer | يبدأ من صفر. |
| `size` | Integer | من 1 إلى 100، الافتراضي 20. |
| `sort` | String | مثال `createdAt,desc`. |

مثال:

```http
GET /api/v1/requests?status=FAILED&status=RETRY_SCHEDULED&page=0&size=20&sort=createdAt,desc
```

---

# 12. واجهات الإدارة

جميعها تحتاج الدور `ADMIN`.

## البحث الإداري

```http
GET /api/v1/admin/requests
```

يدعم أيضًا:

```text
clientSystemId
workerId
failureType
```

## حالة الطوابير

```http
GET /api/v1/admin/queues
```

```json
{
  "items": [
    {
      "name": "hlrms.requests.normal.q",
      "messagesReady": 120,
      "messagesUnacknowledged": 8,
      "consumerCount": 4,
      "publishRate": 340.5,
      "deliveryRate": 332.2,
      "status": "HEALTHY"
    }
  ],
  "collectedAt": "2026-07-23T10:15:30Z"
}
```

## حالة Workers

```http
GET /api/v1/admin/workers
```

## عرض الإعدادات

```http
GET /api/v1/admin/configurations
```

## تعديل إعداد

```http
PATCH /api/v1/admin/configurations/{key}
```

```json
{
  "value": 5,
  "reason": "Increase retry limit after load-test review."
}
```

يجب التحقق من القيمة، وتسجيل القيمة السابقة والجديدة والمستخدم والوقت والسبب.

---

# 13. Health وMetrics

```http
GET /actuator/health
GET /actuator/prometheus
```

قد تقيد هذه الواجهات على الشبكة الداخلية في الإنتاج.

---

# 14. Pagination وSorting

الترقيم يبدأ من الصفر، والحد الأقصى لـ`size` هو 100.

صيغة الفرز:

```text
field,direction
```

الحقول المسموحة مبدئيًا:

```text
createdAt
updatedAt
receivedAt
completedAt
priority
status
```

---

# 15. Rate Limiting

```http
429 Too Many Requests
Retry-After: 30
```

---

# 16. حدود أولية

| العنصر | الحد |
|---|---:|
| Request Body | 1 MB |
| `requestType` | 80 محرفًا |
| `Idempotency-Key` | 120 محرفًا |
| `X-Correlation-ID` | 100 محرف |
| `metadata` keys | 50 مفتاحًا |
| Page Size | 100 عنصر |

---

# 17. قواعد التوافق

1. إضافة حقل اختياري تغيير متوافق.
2. حذف حقل أو تغيير نوعه تغيير غير متوافق.
3. إضافة قيمة Enum جديدة تحتاج تنسيقًا مع العملاء.
4. يجب أن يتجاهل العميل الحقول غير المعروفة.
5. التغيير غير المتوافق يحتاج `/api/v2`.
