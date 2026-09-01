# تكامل تطبيق أو موقع مع HLRMS

## المبدأ

يتعامل العميل مع HLRMS كمنصة عامة لقبول العمل وتتبع حالته. لا يحتاج التطبيق إلى معرفة RabbitMQ أو Outbox أو Worker، ولا يحتاج HLRMS إلى معرفة واجهة المستخدم أو مجال الأعمال.

أي Client قابل للاستبدال إذا استطاع:

1. الحصول على JWT.
2. إرسال `requestType` و`payload` مع `Idempotency-Key`.
3. حفظ Request ID المعاد.
4. الاستعلام حتى الحالة النهائية.
5. عرض `result` أو `errorMessage`.

## العقد الوظيفي

### تسجيل الدخول

```http
POST /api/v1/auth/login
Content-Type: application/json
```

### إرسال طلب

```http
POST /api/v1/requests
Authorization: Bearer <access-token>
Idempotency-Key: <unique-key>
Content-Type: application/json
```

```json
{
  "requestType": "GENERIC_OPERATION",
  "payload": "{\"source\":\"demo-client\",\"data\":{}}"
}
```

تعيد العملية الأولى `201 Created`. إعادة العملية نفسها بالمفتاح والمحتوى نفسيهما تعيد `200 OK` مع `Idempotency-Replayed: true`. إعادة المفتاح نفسه لمحتوى مختلف تعيد `409 Conflict`.

### تتبع الحالة

```http
GET /api/v1/requests/{requestId}
Authorization: Bearer <access-token>
```

الحالات المنفذة: `PENDING` و`PROCESSING` و`COMPLETED` و`FAILED`.

## Demo Client الموجود

يطبق Android Demo Client المسار نفسه باستخدام Retrofit:

- `RequestApi.createRequest()` يرسل POST مع JWT وIdempotency-Key.
- `RequestRepository` ينشئ المفتاح ويحفظ Request ID.
- شاشة الحالة تستدعي `getRequestById()` كل ثانية حتى `COMPLETED` أو `FAILED`.
- `ApiClient` يستخدم عنوان ngrok الثابت للوصول الوظيفي من الهاتف.

هذه الطبقة ليست جزءًا من HLRMS Core. يمكن استبدالها بتطبيق نتائج امتحانات، بوابة تسجيل، نظام حجوزات، أو أي Client آخر دون تغيير قلب إدارة الطلبات.

## العرض الوظيفي

يعرض أمام اللجنة:

1. تسجيل الدخول.
2. إرسال طلب واحد من Demo Client.
3. ظهور Request ID فور القبول.
4. انتقال الحالة إلى الحالة النهائية.
5. ظهور الزيادة في Grafana وصف الطلب داخل PostgreSQL.

يثبت ذلك أن تطبيقًا حقيقيًا يستخدم المسار الكامل، لكنه لا يثبت السعة العالية وحده.

## آلاف العملاء

لا ترسل آلاف الطلبات بالضغط اليدوي على Android. يستخدم k6 العقد نفسه، وينشئ مفتاحًا وPayload فريدين لكل Iteration. يمثل ذلك آلاف العملاء البرمجيين.

الدليل الكامل يجمع بين:

- Demo Client لمسار واحد ظاهر.
- k6 للحمل المنضبط.
- Grafana وRabbitMQ للمراقبة الحية.
- PostgreSQL Reconciliation لإثبات أن المقبول حُفظ وعولج.

## اعتبارات التكامل

- يبقى Idempotency-Key ثابتًا طوال محاولات إعادة العملية المنطقية نفسها.
- لا يعاد استخدام المفتاح لعملية مختلفة.
- يفضل Backoff عند Polling، أو Push/WebSocket مستقبلًا للطلبات الطويلة.
- لا يرسل العميل `X-User-*`؛ Gateway يحذفها ويولدها من JWT.
- يستخدم ngrok للعرض الوظيفي فقط، ويتصل k6 مباشرة بعنوان LAN في Benchmark.
