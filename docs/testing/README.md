# اختبارات HLRMS

توجد في المصدر الحالي 206 دوال `@Test` موزعة كما يأتي. هذا الرقم جرد للكود، وليس ادعاءً بأن جميع الاختبارات نجحت في أي بيئة دون تشغيلها.

| الخدمة | دوال الاختبار |
|---|---:|
| API Gateway | 56 |
| Auth Service | 25 |
| Request Service | 89 |
| Request Worker | 36 |
| **الإجمالي** | **206** |

## طبقات الاختبار

- Unit Tests: Services وFilters وSecurity وFailure handling.
- Controller Tests: MockMvc وعقود HTTP.
- Repository Tests: PostgreSQL وFlyway والقيود الفريدة.
- Redis Integration: Idempotency وLock وCache وFallback behavior.
- RabbitMQ Integration: Publish/Consume وRetry وDLQ وProcessed Events.
- End-to-End: Gateway → Request Service → Outbox → RabbitMQ → Worker.
- Performance: k6 مع Prometheus وGrafana وDatabase reconciliation.

## التشغيل

اختبارات Gateway وAuth التي تستخدم Mocks:

```bash
cd backend/api-gateway && ./mvnw test
cd ../auth-service && ./mvnw test
```

اختبارات Request Service وWorker تحتاج PostgreSQL وRedis وRabbitMQ المحليين وفق `application-test.yml`:

```bash
docker compose up -d postgres redis rabbitmq
cd backend/request-service && ./mvnw test
cd ../request-worker && ./mvnw test
```

## الوثائق

- [استراتيجية الاختبار](TESTING-STRATEGY.md)
- [Unit Testing](UNIT-TESTING.md)
- [Integration Testing](INTEGRATION-TESTING.md)
- [Load Testing](LOAD-TESTING.md)
- [بيانات الاختبار](TEST-DATA.md)
- [مصفوفة التتبع](TRACEABILITY-MATRIX.md)
- [دليل k6](../../tests/performance/k6/README.md)
