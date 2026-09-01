# التقنيات المعتمدة

| المجال | التقنية | الاستخدام الفعلي |
|---|---|---|
| Backend | Java 21، Spring Boot 3.x، Maven | خدمات Gateway وAuth وRequest وWorker وDemo API |
| Edge | Spring Cloud Gateway، HAProxy 3.4 | التوجيه والسياسات وتوزيع الحمل |
| Security | Spring Security، JJWT، BCrypt | JWT وRBAC وتخزين كلمات المرور |
| Data | PostgreSQL 17، Spring Data JPA، Flyway | الحالة المتينة ومخططات قواعد البيانات |
| Fast State | Redis 8 | Idempotency وLocks وCache وRate Limiting |
| Messaging | RabbitMQ 4، Spring AMQP | Direct Exchange وDurable Queue وPublisher Confirms وDLQ |
| Resilience | Resilience4j | Circuit Breaker وRetry وBulkhead/TimeLimiter حيث تم إعداده |
| Observability | Actuator، Micrometer، Prometheus 3.5، Grafana | Health وMetrics وDashboard |
| Infrastructure | Docker، Docker Compose | تشغيل البيئة المحلية واختبارات التوسع |
| Load Testing | k6 | Arrival-rate وVU-based scenarios |
| Demo Client | Kotlin، Jetpack Compose، Retrofit | إثبات التكامل الوظيفي القابل للاستبدال |
| Diagrams | diagrams.net XML، Inkscape، Python generator | drawio وPDF وPNG من مصدر واحد |

## قرارات تنفيذية

- Request Service وOutbox Publisher يعملان من Image واحدة مع فصل الدور عبر `OUTBOX_PUBLISHER_ENABLED`.
- HAProxy الحالي يوزع Request traffic على نسختين محددتين.
- Listener في كل Worker يستخدم Auto Acknowledgement مع Retry وMessage Recoverer؛ ليس Manual ACK.
- الطوابير الحالية Durable Classic Queues. Quorum Queues ليست مفعلة في الإعداد المنفذ.
- Demo Client وDemo Business API منفصلان عن HLRMS Core.

## أدوات غير معتمدة في النتيجة الحالية

- Loki غير موجود في Docker Compose الحالي.
- GitHub Actions ليست جزءًا مثبتًا من مسار التشغيل الحالي.
- Kubernetes وCloud deployment أهداف مستقبلية وليست ضمن Benchmark المنفذ.
