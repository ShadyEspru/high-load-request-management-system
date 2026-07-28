# قائمة التحقق الأمنية
## Security Checklist

# قبل التطوير

- [ ] تحديد الأدوار والصلاحيات.
- [ ] تحديد مصدر الهوية.
- [ ] اعتماد طريقة توقيع JWT.
- [ ] تحديد مدة Access Token.
- [ ] تحديد سياسة الأسرار.
- [ ] تحديد حدود Payload وRate Limit.
- [ ] اعتماد Error Format وCorrelation ID وIdempotency Policy.

# أثناء تطوير API

- [ ] كل Endpoint محمي افتراضيًا.
- [ ] Authorization على مستوى المورد.
- [ ] Validation لجميع DTOs.
- [ ] لا توجد SQL مبنية مباشرة.
- [ ] لا يعاد Stack Trace.
- [ ] لا تسجل Tokens أو Passwords.
- [ ] توجد اختبارات 401 و403 و404 وIDOR.
- [ ] يوجد Rate Limiting وحد لحجم الطلب.

# RabbitMQ

- [ ] Virtual Host منفصل.
- [ ] حسابات Producer وWorker محدودة.
- [ ] Management UI غير مكشوف للعامة.
- [ ] Schema Validation للرسائل.
- [ ] لا توجد أسرار داخل الرسائل.
- [ ] Retry محدود وDLQ مفعلة.
- [ ] Queue Depth مراقبة.

# PostgreSQL و Redis

- [ ] الحسابات ليست Superuser.
- [ ] المنافذ غير مكشوفة للعامة.
- [ ] كلمات المرور خارج Git.
- [ ] النسخ الاحتياطية محمية.
- [ ] Redis يحتاج Authentication وTTL.

# Android

- [ ] تعطيل Cleartext Traffic.
- [ ] حفظ Token بأمان.
- [ ] عدم وضع أسرار داخل APK.
- [ ] عدم تسجيل Token في Logcat.
- [ ] التعامل مع انتهاء الجلسة.
- [ ] احترام Retry-After.
- [ ] استخدام Idempotency-Key عند إعادة الطلب.

# قبل Pull Request

- [ ] تشغيل Unit وIntegration Tests.
- [ ] مراجعة Dependencies.
- [ ] فحص Secrets.
- [ ] تحديث OpenAPI وError Catalog.
- [ ] تحديث Threat Model عند الحاجة.
- [ ] مراجعة Logs والصلاحيات الجديدة.

# قبل النشر

- [ ] HTTPS مفعل.
- [ ] Default Credentials محذوفة.
- [ ] أسرار الإنتاج مختلفة.
- [ ] Actuator وGrafana وRabbitMQ UI مقيدة.
- [ ] تنبيهات الفشل وDLQ مفعلة.
- [ ] النسخ الاحتياطي مجرب.
- [ ] خطة تدوير الأسرار والاستجابة للحوادث جاهزة.
- [ ] اختبار Load وSecurity مكتمل.
