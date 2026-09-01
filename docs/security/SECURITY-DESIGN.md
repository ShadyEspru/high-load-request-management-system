# التصميم الأمني

## الهوية

- يسجل Auth Service المستخدم ويخزن Password Hash باستخدام BCrypt.
- يصدر Access Token قصير العمر وRefresh Token.
- يتحقق API Gateway من توقيع JWT وانتهائه وClaims المطلوبة.
- يحذف Gateway أي `X-User-Id`, `X-User-Email`, `X-User-Roles` قادمة من Client ثم يعيد توليدها.

## حدود الثقة

```text
Untrusted Client
  → API Gateway LB
  → API Gateway (JWT trust boundary)
  → Internal Services (trusted headers)
  → PostgreSQL / Redis / RabbitMQ
```

Request Service يعتمد على Trusted Headers ولا يعيد التحقق من JWT. لذلك يجب ألا يكون منفذه الداخلي متاحًا لعميل غير موثوق.

## التفويض

- User endpoints تقيد القراءة بـ`user_id` المستخرج من Gateway.
- Admin endpoints تستدعي `requireAdmin()`.
- عدم ملكية Request يعاد كـ404 بدل كشف وجوده.
- Demo Business API منفصل ويطبق JWT/secret بحسب المسار.

## حماية التكرار

Idempotency-Key لا يعد سرًا، لكنه يمنع إعادة تنفيذ العملية المنطقية عند Retry. يضم المفتاح الفعلي User ID، وتحسم Unique Constraint في PostgreSQL السباقات بين Replicas. Redis مسار سريع وليس مصدر الاتساق الوحيد.

## إدارة الأسرار

يجب تمرير القيم الآتية عبر `.env` أو Secret Store:

- `JWT_SECRET`
- PostgreSQL credentials
- Redis password
- RabbitMQ credentials
- Grafana admin password
- Demo internal secret

القيم الافتراضية في compose للتطوير فقط ويجب تغييرها قبل أي نشر مشترك.

## Rate Limiting وResilience

- يطبق Gateway Rate Limiter على GET requests وAdmin routes وفق User ID.
- POST الأساسي لا يحمل Rate Limiter في الإعداد الحالي حتى يسمح بBenchmark القبول؛ يلزم Policy مناسبة قبل Production.
- Circuit Breaker وRetry مطبقان على مسارات القراءة وبعض مسارات الاختبار.

## نقاط تحتاج تقييدًا

1. `request-service-lb` منشور على `:18080` للتشخيص. الوصول الخارجي إليه قد يسمح بتزوير Trusted Headers؛ يجب ربطه بـlocalhost أو Firewall في أي بيئة غير محلية.
2. `/api/v1/perf/**` مصنف Public في Gateway ومخصص للاختبار فقط. يجب تعطيله أو حمايته في Production.
3. Actuator details ظاهرة في إعداد التطوير؛ يفضل فصل Management Network وتقييد التفاصيل.
4. اتصال الخدمات داخل Docker غير مشفر. عند الانتقال إلى Hosts متعددة يلزم TLS أو شبكة خاصة موثوقة.
5. CORS الافتراضي يسمح `http://localhost:3000` فقط؛ يجب تحديد Origin حقيقي بدقة.

## Logging

- يمر Correlation ID عبر Gateway لتتبع الطلب.
- لا تسجل كلمات المرور أو Tokens أو Payload حساس.
- يخفي Android logging ترويسة Authorization.
- يجب تجنب DEBUG تحت الحمل لأنه يرفع I/O ويشوّه Benchmark.
