# ADR-004: استخدام Transactional Outbox Pattern

- **Status:** Accepted
- **Date:** 2026-07-23
- **Decision Owners:** HLRMS Team
- **Related Requirements:** No Lost Requests, Reliable Publication, Data Consistency
- **Related ADRs:** ADR-001, ADR-002

## 1. السياق

عند استقبال طلب جديد يجب حفظه في PostgreSQL ونشر رسالة إلى RabbitMQ. هاتان عمليتان في نظامين مختلفين ولا تشتركان في Transaction واحدة.

حفظ الطلب ثم فشل النشر يترك طلبًا عالقًا. النشر ثم فشل الحفظ ينتج رسالة بلا طلب.

## 2. محركات القرار

- عدم فقد الطلبات المقبولة.
- تحمل توقف RabbitMQ.
- تجنب Distributed Transactions.
- دعم Retry والمراقبة والتدقيق.

## 3. البدائل المدروسة

### النشر بعد Commit

بسيط، لكنه قد يفشل بعد نجاح الحفظ.

### النشر قبل Commit

قد تصل الرسالة قبل وجود الطلب أو يفشل Commit بعد النشر.

### Two-Phase Commit

يحاول Atomicity، لكنه معقد ويؤثر في التوفر والأداء.

### Transactional Outbox

يحفظ الطلب وحدث النشر داخل Transaction واحدة، ويتحمل توقف Broker، لكنه يحتاج Publisher وIdempotent Consumer.

## 4. القرار

داخل Transaction واحدة:

```text
INSERT request
INSERT outbox_event
COMMIT
```

بعدها يقرأ Outbox Publisher الأحداث غير المنشورة ويرسلها إلى RabbitMQ. بعد Publisher Confirm يحدث السجل إلى `PUBLISHED`.

## 5. نموذج البيانات

```text
outbox_events
-------------
id
aggregate_type
aggregate_id
event_type
routing_key
payload
headers
status
attempt_count
next_attempt_at
created_at
published_at
last_error
version
```

الحالات:

```text
PENDING
PUBLISHING
PUBLISHED
FAILED
DEAD
```

## 6. استراتيجية القراءة

```sql
SELECT *
FROM outbox_events
WHERE status = 'PENDING'
  AND next_attempt_at <= now()
ORDER BY created_at
FOR UPDATE SKIP LOCKED
LIMIT :batchSize;
```

يسمح ذلك بتشغيل عدة Publisher Instances. مع ذلك يبقى احتمال النشر المتكرر قائمًا.

## 7. Delivery Semantics

```text
At-Least-Once Publication
At-Least-Once Consumption
Idempotent Processing
```

لا يدعي النظام Exactly-Once End-to-End.

## 8. Idempotent Consumer

يستخدم Worker Unique Constraints أو Processed Messages أو التحقق من الحالة الحالية لمنع تنفيذ الأثر مرتين.

## 9. Retry والتنظيف

- زيادة `attempt_count`.
- Exponential Backoff.
- نقل الحدث إلى `DEAD` بعد الحد الأقصى.
- Alert على الأحداث القديمة أو DEAD.
- Retention وأرشفة لأحداث `PUBLISHED`.

## 10. النتائج الإيجابية

- عدم فقد نية النشر.
- تحمل توقف RabbitMQ.
- سجل تدقيقي.
- تجنب 2PC.

## 11. النتائج السلبية والمخاطر

- احتمال نشر مكرر.
- زيادة الكتابات.
- Outbox Lag.
- نمو الجدول والحاجة إلى Retention.

## 12. إجراءات التخفيف

- Idempotent Consumers.
- Publisher Confirms.
- `FOR UPDATE SKIP LOCKED`.
- Batch Size مضبوط.
- فهرس على `(status, next_attempt_at, created_at)`.
- Metrics وAlerts للـPending وLag وFailures.

## 13. معايير القبول

- [ ] الطلب والحدث داخل Transaction واحدة.
- [ ] لا يعد الحدث منشورًا قبل Confirm.
- [ ] يعاد النشر بعد عودة RabbitMQ.
- [ ] يعالج Worker التكرار بأمان.
- [ ] توجد Metrics وAlerts وRetention Policy.

## 14. شروط إعادة المراجعة

عند اعتماد CDC مثل Debezium، أو تغير Broker، أو عدم قدرة Polling Publisher على تحقيق الأداء المطلوب.
