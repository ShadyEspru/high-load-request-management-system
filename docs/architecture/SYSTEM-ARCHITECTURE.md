# المعمارية المنفذة لنظام HLRMS

## نطاق النظام

يفصل HLRMS **قبول الطلب** عن **تنفيذ العمل**. يستقبل `Request Service` قيمتي `requestType` و`payload` بصورة عامة، ثم يعيد `Request ID` وحالة قابلة للتتبع. يمكن ربط Mobile أو Web أو Partner System بالمنصة دون تغيير قلب إدارة الطلبات.

يتضمن المستودع `Demo Client` و`Demo Business API` لإظهار التكامل الوظيفي. كلاهما خارج حدود HLRMS Core وقابل للاستبدال.

## المكوّنات

| الطبقة | المكوّن | المسؤولية المنفذة |
|---|---|---|
| Entry | `API Gateway LB` | نقطة الدخول المنشورة على `:8088` وفحص صحة Gateway |
| Edge | `API Gateway` | `JWT Validation`، إزالة ترويسات الهوية الواردة، توليد ترويسات موثوقة، التوجيه، `Rate Limiting` و`Circuit Breaker` |
| Identity | `Auth Service` | `Register` و`Login` و`Refresh Token` مع قاعدة `hlrms_auth` |
| Admission | `Request Service LB` | توزيع `Round-Robin` على نسختين من `Request Service` |
| Admission | `Request Service × 2` | الملكية، `Idempotency`، حفظ الطلب وOutbox، والاستعلام |
| Publication | `Outbox Publisher × 1` | مطالبة دفعات الأحداث ونشرها وتسجيل `Publisher Confirms` |
| Messaging | `RabbitMQ` | Direct Exchange، طابور متين، إعادة تسليم، وDLQ |
| Execution | `Request Worker × N` | تسجيل `eventId` مرة واحدة، التنفيذ، وتحديث الحالة النهائية |
| State | `PostgreSQL` | قواعد الهوية والطلبات والأحداث المتينة |
| Fast Path | `Redis` | Idempotency Replay، Distributed Lock، Request Cache وGateway Rate Limiter |
| Observability | `Prometheus + Grafana` | جمع مقاييس التطبيق والبنية وعرضها |

## قبول الطلب

1. يرسل العميل `POST /api/v1/requests` مع `Bearer JWT` و`Idempotency-Key`.
2. يتحقق Gateway من الرمز، ويحذف أي `X-User-*` قادمة من العميل، ثم يولدها من Claims الموثوقة.
3. يوزع HAProxy الطلب على إحدى نسختي Request Service.
4. تبني الخدمة بصمة من `requestType + payload` ضمن نطاق المستخدم.
5. يعيد Redis النتيجة السابقة إذا تطابق المفتاح والمحتوى. استخدام المفتاح نفسه لمحتوى مختلف يعيد `409 Conflict`.
6. يحفظ PostgreSQL صف الطلب وحدث Outbox في Transaction واحدة، وتصبح الحالة `PENDING`.
7. يعاد `201 Created`. إعادة الطلب نفسه تعيد `200 OK` مع `Idempotency-Replayed: true`.

## المعالجة غير المتزامنة

1. يطالب Outbox Publisher بالأحداث المعلقة على دفعات.
2. ينشر `REQUEST_CREATED` إلى `hlrms.request.exchange` باستخدام `request.created`.
3. بعد Publisher Confirm تصبح حالة الحدث `PUBLISHED`.
4. يستهلك Worker الرسالة من `hlrms.request.processing.queue`.
5. يسجل Worker `eventId` في `processed_events`. إذا كان موجودًا يتجاهل الحدث المكرر.
6. تتحول الحالة من `PENDING` إلى `PROCESSING` ثم `COMPLETED` أو `FAILED`.
7. بعد استنفاد Retry يعلّم Message Recoverer الطلب `FAILED` ويرسل الرسالة إلى `hlrms.request.processing.dlq`.

## ضمان الاتساق

- `Transactional Outbox` يمنع حفظ طلب دون حدث قابل للنشر.
- `Idempotent Consumer` يمنع تنفيذ الحدث نفسه مرتين بعد Redelivery.

قاعدة التوفيق بعد الاختبار:

```text
accepted requests
= persisted requests
= outbox events
= published events
= processed events
```

ويجب أن ينتهي التشغيل دون تراكم دائم في Outbox أو Ready/Unacked Messages، ودون زيادة غير متوقعة في DLQ.

## التوازي

إعداد Listener في كل Worker هو `concurrency=1` و`max-concurrency=4` و`prefetch=10`؛ لذلك قد تظهر أربع Consumers لكل نسخة تحت الحمل. يدعم `docker-compose.scaling-base.yml` زيادة Worker Replicas.

أثبت اختبار `1 → 2 → 4 → 8` صحة معالجة جميع الرسائل، لكنه لم يحقق Scaling خطيًا على Host واحد بسبب التنافس على CPU واتصالات PostgreSQL وبقية الموارد المشتركة.

## سلوك الأعطال

| العطل | السلوك |
|---|---|
| RabbitMQ غير متاح | يستمر قبول الطلب إذا كانت PostgreSQL متاحة؛ تتراكم Outbox ثم تُنشر بعد عودة Broker |
| Redis غير متاح | يبقى الاتساق محميًا بالقيد الفريد ومسار PostgreSQL؛ يتباطأ Fast Path |
| Worker متوقف | تبقى الرسائل أو يعاد تسليمها إلى Consumer متاح |
| فشل التنفيذ بعد Retry | يصبح الطلب `FAILED` وتنتقل الرسالة إلى DLQ |
| PostgreSQL غير متاح | تتوقف عمليات القبول المتينة وتفشل Readiness ثم تتعافى الخدمات بعد عودة القاعدة |

## قيود النسخة الحالية

- يعرّف `request-service.cfg` نسختين محددتين؛ زيادة العدد تتطلب تحديث Service Discovery أو HAProxy.
- يعرّف `api-gateway.cfg` Gateway واحدًا رغم وجود Load Balancer أمامه.
- يجمع Prometheus مقاييس Request Service عبر Load Balancer، ولا يميز كل Replica بشكل مضمون.
- استهداف `request-worker:8082` لا يجمع كل Worker Replica بشكل مستقل عند التوسع.
- كلمات المرور الافتراضية للتطوير فقط وليست Production Configuration.
- Demo Business Adapter مثال تكامل، وليس جزءًا لازمًا من HLRMS.

## بنية اختبار 1000 RPS

يعمل HLRMS كاملًا على Host مستقل، ويعمل k6 على Host آخر ضمن LAN نفسها. يستخدم الاختبار عنوان `http://<HOST_B_LAN_IP>:8088` ولا يستخدم ngrok، لأن أي وسيط خارجي غير مضبوط قد يصبح عنق زجاجة ويشوّه القياس.
