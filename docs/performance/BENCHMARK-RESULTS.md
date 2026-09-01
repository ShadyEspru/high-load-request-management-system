# نتائج اختبارات الأداء

## معنى iterations/s في السكربت الحالي

يستخدم `baseline.js` المنفذ `constant-arrival-rate`. تنفذ `setup()` عملية Login مرة واحدة، ثم تنفذ `createOnly()` طلب `POST /api/v1/requests` واحدًا في كل Iteration. لذلك في هذا السكربت تحديدًا:

```text
iterations/s ≈ business request submissions/s
```

لا تنطبق المساواة على كل سكربتات k6. في `load.js` و`stress.js` قد تنفذ Iteration واحدة `POST` ثم `GET` وربما `LIST`؛ عندها لا تساوي `iterations/s` عدد HTTP Requests/s.

تعني `dropped_iterations` أن k6 لم يبدأ بعض الدورات في موعدها بسبب نقص VUs أو تشبع مولد الحمل. لا تعني أن HLRMS استقبل تلك الطلبات ثم فقدها.

## النتائج المسجلة

| التشغيل | النتيجة | التصنيف |
|---|---|---|
| Full E2E 125 RPS | `P95 = 389 ms` | أداء نظيف |
| Full E2E 250 RPS | `P95 = 628 ms` | PASS على بيئة التطوير المحلية |
| Full E2E 350 RPS | لا فقد للطلبات المقبولة، `P95 = 1.91 s` | Correctness PASS مع Latency متدهورة |
| Full E2E 500 RPS | `2488` طلبًا مقبولًا، `13` Dropped، `P95 = 2.29 s` | غير مستقر؛ تشبع Host المحلي |
| Worker Scaling | `30001/30001` processed، و`DLQ 4 → 4` | Correctness PASS؛ Scaling غير خطي |
| Preliminary Spike | `24871` accepted/processed، `631` Dropped، `0` HTTP failures | Integrity PASS؛ Load Generator متشبع |
| Soak 350، 30 دقيقة | Effective `347.20 RPS`، `624965` Iterations مسجلة، `0.29%` HTTP failures، `5038` Dropped، `P95 = 3.36 s` | تدهور طويل المدة؛ ليس Performance PASS نظيفًا |

هذه الأرقام مصنفة **Recorded Run** لأن المخرجات الخام الكاملة لكل تشغيل غير موجودة في المستودع بعد. توجد أدلة خام مستقلة لـ`P19-R10` الخاصة بـIdempotency وRedis Fallback داخل `docs/performance/results/P19-R10/`.

## Full E2E 500

وصل استهلاك CPU المجمع أثناء إحدى الذروات إلى نحو `789%` على بيئة ترى ثمانية Logical CPUs، أي بالقرب من سقف `800%`. كان k6 والنظام وقواعد البيانات وRabbitMQ يعملون على الجهاز نفسه. لذلك تقيس النتيجة سقف البيئة المحلية ولا تثبت سقف المعمارية.

أكملت جميع الطلبات المقبولة خط `Request → Outbox → RabbitMQ → Worker`، لكن وجود `13 dropped_iterations` يمنع تصنيف هدف 500 RPS بأنه مستقر.

## Worker Scaling

| Workers | Consumers | Gross msg/s | Active msg/s | Active Speedup | Efficiency |
|---:|---:|---:|---:|---:|---:|
| 1 | 4 | 474.71 | 595.83 | 1.000× | 100% |
| 2 | 8 | 478.88 | 632.40 | 1.061× | 53.1% |
| 4 | 16 | 388.67 | 588.33 | 0.987× | 24.7% |
| 8 | 32 | 254.83 | 421.84 | 0.708× | 8.9% |

تحافظ المنظومة على Correctness مع زيادة Workers، لكن Replicas داخل Host واحد تنافست على الموارد. ظهرت 84 وصلة PostgreSQL مع ثماني نسخ، منها 78 Idle و`idle in transaction=0`؛ لذلك لا يصح اتهام PostgreSQL وحدها. الاستنتاج الدقيق أن ميزانية الموارد المشتركة أصبحت قيدًا، وأن فصل Hosts ضروري قبل تقييم التوسع الحقيقي.

## Preliminary Spike

| المرحلة | Iterations المنفذة | Dropped | HTTP Failures | P95 |
|---|---:|---:|---:|---:|
| Baseline 350 | 6797 | 204 | 0 | 3.15 s |
| Spike 800 | 7718 | 282 | 0 | 2.92 s |
| Recovery 350 | 10356 | 145 | 0 | 2.15 s |
| الإجمالي | 24871 | 631 | 0 | 2.83 s overall |

سُجلت المطابقة:

```text
OUTBOX_DELTA = 24871
PUBLISHED_DELTA = 24871
PROCESSED_DELTA = 24871
DLQ = 4 → 4
PEAK_QUEUE = 1158
RECOVERY_MS = 5729
```

لكن السيناريو حجز نحو 3100 VUs مبدئيًا ووصل `vus_max` إلى 3447 على الجهاز نفسه. لذلك يثبت التشغيل سلامة الطلبات المقبولة، ولا يستخدم لقياس سعة Server وحده.

## حالة هدف 1000 RPS

هدف `1000 RPS` هو **Planned Validation** وليس نتيجة مثبتة. البيئة الصحيحة:

- Host A يشغّل k6 ويحفظ النتائج.
- Host B يشغّل HLRMS كاملًا داخل Docker.
- اتصال مباشر ضمن LAN.
- الهدف `http://<HOST_B_LAN_IP>:8088`.
- عدم استخدام ngrok في Benchmark.
- توثيق مواصفات الجهازين وعدد Replicas قبل التشغيل.

الارتفاع المقترح هو `350 → 500 → 750 → 1000 RPS` مع `preAllocatedVUs` معقولة و`maxVUs` يصل إلى 3000 عند الحاجة، لا حجز 3000 VUs منذ البداية.

لا يعتمد الهدف إلا إذا تحققت معًا:

1. لا توجد Dropped غير مفسرة من مولد الحمل.
2. `http_req_failed` ضمن الحد المتفق عليه.
3. Latency موثقة عند P95 وP99.
4. Accepted = Requests = Outbox = Published = Processed.
5. Outbox والطوابير تعود إلى الصفر بعد Recovery.
6. لا توجد زيادة غير متوقعة في DLQ.

## البيانات المطلوبة قبل النشر النهائي

- ملخص k6 النصي لكل تشغيل رئيسي.
- `JSON/CSV` أو `--summary-export`.
- صور Grafana أثناء تشغيل يحمل `TEST_RUN_ID` واضحًا، لا صور Idle.
- مواصفات Host A وHost B.
- أمر التشغيل وCommit Hash للسكربت.
- عدد Replicas وإعداد `WORKER_SIMULATED_DELAY_MS`.

بعد إضافتها يمكن تغيير تصنيف النتائج من Recorded Run إلى Raw Evidence Available.
