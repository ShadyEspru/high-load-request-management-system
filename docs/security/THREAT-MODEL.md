# نموذج التهديدات لنظام HLRMS
## Threat Model

# 1. المنهج

يستخدم النموذج تصنيف `STRIDE`:

- Spoofing
- Tampering
- Repudiation
- Information Disclosure
- Denial of Service
- Elevation of Privilege

# 2. الأصول الحساسة

| الأصل | الأهمية |
|---|---|
| Access Tokens | حرجة |
| JWT Signing Keys | حرجة |
| بيانات الطلبات | عالية |
| نتائج المعالجة | عالية |
| RabbitMQ Credentials | حرجة |
| Database Credentials | حرجة |
| Configuration Values | عالية |
| Audit Logs | عالية |
| Monitoring Data | متوسطة إلى عالية |

# 3. حدود الثقة

1. بين Android والـAPI Gateway.
2. بين API Gateway والخدمات الداخلية.
3. بين الخدمات وRabbitMQ.
4. بين الخدمات وPostgreSQL.
5. بين الخدمات وRedis.
6. بين Prometheus والمكونات.
7. بين المسؤول وواجهات الإدارة.

# 4. التهديدات الرئيسية

## T-01 سرقة Token

**الفئة:** Spoofing  
**التخفيف:** Access Token قصير، TLS، التحقق من Issuer وAudience، تدوير المفاتيح، وإلغاء الجلسات المشبوهة.

## T-02 الوصول إلى طلب عميل آخر

**الفئة:** Elevation of Privilege / Information Disclosure  
**التخفيف:** فحص `clientSystemId` واختبارات Authorization وعدم الاعتماد على Request ID وحده.

## T-03 تعديل Payload أثناء النقل

**الفئة:** Tampering  
**التخفيف:** HTTPS، Validation، Hash للـIdempotency، وتوقيع JWT.

## T-04 إعادة إرسال الطلب

**الفئة:** Tampering / Denial of Service  
**التخفيف:** `Idempotency-Key` وRate Limiting وكشف التكرار.

## T-05 SQL Injection

**الفئة:** Tampering / Information Disclosure  
**التخفيف:** ORM وPrepared Statements وحساب قاعدة بيانات محدود.

## T-06 رسالة RabbitMQ مزورة أو تالفة

**الفئة:** Spoofing / Tampering  
**التخفيف:** صلاحيات منفصلة وSchema Validation وVirtual Host ورفض الرسائل غير الصالحة إلى DLQ.

## T-07 Retry Loop غير محدود

**الفئة:** Denial of Service  
**التخفيف:** Maximum Attempts وBackoff وDLQ وتنبيهات Retry.

## T-08 إغراق الطوابير

**الفئة:** Denial of Service  
**التخفيف:** Rate Limiting وQueue Limits وBackpressure ومراقبة Queue Depth.

## T-09 تسرب الأسرار

**الفئة:** Information Disclosure  
**التخفيف:** Secret Manager وSecret Scanning وLog Redaction وتدوير الأسرار.

## T-10 إساءة استخدام الإدارة

**الفئة:** Elevation of Privilege / Repudiation  
**التخفيف:** ADMIN Role وAudit Logs وشبكة داخلية وSession Timeout.

## T-11 كشف Actuator

**الفئة:** Information Disclosure  
**التخفيف:** تقييد الشبكة، حماية Prometheus، وعدم كشف Endpoints الحساسة.

## T-12 التلاعب بالسجلات

**الفئة:** Repudiation / Tampering  
**التخفيف:** Centralized Logging وصلاحيات كتابة محدودة وRetention Policy وCorrelation ID.

# 5. جدول المخاطر

| التهديد | الاحتمال | التأثير | المستوى |
|---|---|---|---|
| سرقة Token | متوسط | عالٍ | عالٍ |
| تجاوز صلاحية المورد | متوسط | عالٍ | عالٍ |
| إغراق Queue | عالٍ | عالٍ | حرج |
| تسرب الأسرار | متوسط | حرج | حرج |
| Retry Loop | متوسط | عالٍ | عالٍ |
| SQL Injection | منخفض مع ORM | حرج | عالٍ |
| كشف Metrics | متوسط | متوسط | متوسط |

# 6. اختبارات أمنية مطلوبة

- Endpoint دون Token.
- Token منتهي أو بتوقيع غير صحيح.
- Token بدور غير كافٍ.
- Client يطلب موردًا لعميل آخر.
- تكرار Idempotency Key مع Payload مختلف.
- Payload أكبر من الحد.
- Rate Limit.
- رسالة RabbitMQ غير مطابقة للـSchema.
- تجاوز Maximum Retry Attempts.
