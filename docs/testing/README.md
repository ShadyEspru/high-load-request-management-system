# Testing Documentation — HLRMS

يوثق هذا المجلد استراتيجية الاختبار الخاصة بنظام:

**High-Load Request Management System (HLRMS)**

الهدف من هذه الحزمة هو تعريف منهجية اختبار واضحة وقابلة للتنفيذ تغطي جودة النظام من مستوى الوحدات البرمجية حتى اختبارات الحمل والتحمل.

---

## 1. أهداف الاختبار

تهدف عملية الاختبار إلى التحقق من أن النظام:

- يطبق المتطلبات الوظيفية بصورة صحيحة.
- يحافظ على اتساق البيانات.
- يعالج الطلبات بصورة غير متزامنة وموثوقة.
- يتحمل الأخطاء المؤقتة وفشل المكونات.
- يمنع تكرار تنفيذ نفس العملية بصورة غير آمنة.
- يحقق أهداف الأداء وقابلية التوسع.
- يوفر مقاييس تشغيلية تساعد على اكتشاف المشكلات.
- يحمي واجهات API والبيانات من الاستخدام غير المصرح به.

---

## 2. نطاق الاختبار

تشمل استراتيجية الاختبار المكونات التالية:

```text
Android Client
API Gateway / Request API
Authentication and Authorization
Request Service
Transactional Outbox
RabbitMQ
Workers
PostgreSQL
Redis
Monitoring Stack
REST API
```

---

## 3. أنواع الاختبارات

سيتم تطبيق الأنواع التالية:

| نوع الاختبار | الهدف |
|---|---|
| Unit Testing | اختبار Business Logic بصورة معزولة |
| Component Testing | اختبار مكون Spring Boot مع اعتماداته المحلية |
| Integration Testing | اختبار التكامل مع PostgreSQL وRabbitMQ وRedis |
| API Testing | التحقق من Contract وسلوك REST API |
| End-to-End Testing | اختبار دورة حياة الطلب كاملة |
| Resilience Testing | اختبار الفشل والاستعادة وRetry وDLQ |
| Security Testing | اختبار المصادقة والصلاحيات والتحقق من المدخلات |
| Load Testing | قياس الأداء تحت حمل متوقع |
| Stress Testing | تحديد حدود النظام ونقطة الانهيار |
| Spike Testing | اختبار الارتفاع المفاجئ في الحمل |
| Soak Testing | اختبار الاستقرار تحت حمل طويل |
| Acceptance Testing | التحقق من معايير قبول المتطلبات |

---

## 4. مبادئ الاختبار

### 4.1 Test Pyramid

سيتم توزيع الاختبارات وفق المبدأ التالي:

```text
                 End-to-End
               /            \
             Integration Tests
           /                  \
        Unit Tests and Component Tests
```

ستكون اختبارات Unit هي الأكثر عددًا، بينما تكون End-to-End أقل عددًا وأكثر تركيزًا على المسارات الحرجة.

### 4.2 Automation First

يجب أتمتة الاختبارات القابلة للتكرار ضمن Pipeline قدر الإمكان.

### 4.3 Production-Like Dependencies

تستخدم Testcontainers لتشغيل نسخ حقيقية من:

- PostgreSQL
- RabbitMQ
- Redis

بدل الاعتماد على Mocks في اختبارات التكامل.

### 4.4 Deterministic Tests

يجب أن تكون الاختبارات:

- مستقلة.
- قابلة لإعادة التنفيذ.
- غير معتمدة على ترتيب التشغيل.
- غير معتمدة على بيانات بيئة مشتركة.
- ذات نتائج واضحة وثابتة.

### 4.5 Test the Failure Path

لا يقتصر الاختبار على Happy Path، بل يشمل:

- Timeout.
- Invalid Input.
- Duplicate Request.
- RabbitMQ Unavailable.
- Redis Unavailable.
- PostgreSQL Transaction Rollback.
- Worker Failure.
- Retry Exhaustion.
- DLQ Routing.
- Duplicate Message Delivery.

---

## 5. أدوات الاختبار

### Backend

```text
JUnit 5
Mockito
AssertJ
Spring Boot Test
MockMvc
Testcontainers
WireMock
Awaitility
JaCoCo
```

### Database and Messaging

```text
PostgreSQL Testcontainer
RabbitMQ Testcontainer
Redis Testcontainer
Flyway
```

### API and Performance

```text
k6
OpenAPI validation
HTTP test scripts
```

### CI

```text
Maven
GitHub Actions
Docker
Docker Compose
```

---

## 6. هيكل المجلد

```text
docs/testing/
├── README.md
├── TESTING-STRATEGY.md
├── UNIT-TESTING.md
├── INTEGRATION-TESTING.md
├── LOAD-TESTING.md
├── TEST-DATA.md
└── TRACEABILITY-MATRIX.md

tests/performance/k6/
├── README.md
├── config.js
├── helpers.js
├── thresholds.js
├── smoke.js
├── baseline.js
├── load.js
├── stress.js
├── spike.js
├── soak.js
├── recovery.js
└── diagnostics/
```

---

## 7. مسؤولية كل ملف

### `TESTING-STRATEGY.md`

يوثق الاستراتيجية العامة، مستويات الاختبار، البيئات، معايير الدخول والخروج، إدارة العيوب، والتكامل مع CI.

### `UNIT-TESTING.md`

يوثق قواعد Unit Tests، التسمية، العزل، Mocking، التغطية، والأمثلة المستهدفة.

### `INTEGRATION-TESTING.md`

يوثق اختبارات التكامل باستخدام Testcontainers واختبارات PostgreSQL وRabbitMQ وRedis وTransactional Outbox.

### `LOAD-TESTING.md`

يوثق أهداف الأداء، السيناريوهات، مؤشرات القياس، Thresholds، وتحليل النتائج.

### `TEST-DATA.md`

يوثق بيانات الاختبار، Payloads، Tokens، الحالات الصحيحة والخاطئة، وسياسات تنظيف البيانات.

### `TRACEABILITY-MATRIX.md`

يربط بين المتطلبات، Use Cases، API Endpoints، وحالات الاختبار.

### `tests/performance/k6/`

يحتوي سكربتات اختبارات الأداء والحمل القابلة للتنفيذ. أبقيت وثائق الاختبار داخل `docs/testing/`، بينما نُقلت ملفات التنفيذ إلى شجرة الاختبارات في جذر المستودع.

---

## 8. مستويات بيئة الاختبار

| البيئة | الاستخدام |
|---|---|
| Local | تطوير Unit وIntegration Tests |
| CI | تشغيل الاختبارات آليًا مع كل Pull Request |
| Test | اختبار تكامل النظام الكامل |
| Performance | تنفيذ k6 ومراقبة الموارد |
| Production-Like | اختبار النشر والتحمل قبل الإصدار |

لا تستخدم بيانات Production الحقيقية داخل بيئات الاختبار.

---

## 9. تصنيف حالات الاختبار

تستخدم المعرفات التالية:

```text
UT-XXX   Unit Test
IT-XXX   Integration Test
API-XXX  API Test
E2E-XXX  End-to-End Test
SEC-XXX  Security Test
RES-XXX  Resilience Test
LT-XXX   Load Test
ST-XXX   Stress Test
SP-XXX   Spike Test
SOAK-XXX Soak Test
AT-XXX   Acceptance Test
```

مثال:

```text
IT-004: Save request and outbox event in one transaction
RES-003: Publish pending outbox events after RabbitMQ recovery
LT-002: Create requests at target throughput for 15 minutes
```

---

## 10. المسارات الحرجة

يجب إعطاء الأولوية للمسارات التالية:

### إنشاء طلب

```text
Client
→ POST /requests
→ Validation
→ PostgreSQL Transaction
→ Request + Outbox Event
→ 202 Accepted
```

### نشر Outbox Event

```text
Outbox Publisher
→ Read PENDING Event
→ Publish to RabbitMQ
→ Publisher Confirm
→ Mark PUBLISHED
```

### معالجة الطلب

```text
RabbitMQ
→ Worker
→ Mark PROCESSING
→ Execute Processing
→ Save Result
→ Mark COMPLETED
→ Manual ACK
```

### معالجة الفشل

```text
Worker Failure
→ Retry Decision
→ Retry Queue
→ Next Attempt
→ DLQ after maximum retries
```

### Idempotency

```text
Repeated POST with same Idempotency-Key
→ Return original result
→ Do not create duplicate request
```

---

## 11. مؤشرات الجودة

ستتم متابعة مؤشرات مثل:

- Test Pass Rate.
- Test Execution Time.
- Code Coverage.
- Defect Density.
- Escaped Defects.
- API Error Rate.
- P95 وP99 Latency.
- Requests Per Second.
- Queue Depth.
- Outbox Lag.
- Retry Rate.
- DLQ Count.
- Database Connection Usage.
- CPU وMemory Usage.

---

## 12. التغطية البرمجية

التغطية ليست هدفًا منفردًا، لكنها مؤشر مساعد.

القيم الأولية المستهدفة:

```text
Overall Line Coverage >= 80%
Business Service Coverage >= 90%
Critical Rules Branch Coverage >= 85%
```

لا يسمح برفع التغطية عبر اختبارات سطحية بلا Assertions ذات قيمة.

---

## 13. معايير الدخول

يبدأ مستوى الاختبار عندما:

- تكون المتطلبات ذات الصلة موثقة.
- يكون API Contract معروفًا.
- تكون Dependencies قابلة للتشغيل.
- تكون Test Data جاهزة.
- يمر المشروع بمرحلة Compilation.
- تكون Migrations قابلة للتطبيق.

---

## 14. معايير الخروج

يمكن اعتبار الإصدار جاهزًا عندما:

- تنجح جميع الاختبارات الحرجة.
- لا توجد عيوب Critical أو High غير مقبولة.
- تتحقق Thresholds الأداء المعتمدة.
- تعمل سيناريوهات Retry وDLQ.
- ينجح اختبار Transactional Outbox.
- ينجح اختبار Duplicate Delivery.
- تكون نتائج الاختبار موثقة.
- تكون الأدلة قابلة لإعادة الإنتاج.

---

## 15. قواعد CI

يجب أن تمنع Pipeline دمج Pull Request عند:

- فشل Compilation.
- فشل Unit Tests.
- فشل Integration Tests الحرجة.
- انخفاض التغطية عن الحد المعتمد.
- فشل Static Analysis الحرج.
- فشل OpenAPI Validation.
- وجود Migration غير قابلة للتطبيق.

اختبارات Load وSoak الطويلة يمكن تشغيلها في Pipeline منفصلة أو بصورة مجدولة.

---

## 16. إدارة العيوب

يجب أن يحتوي كل Defect Report على:

- معرف.
- وصف.
- البيئة.
- خطوات إعادة الإنتاج.
- النتيجة الفعلية.
- النتيجة المتوقعة.
- Severity.
- Evidence.
- Correlation ID عند توفره.
- Logs ذات الصلة.
- حالة الإصلاح وإعادة الاختبار.

---

## 17. الأدلة المطلوبة

تحفظ أدلة الاختبار المهمة، مثل:

- تقارير Maven Tests.
- تقارير JaCoCo.
- نتائج k6.
- Screenshots من Grafana.
- Logs لحالات الفشل.
- Queue Metrics.
- Database Metrics.
- Test Run Summary.

لا تحفظ Secrets أو Tokens صالحة داخل المستودع.

---

## 18. Definition of Done

لا تعد المهمة منتهية إلا عند:

- تنفيذ الكود.
- إضافة Unit Tests.
- إضافة Integration Tests عند وجود تكامل خارجي.
- تحديث الوثائق.
- تمرير CI.
- توثيق أي قرار أو استثناء.
- ربط الاختبار بالمتطلب المناسب.
- عدم وجود TODO غير موثق في المسار الحرج.

---

## 19. ترتيب تنفيذ وثائق الاختبار

سيتم تنفيذ الحزمة بالترتيب التالي:

```text
1. README.md
2. TESTING-STRATEGY.md
3. UNIT-TESTING.md
4. INTEGRATION-TESTING.md
5. LOAD-TESTING.md
6. TEST-DATA.md
7. TRACEABILITY-MATRIX.md
8. k6 scripts
```

---

## 20. حالة الحزمة

| الملف | الحالة |
|---|---|
| README.md | Completed |
| TESTING-STRATEGY.md | Planned |
| UNIT-TESTING.md | Planned |
| INTEGRATION-TESTING.md | Planned |
| LOAD-TESTING.md | Planned |
| TEST-DATA.md | Planned |
| TRACEABILITY-MATRIX.md | Planned |
| k6 Scripts | Planned |
