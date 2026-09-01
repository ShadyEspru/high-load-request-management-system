# مواصفات API المنفذة

## القواعد العامة

- Base URL: `http://localhost:8088`
- كل واجهات Requests تحتاج `Authorization: Bearer <JWT>`.
- يمرر Gateway هوية موثوقة إلى الخدمات الداخلية؛ لا يرسل Client ترويسات `X-User-*`.
- كل POST للطلبات يحتاج `Idempotency-Key` لا يتجاوز 100 محرف.
- `requestType` مطلوب وبحد أقصى 100 محرف.
- `payload` String مطلوب وبحد أقصى 10000 محرف؛ يمكن أن يحتوي JSON serialized.

## Authentication

### Register

```http
POST /api/v1/auth/register
```

```json
{
  "email": "user@example.com",
  "password": "strong-password",
  "firstName": "Test",
  "lastName": "User"
}
```

النجاح: `201 Created` مع `id`, `email`, `firstName`, `lastName`, `createdAt`.

### Login

```http
POST /api/v1/auth/login
```

```json
{
  "email": "user@example.com",
  "password": "strong-password"
}
```

النجاح: `200 OK` مع `accessToken`, `refreshToken`, `tokenType`.

### Refresh

```http
POST /api/v1/auth/refresh
```

```json
{"refreshToken":"<token>"}
```

## إنشاء طلب

```http
POST /api/v1/requests
Authorization: Bearer <access-token>
Idempotency-Key: <key>
Content-Type: application/json
```

```json
{
  "requestType": "STANDARD",
  "payload": "{\"source\":\"client\",\"operation\":\"example\"}"
}
```

| الحالة | المعنى |
|---|---|
| `201 Created` | طلب جديد؛ `Idempotency-Replayed: false` و`Location` موجودان |
| `200 OK` | Replay مطابق؛ `Idempotency-Replayed: true` |
| `400 Bad Request` | Header مفقود أو DTO غير صالح |
| `401 Unauthorized` | JWT أو Trusted Identity غير صالح |
| `409 Conflict` | المفتاح نفسه استُخدم لمحتوى مختلف |

مثال Response:

```json
{
  "id": "b1eecfab-5601-44a5-a0da-23a4a709196b",
  "idempotencyKey": "client-operation-001",
  "requestType": "STANDARD",
  "payload": "{\"source\":\"client\"}",
  "status": "PENDING",
  "result": null,
  "errorMessage": null,
  "createdAt": "2026-08-31T10:00:00Z",
  "updatedAt": "2026-08-31T10:00:00Z",
  "completedAt": null,
  "version": 0
}
```

## قراءة طلب واحد

```http
GET /api/v1/requests/{id}
Authorization: Bearer <access-token>
```

يعيد المستخدم طلبه فقط. إذا كان ID غير موجود أو لا يخص المستخدم تعاد `404`، فلا يكشف النظام وجود طلب مستخدم آخر.

## عرض طلبات المستخدم

```http
GET /api/v1/requests?status=PENDING&page=0&size=20
Authorization: Bearer <access-token>
```

- `status`: اختياري ومن القيم `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`.
- `page`: يبدأ من صفر.
- `size`: من 1 إلى 100.
- الترتيب: `createdAt DESC`.

## واجهات المسؤول

```http
GET /api/v1/admin/requests?status=FAILED&page=0&size=20
GET /api/v1/admin/requests/{requestId}
Authorization: Bearer <admin-token>
```

تحتاج Role باسم `ADMIN`. يعيد المستخدم غير المخول `403 Forbidden`.

## Correlation ID

يقبل Gateway `X-Correlation-ID` صالحًا أو يولد قيمة جديدة، ويعيده في Response. يستخدم للربط بين Client وGateway وLogs، بينما يستخدم `TEST_RUN_ID` لتجميع تشغيل أداء كامل.
