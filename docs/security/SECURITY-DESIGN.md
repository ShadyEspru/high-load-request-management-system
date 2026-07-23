# التصميم الأمني لنظام HLRMS
## Security Design

# 1. الهدف

تحدد هذه الوثيقة الضوابط الأمنية الأساسية لنظام HLRMS، بهدف حماية واجهات API وبيانات الطلبات والحسابات والبنية التحتية والسجلات والأسرار.

# 2. النطاق

يشمل التصميم:

- Android Application
- API Gateway
- Authentication and Authorization
- Request Service
- Worker Services
- RabbitMQ
- PostgreSQL
- Redis
- Monitoring Stack
- Docker Deployment

# 3. المبادئ الأمنية

## 3.1 أقل صلاحية

يمنح كل مستخدم أو خدمة أقل قدر ممكن من الصلاحيات اللازمة.

## 3.2 الرفض الافتراضي

أي Endpoint أو وظيفة غير مصرح بها صراحة تعتبر مرفوضة.

## 3.3 الدفاع متعدد الطبقات

يستخدم النظام المصادقة والتفويض والتحقق من المدخلات وRate Limiting والعزل الشبكي والمراقبة وسجلات التدقيق وحماية الأسرار.

## 3.4 عدم الثقة بالمدخلات

يجب التحقق من جميع المدخلات القادمة من Android والأنظمة العميلة وHeaders وQuery Parameters ورسائل RabbitMQ وتحديثات الإعدادات.

# 4. المصادقة

## 4.1 Bearer JWT

```http
Authorization: Bearer <access-token>
```

## 4.2 Claims المقترحة

```json
{
  "sub": "user-or-client-id",
  "iss": "hlrms-auth-service",
  "aud": "hlrms-api",
  "roles": ["CLIENT"],
  "clientSystemId": "5af67110-4b19-4f4b-89cc-94f6d32f9d2c",
  "jti": "c63b81ee-cf11-4c62-9076-7343698cb684",
  "iat": 1784791200,
  "exp": 1784792100
}
```

## 4.3 قواعد JWT

- مدة Access Token قصيرة.
- التحقق من `iss` و`aud` و`exp`.
- رفض الخوارزمية غير المتوقعة.
- عدم قبول `alg=none`.
- دعم تدوير مفاتيح التوقيع.
- عدم تسجيل الرمز كاملًا في Logs.

# 5. التفويض

الأدوار الأساسية:

```text
CLIENT
ADMIN
MONITORING
```

لا يكفي التحقق من الدور فقط؛ يجب أيضًا التحقق من ملكية المورد:

```text
request.clientSystemId == token.clientSystemId
```

واجهات `/api/v1/admin/**` مقيدة بدور `ADMIN`.

# 6. حماية API

## TLS

في الإنتاج يستخدم HTTPS فقط، ولا تقبل الاتصالات غير المشفرة للواجهات العامة.

## Rate Limiting

يطبق حسب Client System وUser ID وEndpoint. عند التجاوز:

```http
429 Too Many Requests
Retry-After: 30
```

## Idempotency

`POST /api/v1/requests` يحتاج `Idempotency-Key`. يربط المفتاح بـClient System وRequest Hash وRequest ID وExpiration Time.

## Correlation ID

يقبل النظام `X-Correlation-ID` بعد التحقق من الطول والمحارف، أو ينشئ قيمة جديدة.

# 7. التحقق من المدخلات

- استخدام DTOs محددة.
- تحديد أقصى طول لكل String.
- تحديد أقصى حجم وعمق وعدد مفاتيح للـPayload.
- تحديد قيم Enum المسموحة.
- تطبيق Validation خاص حسب `requestType`.
- استخدام ORM أو Prepared Statements.
- منع تمرير مدخلات المستخدم مباشرة إلى SQL أو أوامر النظام.
- عدم استخدام مدخلات المستخدم كأسماء Exchanges أو Queues.

# 8. أمان RabbitMQ

الحسابات المقترحة:

```text
hlrms-producer
hlrms-worker
hlrms-monitoring
hlrms-admin
```

الصلاحيات:

- Producer يكتب فقط على Exchanges المطلوبة.
- Worker يقرأ من Main Queues ويكتب على Retry وDLX.
- Monitoring يقرأ المقاييس فقط.
- Admin مقيد للاستخدام التشغيلي.

ضوابط إضافية:

- Virtual Host باسم `/hlrms`.
- عدم استخدام المستخدم الافتراضي في الإنتاج.
- عدم كشف Management UI للعامة.
- عدم وضع Tokens أو أسرار داخل الرسائل.
- التحقق من Message Schema.
- تقييد حجم الرسالة.

# 9. أمان PostgreSQL وRedis

## PostgreSQL

- حساب تطبيق محدود.
- حساب Migrations منفصل إن أمكن.
- عدم استخدام Superuser.
- تشفير النسخ الاحتياطية.
- تقييد الوصول بالشبكة.
- عدم تسجيل SQL الحساس.

## Redis

- عدم كشفه للعامة.
- تفعيل Authentication وACLs.
- استخدام TTL.
- عدم اعتباره مصدر الحقيقة النهائي.
- عدم تخزين Tokens خام دون حاجة.

# 10. إدارة الأسرار

تشمل الأسرار كلمات مرور قواعد البيانات وRabbitMQ ومفاتيح JWT وAPI Keys وكلمات مرور Grafana وSMTP.

القواعد:

- لا تحفظ في Git.
- لا توضع داخل Docker Image.
- تمرر عبر Environment Variables أو Secret Manager.
- تدوّر دوريًا.
- تستخدم قيم مختلفة لكل بيئة.

# 11. السجلات

يسجل النظام:

- Correlation ID
- Request ID
- Client ID
- Endpoint
- HTTP Status
- Duration
- Error Code
- Worker ID
- Attempt Number

ولا يسجل:

- Access Tokens
- Refresh Tokens
- Passwords
- Authorization Header
- Connection Strings
- Secret Keys
- Payload كاملًا إذا كان حساسًا

# 12. Audit Logging

تسجل العمليات الحساسة، مثل:

- تسجيل الدخول الإداري.
- تعديل Configuration.
- إعادة معالجة DLQ.
- تعديل Retry Policy.
- تغيير الصلاحيات.
- تدوير الأسرار.

الحقول المقترحة:

```text
actorId
actorType
action
resourceType
resourceId
oldValue
newValue
timestamp
correlationId
sourceIp
result
```

# 13. Android Security

- حفظ Access Token باستخدام Android Keystore أو تخزين آمن.
- عدم تضمين أسرار ثابتة داخل APK.
- تعطيل Cleartext Traffic.
- عدم تسجيل Tokens في Logcat.
- التعامل مع انتهاء الجلسة.
- استخدام Network Security Configuration.

# 14. Actuator وMonitoring

لا تكشف `/actuator/prometheus` للعامة. يفضل الوصول إليه من شبكة داخلية أو عبر Reverse Proxy أو mTLS. كما يجب عدم كشف Endpoints حساسة مثل `env` و`beans` و`mappings` للعامة.

# 15. الأخطاء الآمنة

يجب إعادة رسالة آمنة دون Stack Trace أو SQL أو Hostnames داخلية:

```json
{
  "code": "INTERNAL_ERROR",
  "message": "حدث خطأ غير متوقع.",
  "correlationId": "..."
}
```

# 16. منع إساءة الاستخدام

- حدود حجم الطلب.
- Timeouts.
- Queue Limits.
- Consumer Prefetch.
- Circuit Breaker.
- Rate Limiting.
- مراقبة Queue Backlog.
- منع Retry غير المحدود.

# 17. الاستجابة للحوادث

1. اكتشاف الحادث.
2. تحديد النطاق.
3. احتواء الضرر.
4. تدوير الأسرار.
5. حفظ الأدلة.
6. إصلاح السبب.
7. استعادة الخدمة.
8. توثيق الدروس المستفادة.

# 18. تعريف الانتهاء الأمني

لا تعد الميزة مكتملة قبل تطبيق Authentication وAuthorization وValidation وError Handling الآمن، ووجود اختبارات للصلاحيات، وعدم وجود أسرار في المستودع، وتحديث OpenAPI وThreat Model.
