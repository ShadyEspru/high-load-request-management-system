# تشغيل k6

## Baseline Arrival Rate

`baseline.js` ينفذ Login مرة واحدة في setup ثم POST واحدًا لكل Iteration.

```bash
k6 run \
  -e BASE_URL=http://<HLRMS_HOST>:8088 \
  -e AUTH_TOKEN=<TOKEN> \
  -e TEST_RUN_ID=DEFENSE-<UTC>-<RANDOM> \
  -e BASELINE_RATE=350 \
  -e BASELINE_DURATION=30s \
  -e BASELINE_VUS=500 \
  --summary-export=k6-summary.json \
  tests/performance/k6/baseline.js
```

في Windows PowerShell يستخدم Backtick بدل Backslash أو يوضع الأمر في سطر واحد.

## المتغيرات

| المتغير | الغرض |
|---|---|
| `BASE_URL` | عنوان Gateway LB |
| `AUTH_TOKEN` | Access Token جاهز؛ يلغي Login ببيانات المستخدم |
| `TEST_USERNAME`, `TEST_PASSWORD` | بديل AUTH_TOKEN |
| `TEST_RUN_ID` | ربط كل الطلبات والأدلة |
| `BASELINE_RATE` | Iterations المطلوبة في الثانية |
| `BASELINE_DURATION` | مدة السيناريو |
| `BASELINE_VUS` | VUs المحجوزة مبدئيًا |

## المقاييس المهمة

| المقياس | التفسير |
|---|---|
| `iterations` | عدد دورات k6 المكتملة |
| `http_reqs` | عدد HTTP Requests الفعلية |
| `http_req_duration` | Latency من جهة Client |
| `http_req_failed` | فشل HTTP وفق k6 |
| `dropped_iterations` | دورات لم تبدأ في موعدها |
| `checks` | تحقق Status وJSON ووجود Request ID |
| `business_error_rate` | فشل تحقق الأعمال داخل السكربت |

## اختيار السكربت

- `smoke.js`: فحص سريع قبل أي Benchmark.
- `baseline.js`: Arrival rate ثابت لإنشاء الطلبات.
- `load.js`: VUs متدرجة مع POST وGET وLIST.
- `stress.js`: رفع VUs لاكتشاف نقطة التدهور.
- `spike.js`: ارتفاع مفاجئ ثم Recovery.
- `soak.js`: تشغيل طويل لاكتشاف التدهور والتسرب.
- `recovery.js`: فحص عودة النظام بعد العطل.

## منع قياس خاطئ

- لا تشغل k6 على Host النظام في Final Benchmark.
- لا تستخدم ngrok.
- لا تساوِ Iterations/s مع HTTP RPS إلا بعد قراءة دالة السيناريو.
- لا تصف Dropped بأنها Requests مفقودة من Server.
- لا تعتمد النتيجة قبل Reconciliation ونهاية Queue drain.
