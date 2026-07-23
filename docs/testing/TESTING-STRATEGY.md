# Testing Strategy — HLRMS

يوثق هذا الملف الاستراتيجية العامة لاختبار نظام:

**High-Load Request Management System (HLRMS)**

تهدف الاستراتيجية إلى ضمان صحة الوظائف، اتساق البيانات، موثوقية المعالجة غير المتزامنة، قابلية التوسع، تحمل الأعطال، والأمان.

---

## 1. الغرض

تحدد هذه الوثيقة:

- مستويات الاختبار.
- أنواع الاختبارات.
- نطاق كل مستوى.
- المسؤوليات.
- بيئات الاختبار.
- الأدوات المستخدمة.
- معايير الدخول والخروج.
- أولويات التنفيذ.
- سياسة التغطية.
- إدارة العيوب.
- متطلبات الأدلة والتقارير.
- تكامل الاختبارات مع CI/CD.

---

## 2. نطاق النظام

تشمل الاستراتيجية المكونات التالية:

```text
Android Client
REST API
Authentication and Authorization
Request Service
Transactional Outbox
RabbitMQ Exchanges and Queues
Worker Services
PostgreSQL
Redis
Retry and DLQ Flow
Monitoring Stack
Prometheus
Grafana
```

---

## 3. أهداف الجودة

يجب أن تثبت الاختبارات أن النظام:

1. ينفذ المتطلبات الوظيفية بصورة صحيحة.
2. يمنع فقد الطلبات المقبولة.
3. يمنع التنفيذ المكرر غير الآمن.
4. يحافظ على اتساق حالة الطلب.
5. يعالج الرسائل وفق At-Least-Once Delivery.
6. يستعيد العمل بعد الأعطال المؤقتة.
7. يوجه الفشل النهائي إلى DLQ.
8. يحقق أهداف الأداء.
9. يوفر قابلية مراقبة كافية.
10. يحمي الواجهات والبيانات من الاستخدام غير المصرح به.

---

## 4. منهجية الاختبار

### 4.1 Test Pyramid

يعتمد المشروع الهرم التالي:

```text
                 End-to-End Tests
              API and Integration Tests
         Unit and Component-Level Tests
```

التوزيع المستهدف:

| المستوى | النسبة التقريبية |
|---|---:|
| Unit Tests | 60% |
| Component / Integration Tests | 30% |
| End-to-End Tests | 10% |

هذه النسب إرشادية وليست شرطًا حسابيًا ثابتًا.

### 4.2 Shift Left

تبدأ أنشطة الاختبار مبكرًا من خلال:

- مراجعة المتطلبات.
- مراجعة API Contract.
- مراجعة ADRs.
- تعريف Test Cases قبل التنفيذ.
- إضافة الاختبارات مع كل Feature.
- منع تأجيل الاختبار إلى نهاية المشروع.

### 4.3 Risk-Based Testing

تعطى الأولوية للمسارات الأعلى خطورة:

```text
Request Creation
Transactional Outbox
RabbitMQ Publishing
Worker Processing
Retry
DLQ
Idempotency
Authorization
Database Consistency
```

---

## 5. مستويات الاختبار

## 5.1 Unit Testing

### الهدف

اختبار Business Logic بصورة معزولة وسريعة.

### النطاق

- Services.
- Validators.
- Mappers.
- State Transition Rules.
- Retry Decision Logic.
- Priority Resolution.
- Idempotency Logic.
- Exception Mapping.
- Security Helpers.

### الخصائص

- لا تعتمد على PostgreSQL حقيقي.
- لا تعتمد على RabbitMQ حقيقي.
- لا تعتمد على Redis حقيقي.
- تستخدم Mocks فقط للحدود الخارجية.
- تنفذ بسرعة داخل كل Build.

---

## 5.2 Component Testing

### الهدف

اختبار مكون Spring Boot واحد مع Configuration حقيقية قدر الإمكان.

### أمثلة

- اختبار Request Service مع PostgreSQL Testcontainer.
- اختبار Outbox Publisher مع RabbitMQ Testcontainer.
- اختبار Rate Limiter مع Redis Testcontainer.
- اختبار Controller مع Spring Context.

### الخصائص

- أوسع من Unit Test.
- أضيق من End-to-End.
- تستخدم Dependencies حقيقية عند الحاجة.
- تركز على حدود مكون واحد.

---

## 5.3 Integration Testing

### الهدف

التحقق من صحة التكامل بين مكونات النظام.

### التكاملات الحرجة

```text
Spring Boot ↔ PostgreSQL
Spring Boot ↔ RabbitMQ
Spring Boot ↔ Redis
Request Service ↔ Outbox Table
Outbox Publisher ↔ RabbitMQ
RabbitMQ ↔ Worker
Worker ↔ PostgreSQL
```

### أمثلة

- حفظ الطلب وOutbox Event داخل Transaction واحدة.
- استرجاع الطلب بعد Commit.
- Rollback عند فشل الكتابة.
- نشر Outbox Event والحصول على Publisher Confirm.
- إعادة النشر بعد استعادة RabbitMQ.
- Cache Miss والرجوع إلى PostgreSQL.
- Routing إلى Queue الصحيحة حسب الأولوية.

---

## 5.4 API Testing

### الهدف

التحقق من التزام التطبيق بعقد REST API.

### النطاق

- Status Codes.
- Request Validation.
- Response Schema.
- Error Schema.
- Authentication.
- Authorization.
- Idempotency-Key.
- Correlation ID.
- Pagination.
- Filtering.
- Sorting.
- Content Type.

### أمثلة

```text
POST /requests
GET /requests/{id}
GET /requests
POST /admin/requests/{id}/retry
GET /actuator/health
```

---

## 5.5 End-to-End Testing

### الهدف

اختبار دورة حياة الطلب كاملة عبر النظام.

### السيناريو الأساسي

```text
Client
→ POST /requests
→ PostgreSQL
→ Outbox
→ RabbitMQ
→ Worker
→ Result
→ GET /requests/{id}
```

### السيناريوهات الحرجة

- معالجة ناجحة.
- فشل مؤقت ثم نجاح.
- استنفاد Retry ثم DLQ.
- رسالة مكررة.
- طلب مكرر بنفس Idempotency-Key.
- RabbitMQ متوقف ثم يعود.
- Redis متوقف مع استمرار المسار الأساسي.

---

## 5.6 Resilience Testing

### الهدف

التحقق من سلوك النظام عند الفشل.

### حالات الاختبار

| الحالة | السلوك المتوقع |
|---|---|
| توقف RabbitMQ | يبقى Outbox Event في PENDING |
| عودة RabbitMQ | يعاد النشر تلقائيًا |
| توقف Worker | تبقى الرسائل في Queue |
| فشل Worker قبل ACK | تعاد الرسالة |
| توقف Redis | الرجوع إلى PostgreSQL |
| فشل PostgreSQL Transaction | لا ينشأ Request ولا Outbox Event |
| تجاوز Retry | إرسال الرسالة إلى DLQ |
| تكرار الرسالة | لا يتكرر الأثر التجاري |

### مبادئ

- لا تستخدم Chaos غير منضبطة في البيئة المشتركة.
- يجب تحديد الفشل المتوقع ومدة الاختبار.
- يجب جمع Logs وMetrics لكل تجربة.
- يجب إعادة البيئة إلى حالة سليمة بعد الاختبار.

---

## 5.7 Security Testing

### الهدف

التحقق من الضوابط الموثقة في Security Design.

### النطاق

- JWT validation.
- Role-Based Access Control.
- Resource-Level Authorization.
- Input Validation.
- Injection Prevention.
- Rate Limiting.
- Secret Exposure.
- Actuator Protection.
- Error Information Leakage.
- Audit Logging.

### أمثلة

- رفض Token منتهي.
- رفض Token بتوقيع غير صالح.
- منع CLIENT من Endpoint إداري.
- منع الوصول إلى طلب يخص Client آخر.
- رفض Payload غير صالح.
- عدم إظهار Stack Trace للعميل.
- حماية `/actuator/prometheus`.

---

## 5.8 Performance Testing

### الأنواع

- Load Test.
- Stress Test.
- Spike Test.
- Soak Test.
- Capacity Test.

### المؤشرات

```text
Request Rate
Response Time
P50
P95
P99
Error Rate
Queue Depth
Unacked Messages
Outbox Lag
Worker Throughput
CPU
Memory
Database Connections
Redis Memory
RabbitMQ Disk Usage
```

### أهداف أولية

القيم التالية أولية وتثبت أو تعدل بعد Baseline Test:

```text
POST /requests P95 <= 500 ms
GET /requests/{id} P95 <= 300 ms
HTTP Error Rate < 1%
No Lost Accepted Requests
No Unbounded Queue Growth under target load
```

---

## 5.9 Acceptance Testing

### الهدف

التحقق من أن النظام يحقق المتطلبات المعتمدة.

يجب ربط كل Acceptance Test بـ:

```text
Requirement ID
Use Case
API Endpoint
Test Case ID
Expected Result
Evidence
```

---

## 6. استراتيجية اختبار المكونات

## 6.1 PostgreSQL

يجب اختبار:

- Flyway Migrations.
- Constraints.
- Foreign Keys.
- Unique Idempotency Constraint.
- Transaction Rollback.
- Optimistic Locking عند استخدامه.
- Query Pagination.
- Index-Relevant Queries.
- Outbox Polling.
- Concurrent Access.

## 6.2 RabbitMQ

يجب اختبار:

- Exchange Declaration.
- Queue Declaration.
- Bindings.
- Routing Keys.
- Publisher Confirms.
- Persistent Messages.
- Manual ACK.
- NACK behavior.
- Retry Routing.
- Dead-Letter Routing.
- Consumer Recovery.
- Duplicate Delivery.
- Quorum Queue Configuration في بيئة مناسبة.

## 6.3 Redis

يجب اختبار:

- Cache Hit.
- Cache Miss.
- TTL.
- Key Naming.
- Rate Limiting Counter.
- Expiration.
- Redis Unavailable Fallback.
- Cache Invalidation.
- عدم الاعتماد عليه كمصدر حقيقة.

## 6.4 Transactional Outbox

يجب اختبار:

- Atomic creation of Request and Outbox Event.
- عدم وجود Outbox Event عند Rollback.
- Polling باستخدام `SKIP LOCKED`.
- Concurrent Publishers.
- Publisher Confirm.
- Retry Backoff.
- Duplicate Publish.
- PUBLISHED state.
- DEAD state.
- Outbox Lag Metrics.
- Retention behavior.

## 6.5 Worker

يجب اختبار:

- قراءة الرسالة.
- التحقق من Payload.
- انتقال الحالة.
- حفظ المحاولة.
- حفظ النتيجة.
- ACK بعد Commit.
- عدم ACK عند الفشل.
- Retry decision.
- Idempotent processing.
- DLQ after maximum attempts.

---

## 7. بيئات الاختبار

## 7.1 Local Environment

الاستخدام:

- Unit Tests.
- Integration Tests.
- Manual API Verification.
- Development k6 Smoke Tests.

المكونات:

```text
Docker Compose
PostgreSQL
RabbitMQ
Redis
Prometheus
Grafana
```

## 7.2 CI Environment

الاستخدام:

- Compilation.
- Unit Tests.
- Integration Tests.
- Coverage.
- Static Analysis.
- OpenAPI Validation.
- Migration Validation.

## 7.3 Test Environment

الاستخدام:

- End-to-End.
- Security.
- Resilience.
- Acceptance.

## 7.4 Performance Environment

الاستخدام:

- Load.
- Stress.
- Spike.
- Soak.

يجب أن تكون مستقلة عن بيئة التطوير اليومية قدر الإمكان.

---

## 8. إدارة بيانات الاختبار

يجب أن تكون بيانات الاختبار:

- Synthetic.
- قابلة لإعادة الإنشاء.
- غير مأخوذة من Production.
- خالية من البيانات الشخصية الحقيقية.
- ذات معرفات قابلة للتتبع.
- قابلة للتنظيف.

تستخدم Prefixes مثل:

```text
test-client-
test-request-
perf-run-
```

---

## 9. استقلالية الاختبارات

كل اختبار يجب أن:

- ينشئ بياناته الخاصة.
- لا يعتمد على اختبار سابق.
- ينظف آثاره أو يستخدم Isolation مناسبًا.
- لا يعتمد على ترتيب التنفيذ.
- لا يستخدم Thread Sleep إلا للضرورة القصوى.
- يستخدم Awaitility للانتظار غير المتزامن.
- يملك Timeout واضحًا.

---

## 10. سياسة Mocking

تستخدم Mocks عندما يكون الهدف اختبار منطق معزول.

لا تستخدم Mocks بدل التكامل الحقيقي عندما يكون المطلوب التحقق من:

- SQL.
- Flyway.
- RabbitMQ Routing.
- Publisher Confirms.
- Redis TTL.
- Transaction Boundaries.
- Serialization.

القاعدة:

```text
Unit Test → Mock external boundaries
Integration Test → Use real infrastructure through Testcontainers
```

---

## 11. التسمية والتنظيم

### أسماء الاختبارات

النمط المقترح:

```text
methodName_condition_expectedResult
```

مثال:

```text
createRequest_validCommand_savesRequestAndOutboxEvent
publishEvent_brokerUnavailable_keepsEventPending
processMessage_duplicateDelivery_doesNotRepeatBusinessEffect
```

### هيكل Arrange-Act-Assert

```text
Arrange
Act
Assert
```

أو:

```text
Given
When
Then
```

---

## 12. أولويات الاختبار

| الأولوية | الوصف |
|---|---|
| P0 | يمنع فقد البيانات أو اختراق الأمان أو توقف المسار الأساسي |
| P1 | وظيفة أساسية أو فشل مؤثر مع Workaround محدود |
| P2 | وظيفة ثانوية أو حالة حدية |
| P3 | تحسين أو سلوك غير حرج |

### أمثلة P0

- Transactional Outbox Atomicity.
- Idempotency.
- Authorization.
- Worker ACK after Commit.
- Retry to DLQ.
- No Lost Accepted Requests.

---

## 13. التغطية

الأهداف الأولية:

| النطاق | الحد المستهدف |
|---|---:|
| إجمالي Line Coverage | 80% |
| Business Services | 90% |
| Critical Branches | 85% |
| Security Rules | 90% |
| State Transition Logic | 90% |

لا تستخدم التغطية كبديل لجودة Assertions.

---

## 14. معايير الدخول

يبدأ الاختبار عندما:

- تكون المتطلبات موثقة.
- يكون API Contract محددًا.
- تكون ADRs ذات الصلة معتمدة.
- تكون البيئة قابلة للتشغيل.
- تكون Migrations متاحة.
- يكون الكود قابلًا للـCompilation.
- تكون Test Data محددة.
- تكون Dependencies مستقرة بالقدر الكافي.

---

## 15. معايير الخروج

ينتهي مستوى الاختبار عندما:

- تنجح جميع حالات P0.
- تنجح حالات P1 أو تكون الانحرافات مقبولة وموثقة.
- لا توجد عيوب Critical مفتوحة.
- لا توجد عيوب High بلا قرار واضح.
- تتحقق Thresholds الأداء.
- تحفظ تقارير الاختبار.
- تكون نتائج الفشل قابلة للتفسير.
- يكتمل الربط في Traceability Matrix.

---

## 16. إدارة العيوب

### درجات الخطورة

| الدرجة | الوصف |
|---|---|
| Critical | فقد بيانات، اختراق، توقف كلي، أو فساد واسع |
| High | تعطيل وظيفة أساسية أو عدم موثوقية كبيرة |
| Medium | خلل وظيفي مع Workaround |
| Low | مشكلة محدودة أو شكلية |

### محتوى Defect Report

- Defect ID.
- Title.
- Environment.
- Build Version.
- Preconditions.
- Reproduction Steps.
- Expected Result.
- Actual Result.
- Severity.
- Logs.
- Correlation ID.
- Screenshots أو Metrics.
- Owner.
- Status.

---

## 17. استراتيجية CI

### عند كل Pull Request

```text
Compile
Unit Tests
Integration Tests
Coverage
Static Analysis
OpenAPI Validation
Flyway Validation
```

### عند الدمج إلى develop

```text
Full Integration Suite
Selected End-to-End Tests
Docker Image Build
Security Checks
```

### مجدول أو يدوي

```text
Load Test
Stress Test
Spike Test
Soak Test
Resilience Test
```

---

## 18. بوابات الجودة

يمنع الدمج عند:

- فشل Build.
- فشل Unit Tests.
- فشل Integration Tests الحرجة.
- انخفاض التغطية تحت الحد المعتمد.
- وجود Migration غير صالحة.
- فشل API Contract Validation.
- اكتشاف Secret داخل المستودع.
- وجود ثغرة Critical معروفة في Dependency مستخدمة.

---

## 19. التقارير والأدلة

تحفظ الأدلة التالية:

```text
Surefire Reports
Failsafe Reports
JaCoCo Report
k6 Summary
Prometheus Metrics Snapshot
Grafana Screenshots
RabbitMQ Queue Metrics
Outbox Metrics
Application Logs
Defect Reports
Acceptance Evidence
```

يجب ألا تتضمن التقارير:

- Passwords.
- JWT Tokens صالحة.
- Database Credentials.
- API Secrets.
- بيانات شخصية حقيقية.

---

## 20. Traceability

يجب ربط كل Test Case مهم بـ:

```text
Requirement
Use Case
Component
API Endpoint
ADR
Test Case
Evidence
```

مثال:

| Requirement | ADR | Test Case |
|---|---|---|
| No Lost Accepted Requests | ADR-004 | IT-OUTBOX-001 |
| Broker Failure Tolerance | ADR-005 | RES-RMQ-003 |
| Idempotent Request Creation | ADR-002 | API-IDEM-002 |
| Low Latency Status Read | ADR-003 | LT-STATUS-001 |

---

## 21. المخاطر والقيود

### المخاطر

- اختلاف بيئة الأداء عن Production.
- Flaky Tests بسبب المعالجة غير المتزامنة.
- نتائج Load غير دقيقة عند مشاركة الموارد.
- ضعف Test Data.
- الاعتماد الزائد على Mocks.
- إهمال Failure Paths.
- صعوبة محاكاة Quorum Cluster محليًا.

### التخفيف

- استخدام بيئة أداء مستقلة.
- استخدام Awaitility بدل الانتظار الثابت.
- تشغيل Testcontainers بإصدارات ثابتة.
- توثيق مواصفات البيئة.
- إعادة الاختبارات الحرجة عدة مرات.
- جمع Metrics أثناء الاختبار.
- فصل Smoke Load Tests عن الاختبارات الطويلة.

---

## 22. Definition of Done للاختبارات

لا تعد Feature مكتملة إلا عندما:

- توجد Unit Tests للمنطق الجديد.
- توجد Integration Tests للتكاملات الجديدة.
- تحدث Test Data عند الحاجة.
- تحدث Traceability Matrix.
- تمر Quality Gates.
- توثق السيناريوهات السلبية.
- لا توجد Assertions سطحية.
- تضاف Metrics عند وجود سلوك تشغيلي مهم.
- تحدث الوثائق ذات الصلة.

---

## 23. حالات الاختبار الحرجة المبدئية

| ID | السيناريو | النوع | الأولوية |
|---|---|---|---|
| IT-OUTBOX-001 | حفظ Request وOutbox Event في Transaction واحدة | Integration | P0 |
| IT-OUTBOX-002 | Rollback يمنع حفظ السجلين | Integration | P0 |
| RES-RMQ-001 | توقف RabbitMQ يبقي الحدث PENDING | Resilience | P0 |
| RES-RMQ-002 | عودة RabbitMQ تؤدي إلى النشر | Resilience | P0 |
| IT-WORKER-001 | ACK بعد نجاح Database Commit | Integration | P0 |
| IT-WORKER-002 | فشل قبل ACK يؤدي إلى إعادة التسليم | Integration | P0 |
| API-IDEM-001 | تكرار المفتاح لا ينشئ طلبًا جديدًا | API | P0 |
| IT-DLQ-001 | تجاوز Retry ينقل الرسالة إلى DLQ | Integration | P0 |
| SEC-RBAC-001 | CLIENT ممنوع من Admin Endpoint | Security | P0 |
| RES-REDIS-001 | توقف Redis لا يوقف قراءة الحالة | Resilience | P1 |
| LT-CREATE-001 | إنشاء طلبات ضمن P95 المستهدف | Load | P1 |
| SOAK-001 | استقرار النظام تحت حمل طويل | Soak | P1 |

---

## 24. معايير اعتماد هذه الوثيقة

تعد هذه الاستراتيجية معتمدة عندما:

- يوافق عليها فريق المشروع.
- تتوافق مع Functional Requirements وNFRs.
- تتوافق مع ADRs المعتمدة.
- تستخدم كمرجع أثناء تنفيذ Backend.
- تحدث عند تغير Architecture أو Testing Scope.

---

## 25. حالة الوثيقة

```text
Status: Accepted
Version: 1.0
Owner: HLRMS Team
```
