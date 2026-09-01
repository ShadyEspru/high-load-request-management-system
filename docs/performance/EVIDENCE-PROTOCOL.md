# بروتوكول إثبات اختبار الحمل

## الهدف

يربط البروتوكول ما يرسله k6 بما تحفظه المنظومة وما تعرضه Grafana، حتى لا يعتمد إثبات آلاف الطلبات على لقطة شاشة أو رقم منفرد.

## TEST_RUN_ID يختاره الحضور

قبل الاختبار ينشأ معرّف يتضمن وقت UTC ولاحقة عشوائية يختارها أحد الحضور:

```text
DEFENSE-20260831T103500Z-4827
```

يظهر المعرّف في شاشة k6، ويدخل داخل Payload، ويستخدم في الاستعلامات وأسماء النتائج واللقطات. هذا يجعل تحضير تسجيل مزيف سابقًا غير عملي.

## قبل التشغيل

تسجل المعلومات الآتية:

- Commit Hash واسم سكربت k6.
- أمر التشغيل كاملًا.
- عنوان Host B ضمن LAN.
- مواصفات Host A وHost B.
- عدد نسخ Gateway وRequest Service وPublisher وWorker.
- `WORKER_SIMULATED_DELAY_MS` وقيم `preAllocatedVUs/maxVUs`.
- العدادات الابتدائية في requests وoutbox_events وprocessed_events وDLQ.

## أثناء التشغيل

تظهر الأدلة الآتية حيًا:

1. k6: Target Rate وIterations وDropped وLatency وFailures.
2. Grafana: Gateway RPS وP95 و5xx وQueue Depth وWorker Completion وCPU.
3. RabbitMQ: Ready وUnacked وConsumers وDLQ.
4. Docker Resources: لتحديد ما إذا كان Host نفسه متشبعًا.

صور Grafana أثناء Idle توثق إعداد Dashboard فقط، ولا تثبت أداء النظام تحت الحمل.

## بعد التشغيل

يترك النظام حتى نهاية Recovery، ثم تقارن الأعداد المرتبطة بـTEST_RUN_ID:

```text
accepted requests
= request rows
= outbox rows
= published outbox rows
= processed event rows
```

ثم تتحقق النهاية:

```text
pending outbox = 0
processing outbox = 0
rabbit ready = 0
rabbit unacked = 0
unexpected DLQ delta = 0
```

إذا كانت طلبات FAILED بسبب Failure Injection مخطط، تسجل منفصلة ولا تخلط مع فقد البيانات.

## إثبات المسار عبر التطبيق

يستخدم Demo Client لإثبات مسار وظيفي واحد حقيقي:

```text
Login → POST request → Request ID → Poll status → COMPLETED/FAILED
```

يستخدم k6 واجهات Auth وRequests نفسها، وبنية Payload عامة، وIdempotency-Key فريدًا لكل Iteration. لذلك يمثل آلاف النسخ البرمجية من العميل؛ لا حاجة إلى فتح آلاف شاشات Android.

تسلسل العرض المقنع:

1. ينشئ Demo Client طلبًا واحدًا ويظهر Request ID والحالة النهائية.
2. يختار أحد الحضور لاحقة TEST_RUN_ID.
3. يبدأ k6 وتظهر الزيادة في Grafana وRabbitMQ.
4. بعد Recovery تظهر أعداد قاعدة البيانات المتطابقة.
5. تحفظ المخرجات واللقطات باسم TEST_RUN_ID نفسه.

## ملفات كل تشغيل

```text
performance-evidence/
└── <TEST_RUN_ID>/
    ├── command.txt
    ├── environment.md
    ├── k6-summary.json
    ├── k6-terminal.txt
    ├── reconciliation.txt
    ├── docker-stats.csv
    └── screenshots/
        ├── 01-k6-live.png
        ├── 02-grafana-overview.png
        ├── 03-rabbitmq.png
        └── 04-final-drain.png
```

## قاعدة صياغة الادعاء

- `k6 executed N iterations`: دليل مولد الحمل فقط.
- `HLRMS accepted N requests`: يحتاج دليل API أو قاعدة البيانات.
- `N accepted requests were processed`: يحتاج توفيق Requests وOutbox وProcessed Events.
- `Target RPS was sustained`: يحتاج Thresholds ناجحة وعدم وجود Dropped غير مفسرة طوال النافذة.
