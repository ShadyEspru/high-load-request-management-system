# Threat Model

| التهديد | الخطر | التحكم الحالي | الإجراء المطلوب |
|---|---|---|---|
| JWT مزور أو منتهي | انتحال هوية | توقيع JWT والتحقق في Gateway | تدوير Secret واستخدام Key management |
| تزوير X-User headers | تجاوز الملكية/RBAC | Gateway يحذفها ويولدها | منع الوصول المباشر إلى Request Service |
| إعادة POST بسبب Network retry | تنفيذ مكرر | User-scoped Idempotency + DB constraint | إلزام Client بسياسة مفتاح صحيحة |
| Message redelivery | أثر مكرر | processed_events unique eventId | إبقاء Domain adapters Idempotent أيضًا |
| Payload ضخم | استنزاف ذاكرة/DB | حد 10000 محرف | إضافة Body limit عند Edge إذا تغير العقد |
| Credential stuffing | ضغط على Login | Password hashing ومقاييس Auth | Rate limit خاص بـAuth وLockout policy |
| Queue flooding | تراكم وارتفاع Latency | Durable queue ومراقبة | Admission control وCapacity alerts |
| كشف Actuator | تسريب تفاصيل | شبكة محلية حاليًا | Authentication/management network |
| أسرار افتراضية | سيطرة كاملة على الخدمات | Environment variables متاحة | منع التشغيل بقيم Default في Production |
| Public perf routes | تجاوز JWT/سياسات طبيعية | مخصصة للاختبار | Profile منفصل وتعطيلها افتراضيًا |
| ngrok exposure | تعرض Endpoint عام | HTTPS domain | تشغيله وقت العرض فقط وسياسة وصول واضحة |

## الأصول الحساسة

- Password hashes وRefresh Tokens.
- JWT signing secret.
- بيانات requests وpayload/result.
- RabbitMQ وRedis وPostgreSQL credentials.
- Grafana dashboard وOperational metrics.

## افتراضات النسخة الحالية

- Docker network داخل Host موثوقة.
- Port 18080 للاستخدام المحلي فقط.
- Demo integration لا يحمل بيانات حقيقية.
- Benchmark ينفذ في شبكة LAN موثوقة.
