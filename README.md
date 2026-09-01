# نظام إدارة الطلبات عالية الحمل

**High-Load Request Management System — HLRMS**

منصة عامة لاستقبال عدد كبير من الطلبات، قبولها بسرعة وأمان، ثم تنفيذها بصورة غير متزامنة مع التتبع، ومنع التكرار، والاستعادة بعد الأعطال، والمراقبة التشغيلية.

لا يرتبط HLRMS بتطبيق أو مجال أعمال محدد. التطبيق الموجود في `android-app/` هو **Demo Client** قابل للاستبدال، أُنشئ لإثبات التكامل الوظيفي فقط. يمكن لأي تطبيق Mobile أو Web أو نظام Partner استخدام واجهات المنصة نفسها.

## المسار الأساسي

```text
Client
  → API Gateway Load Balancer
  → API Gateway
  → Request Service Load Balancer
  → Request Service × 2
  → PostgreSQL: Request + Outbox Event
  → Outbox Publisher
  → RabbitMQ
  → Request Worker × N
  → COMPLETED | FAILED
```

يتولى `Auth Service` إصدار `JWT`. يتحقق `API Gateway` من الهوية ويوجه الطلبات. يستخدم `Request Service` مفتاح `Idempotency-Key` ضمن نطاق المستخدم، ويحفظ الطلب وحدث `Outbox` في معاملة واحدة. ينشر `Outbox Publisher` الحدث مع `Publisher Confirms`، ثم ينفذ `Request Worker` العمل بأسلوب `At-Least-Once` مع جدول `processed_events` لمنع معالجة الحدث نفسه مرتين.

## المكونات المنفذة

| المكوّن | المسؤولية |
|---|---|
| `API Gateway` | التحقق من `JWT`، تمرير الهوية، التوجيه، `Rate Limiting` و`Circuit Breaker` |
| `Auth Service` | التسجيل، تسجيل الدخول، وتجديد الرموز |
| `Request Service` | قبول الطلبات، الملكية، `Idempotency`، والاستعلام |
| `Outbox Publisher` | نشر الأحداث المتينة وتسجيل تأكيد النشر |
| `RabbitMQ` | التخزين المؤقت، إعادة التسليم، و`DLQ` |
| `Request Worker` | التنفيذ المتوازي وتحديث الحالة النهائية |
| `PostgreSQL + Redis` | الحالة المتينة، القفل، التخزين المؤقت، والمسار السريع |
| `Prometheus + Grafana` | جمع المقاييس وعرض صحة النظام والحمل والكمون والطوابير |
| `k6` | اختبارات `Smoke` و`Load` و`Stress` و`Spike` و`Soak` |

## بنية التشغيل الحالية

يشغّل `docker-compose.yml` Gateway واحدًا خلف `HAProxy`، ونسختين من `Request Service` خلف `HAProxy`، ودور `Outbox Publisher` مستقلًا، ونسخة Worker واحدة افتراضيًا. يتيح `docker-compose.scaling-base.yml` تغيير عدد نسخ Worker في اختبارات التوسع.

نقطة دخول العملاء:

```text
http://localhost:8088
```

يستخدم `ngrok` للعرض الوظيفي عن بُعد فقط، ولا يدخل في مسار قياس الأداء. عند فصل مولد الحمل عن النظام يستخدم k6 عنوان LAN للحاسوب الذي يشغّل HLRMS.

## تشغيل البيئة

```bash
docker compose up -d --build --scale request-service=2
docker compose ps
```

يجب تعيين الأسرار الفعلية في `.env` وعدم استخدام كلمات المرور الافتراضية خارج بيئة التطوير.

| الواجهة | العنوان |
|---|---|
| HLRMS API | `http://localhost:8088` |
| Grafana | `http://localhost:3000` |
| Prometheus | `http://localhost:9090` |
| RabbitMQ Management | `http://localhost:15672` |
| Request Service diagnostics | `http://localhost:18080` |

## التوثيق

- [فهرس الوثائق](docs/README.md)
- [المعمارية المنفذة](docs/architecture/SYSTEM-ARCHITECTURE.md)
- [عقد API](docs/api/API-SPECIFICATION.md)
- [نتائج الأداء](docs/performance/BENCHMARK-RESULTS.md)
- [بروتوكول إثبات اختبار الحمل](docs/performance/EVIDENCE-PROTOCOL.md)
- [تكامل أي تطبيق أو موقع](docs/integration/CLIENT-INTEGRATION.md)
- [حزمة المخططات](diagrams/README.md)

## الفروع

- `main`: النسخة المستقرة بعد التسليم.
- `develop`: التنفيذ المكتمل قبل الدمج النهائي.
- `docs/final-delivery`: المخططات والوثائق النهائية الجاري اعتمادها.
- `feature/*` و`fix/*`: تطوير الميزات والإصلاحات المعزولة.

لا يُنقل محتوى `docs/final-delivery` إلى `main` إلا بعد مراجعة الملفات الناتجة، نجاح الاختبارات، واعتماد نتائج الأداء النهائية.
