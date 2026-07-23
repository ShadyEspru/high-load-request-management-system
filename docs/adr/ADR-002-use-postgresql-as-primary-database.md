# ADR-002: استخدام PostgreSQL كقاعدة البيانات الأساسية

- **Status:** Accepted
- **Date:** 2026-07-23
- **Decision Owners:** HLRMS Team
- **Related Requirements:** Data Consistency, Durability, Auditability, Queryability
- **Related ADRs:** ADR-003, ADR-004

## 1. السياق

يحتاج النظام إلى تخزين دائم للطلبات، سجل الحالات، المحاولات، النتائج، Idempotency Records، Outbox Events، الإعدادات، وAudit Logs. هذه البيانات مترابطة وتحتاج Transactions وConstraints واستعلامات إدارية.

## 2. محركات القرار

- ACID.
- العلاقات والتكامل المرجعي.
- دعم `JSONB` للـPayload المرن.
- الفهارس والاستعلامات المركبة.
- تكامل Spring Data JPA وFlyway.
- ملاءمة Transactional Outbox.

## 3. البدائل المدروسة

### PostgreSQL

معاملات قوية، JSONB، فهارس، ونضج تشغيلي، لكنه يحتاج ضبطًا جيدًا وقد يصبح نقطة اختناق دون مراقبة.

### MySQL

ناضج، لكنه لا يقدم للمشروع ميزة حاسمة مقارنة بـPostgreSQL.

### MongoDB

مرن للوثائق، لكن نموذج HLRMS علائقي ويحتاج اتساقًا وسجلًا تدقيقيًا واضحًا.

### Redis كمخزن أساسي

سريع، لكنه غير مناسب كمصدر الحقيقة الدائم أو للاستعلامات العلائقية.

## 4. القرار

سيستخدم HLRMS **PostgreSQL** كمصدر الحقيقة الأساسي. تحفظ الحقول الجوهرية في أعمدة صريحة، ويمكن حفظ `payload` و`metadata` و`result.data` في `JSONB`.

## 5. المبررات

يدعم النموذج العلائقي دورة حياة الطلب والمعاملات والقيود، ويتلاءم مباشرة مع Transactional Outbox.

## 6. النتائج الإيجابية

- مصدر حقيقة واحد.
- اتساق قوي.
- Unique Constraints للـIdempotency.
- استعلامات إدارية وتدقيق.
- دعم Outbox Pattern.

## 7. النتائج السلبية والمخاطر

- نمو الجداول بسرعة.
- ضغط كتابة مرتفع.
- الحاجة إلى فهارس وRetention.
- احتمال بطء الاستعلامات الواسعة.

## 8. إجراءات التخفيف

- Pagination وفهارس مبنية على الاستعلامات الفعلية.
- مراقبة Slow Queries وConnection Pool.
- Migrations إلزامية.
- أرشفة البيانات القديمة.
- Read Replicas مستقبلًا عند الحاجة.

## 9. تفاصيل التنفيذ

```text
requests
request_status_history
processing_attempts
processing_results
idempotency_records
outbox_events
system_configurations
audit_logs
```

قيود وفهارس مهمة:

- Unique على `(client_system_id, idempotency_key)`.
- Indexes على `status`, `priority`, `created_at`, `client_system_id`, `next_retry_at`.
- Optimistic Locking عند الحاجة.
- Flyway لجميع تغييرات Schema.

## 10. معايير القبول

- [ ] الطلب وOutbox Event داخل Transaction واحدة.
- [ ] Unique Constraint يمنع تكرار المفتاح.
- [ ] الاستعلامات تدعم Pagination.
- [ ] جميع تغييرات Schema عبر Migrations.
- [ ] توجد اختبارات Rollback.

## 11. شروط إعادة المراجعة

عند الحاجة إلى توزيع جغرافي متعدد الكتابة، أو تغير نموذج البيانات جذريًا، أو تجاوز القدرة التشغيلية للتصميم الحالي.
