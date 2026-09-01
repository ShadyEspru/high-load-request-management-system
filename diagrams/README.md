# دليل مخططات HLRMS

يحتوي هذا المجلد على المخططات النهائية لنظام **High-Load Request Management System (HLRMS)**. تمثل المخططات المنصة بوصفها نظامًا عامًا قابلًا للتكامل مع أي تطبيق Mobile أو Web أو Partner System، وليست مرتبطة بمجال أعمال محدد.

جميع النصوص داخل المخططات باللغة الإنجليزية. أما هذا الملف وقاموس تدفقات البيانات فهما وثائق مرافقة باللغة العربية مع إبقاء المصطلحات التقنية باللغة الإنجليزية.

## الحزمة المعتمدة

- عدد المخططات: **31 مخططًا**.
- لكل مخطط ثلاث صيغ متطابقة في المحتوى: `drawio` و`png` و`pdf`.
- أبعاد PNG: `1920×1080`، وهي مناسبة للعروض التقديمية والتوثيق.
- أبعاد PDF: `1440×810 pt` وبصفحة واحدة لكل مخطط.
- ملف `drawio` هو المصدر القابل للتحرير.

## الفهرس

| المجلد | المخططات | الغرض |
|---|---|---|
| `01-context` | `system-context` | حدود HLRMS وعلاقته بالعملاء والمشغّلين ومنصة المراقبة وخدمات الأعمال الاختيارية. |
| `02-use-cases` | `use-case-overview`، `use-case-submit-request` | قدرات المنصة وحالة الاستخدام التفصيلية لإرسال الطلب. |
| `03-dfd` | `dfd-level0-context`، `dfd-level1` | تدفقات البيانات الخارجية والعمليات الداخلية ومخازن البيانات. |
| `04-activity` | `request-processing-activity` | فصل القبول المتزامن عن التنفيذ غير المتزامن. |
| `05-sequence` | `request-submission-sequence`، `asynchronous-processing-sequence` | تسلسل قبول الطلب ونشر الحدث ومعالجته بأمان. |
| `06-state` | `request-lifecycle-state` | الحالات الأربع المنفذة: `PENDING` و`PROCESSING` و`COMPLETED` و`FAILED`. |
| `07-class` | `core-domain-class-diagram` | مكونات المجال والـOutbox والـIdempotent Consumer. |
| `08-c4` | `c4-level1-system-context`، `c4-level2-container`، `c4-level3-request-processing-components` | مستويات C4 من السياق إلى مكونات معالجة الطلب. |
| `09-deployment` | `deployment-overview`، `docker-compose-topology`، `two-host-performance-topology` | النشر المحلي، شبكة Docker Compose، وفصل مولد الحمل عن النظام. |
| `10-database` | `authentication-database-erd`، `database-landscape`، `requests-database-erd` | ملكية البيانات ومخططات قواعد Auth وRequests المنفذة. |
| `11-rabbitmq` | `rabbitmq-topology`، `message-lifecycle` | بنية Exchange/Queue/DLQ ودورة حياة الرسالة الموثوقة. |
| `12-monitoring` | `monitoring-architecture`، `metrics-and-alerts` | جمع المقاييس في Prometheus وعرضها في Grafana والإشارات التشغيلية. |
| `13-security` | `security-trust-boundaries` | حدود الثقة ومسؤوليات Gateway والخدمات الداخلية. |
| `14-client-integration` | `client-integration-architecture` | تكامل أي Client مع HLRMS عبر العقد العام نفسه. |
| `15-fault-tolerance` | `fault-tolerance-recovery` | سلوك النظام والتعافي عند فشل مكوناته. |
| `16-performance` | `performance-test-architecture`، `performance-evidence-chain` | بنية اختبار الأداء وسلسلة إثبات النتائج. |
| `17-reliability` | `outbox-idempotency` | ضمانات Transactional Outbox ومنع تكرار أثر الحدث. |
| `18-repository` | `repository-and-delivery-workflow` | مسار العمل بين فروع التطوير والتوثيق والتسليم. |
| `19-api-routing` | `api-routing-and-ports` | التوجيه والمنافذ المنشورة والداخلية. |

## قاموس DFD

تُعرّف التدفقات `F1–F13` الظاهرة في `dfd-level1` داخل [قاموس تدفقات البيانات](03-dfd/DATA-DICTIONARY.md).

## التوليد والتحقق

يُعاد إنشاء الحزمة من المصدر البرمجي باستخدام:

```bash
python3 scripts/diagrams/generate_diagrams.py
```

ثم تُفحص الصيغ والأبعاد وبنية XML باستخدام:

```bash
python3 scripts/diagrams/validate_diagrams.py
```

يجب أن تنتهي عملية التحقق بالنتيجة التالية:

```text
OK: 31 diagrams × 3 formats; PNG 1920x1080; PDF 1440x810 pt; editable XML parsed.
```

توجد إعدادات التشغيل الفعلية الخاصة بـPrometheus وGrafana وRabbitMQ ضمن مجلدات التنفيذ والمراقبة في المشروع، ولا تُحفظ مسودات إعداد مستقلة داخل مجلد المخططات.
