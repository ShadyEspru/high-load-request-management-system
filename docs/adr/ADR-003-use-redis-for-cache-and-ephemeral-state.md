# ADR-003: استخدام Redis للحالة السريعة والمؤقتة

- **Status:** Accepted and Implemented

## القرار

يستخدم Redis 8 في وظائف لا تستبدل الحالة المتينة:

- Idempotency replay records.
- Distributed locks بين Request Service Replicas.
- Cache للحالات النهائية.
- Gateway rate limiter state في Redis database منفصلة.

## TTLs الافتراضية

- Idempotency: 24 ساعة.
- Lock: 10 ثوانٍ.
- Request cache: 30 دقيقة.

## مبدأ الفشل الآمن

إذا تعذر Redis، يبقى Unique Constraint ومسار PostgreSQL مصدر الاتساق. قد يتراجع الأداء أو Fast Path، لكن لا يجوز أن يؤدي تعطل Redis إلى تنفيذ طلب مقبول مرتين.

## النتيجة

Redis تحسين للكمون والتنسيق المؤقت، وليس System of Record. لذلك يجب اختبار Redis outage وDatabase fallback بصورة مستقلة.
