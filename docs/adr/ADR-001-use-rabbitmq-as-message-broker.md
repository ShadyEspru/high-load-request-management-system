# ADR-001: استخدام RabbitMQ كوسيط رسائل

- **Status:** Accepted
- **Date:** 2026-07-23
- **Decision Owners:** HLRMS Team
- **Related Requirements:** High Throughput, Asynchronous Processing, Retry, DLQ, Horizontal Scaling
- **Related ADRs:** ADR-004, ADR-005

## 1. السياق

يجب أن يستقبل HLRMS عددًا كبيرًا من الطلبات دون ربط زمن استجابة API بزمن المعالجة. قد تكون المعالجة طويلة، قابلة للفشل المؤقت، موزعة على عدة Workers، وذات أولويات مختلفة.

الاتصال المتزامن المباشر يؤدي إلى اقتران قوي، زمن استجابة أعلى، وفشل الطلب عند تعطل Worker مؤقتًا.

## 2. محركات القرار

- المعالجة غير المتزامنة.
- امتصاص الحمل المفاجئ.
- توزيع الرسائل على Workers.
- Manual ACK وPublisher Confirms.
- Retry وDead-Lettering.
- تكامل Spring Boot وJava.
- سهولة المراقبة والتشغيل باستخدام Docker.

## 3. البدائل المدروسة

### RabbitMQ

مناسب لـWork Queues، يدعم Exchanges وRouting Keys وACK وDLX وTTL، لكنه يحتاج ضبط Retry وPrefetch ومعالجة التكرار.

### Apache Kafka

ممتاز للـEvent Streaming وإعادة القراءة، لكنه أعقد من حاجة المشروع الحالية، وRetry/DLQ فيه يحتاجان تصميمًا إضافيًا.

### Database Polling Queue

يبسط المعاملة، لكنه يضيف ضغطًا على PostgreSQL ويحتاج Locking وPolling معقدين عند التوسع.

### الاتصال المتزامن المباشر

أبسط مبدئيًا، لكنه لا يمتص الحمل ولا يعزل فشل Workers.

## 4. القرار

سيستخدم HLRMS **RabbitMQ** كوسيط الرسائل الرئيسي بين Request Service وWorker Services.

```text
Client → Request API → PostgreSQL + Outbox → RabbitMQ → Worker → Result
```

## 5. المبررات

المشكلة الأساسية هي توزيع مهام غير متزامنة مع ACK وRetry وDLQ وRouting واضح، وهذا يتطابق مع نموذج RabbitMQ أكثر من منصة Event Streaming.

## 6. النتائج الإيجابية

- فصل الاستقبال عن التنفيذ.
- سرعة استجابة `POST /requests`.
- التوسع الأفقي للـWorkers.
- دعم Retry وDLQ.
- عزل فشل Worker عن API.
- مراقبة Queue Depth وConsumer Count.

## 7. النتائج السلبية والمخاطر

- مكون بنية تحتية إضافي.
- احتمال تكرار التسليم.
- ضرورة ضبط ACK وPrefetch وRetry.
- احتمال تراكم Backlog.
- لا يوجد Exactly-Once تلقائيًا.

## 8. إجراءات التخفيف

- Publisher Confirms.
- Transactional Outbox.
- Manual ACK بعد نجاح Database Transaction.
- Idempotent Consumers.
- Maximum Retry Attempts وDLQ.
- مراقبة Queue Depth وUnacked Messages.
- Quorum Queues في الإنتاج.

## 9. تفاصيل التنفيذ

```text
hlrms.requests.exchange
hlrms.retry.exchange
hlrms.dlx.exchange

hlrms.requests.high.q
hlrms.requests.normal.q
hlrms.requests.low.q
hlrms.retry.q
hlrms.dlq
```

## 10. معايير القبول

- [ ] API لا ينتظر Worker.
- [ ] الرسائل توجه حسب الأولوية.
- [ ] ACK بعد نجاح تخزين النتيجة.
- [ ] الفشل المؤقت يعاد، والنهائي يذهب إلى DLQ.
- [ ] يمكن تشغيل عدة Workers.
- [ ] المقاييس ظاهرة في Prometheus.

## 11. شروط إعادة المراجعة

يعاد التقييم عند التحول إلى Event Streaming، أو الحاجة إلى إعادة قراءة تاريخية شاملة، أو عدم قدرة RabbitMQ على تحقيق الحمل المطلوب.
