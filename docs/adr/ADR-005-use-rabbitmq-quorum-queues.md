# ADR-005: استخدام RabbitMQ Quorum Queues

- **Status:** Accepted
- **Date:** 2026-07-23
- **Decision Owners:** HLRMS Team
- **Related Requirements:** High Availability, Message Durability, Broker Failure Tolerance
- **Related ADRs:** ADR-001, ADR-004

## 1. السياق

تحفظ Queues طلبات لم تعالج بعد. Durable Queue وPersistent Messages وحدهما لا يقدمان تحمل فشل عقدة بصورة كافية إذا كانت الرسائل موجودة على عقدة واحدة.

## 2. محركات القرار

- Replication.
- تحمل فشل Node.
- تكامل مع Publisher Confirms.
- Failure Recovery واضح.
- تقليل خطر فقد الرسائل المقبولة.

## 3. البدائل المدروسة

### Classic Queue بعقدة واحدة

بسيطة وقليلة الموارد، لكنها نقطة فشل واحدة.

### Classic Mirrored Queues

نموذج أقدم وأعقد تشغيليًا وأقل ملاءمة من Quorum Queues للتصميم الجديد.

### Quorum Queues

توفر Replication وConsensus وFailover أوضح، لكنها تستهلك قرصًا وشبكة أكثر.

### Streams

ممتازة لإعادة القراءة والاحتفاظ، لكنها نموذج مختلف عن Work Queue الحالية.

## 4. القرار

ستستخدم Queues الإنتاجية:

```text
x-queue-type = quorum
```

وتشمل:

```text
hlrms.requests.high.q
hlrms.requests.normal.q
hlrms.requests.low.q
hlrms.retry.q
hlrms.dlq
```

يمكن استخدام Classic Queue في التطوير المحلي أحادي العقدة، لكن ذلك لا يمثل ضمانات الإنتاج.

## 5. نموذج النشر

```text
RabbitMQ Cluster
├── rabbitmq-1
├── rabbitmq-2
└── rabbitmq-3
```

ثلاث عقد تسمح باستمرار الأغلبية عند فشل عقدة واحدة، مع توزيع العقد على Failure Domains مختلفة قدر الإمكان.

## 6. الموثوقية الكاملة

Quorum Queue لا تكفي وحدها. يجب الجمع بين:

- Durable Exchange.
- Quorum Queue.
- Persistent Message.
- Publisher Confirm.
- Manual ACK.
- Transactional Outbox.
- Idempotent Consumer.

## 7. النتائج الإيجابية

- تحمل فشل عقدة.
- Replication للرسائل.
- Failover أوضح.
- ملاءمة للطلبات المهمة وDLQ.

## 8. النتائج السلبية والمخاطر

- استهلاك قرص وشبكة أكبر.
- الحاجة إلى ثلاث عقد.
- خطر فقد التوفر عند فقد الأغلبية.
- الحاجة إلى Capacity Planning ومراقبة دقيقة.

## 9. إجراءات التخفيف

- Capacity Testing.
- تحديد Message Size.
- مراقبة Disk وQueue Length وConfirm Latency.
- نشر العقد على Hosts مختلفة.
- SSD في الإنتاج.
- Retention للـDLQ.
- اختبار Node Failure واستعادته.

## 10. الأولويات

تستخدم Queues منفصلة لـHIGH وNORMAL وLOW بدل Queue Priority واحدة، لتحقيق عزل ومراقبة وتوسع مستقل. يجب منع Starvation للطلبات منخفضة الأولوية بسياسة Scheduling مناسبة.

## 11. Retry وDLQ

- Retry محدود ويستخدم TTL أو آلية تأخير معتمدة.
- DLQ لا يعاد تشغيلها تلقائيًا بلا مراجعة.
- وجود رسائل في DLQ يطلق Metric وAlert.
- إعادة المعالجة عملية إدارية مدققة.

## 12. معايير القبول

- [ ] Queues الإنتاجية من نوع Quorum.
- [ ] ثلاث عقد في نموذج الإنتاج.
- [ ] استمرار الخدمة عند فشل عقدة واحدة.
- [ ] Publisher Confirms وPersistent Messages وManual ACK.
- [ ] Metrics وAlerts للقرص والأغلبية والـBacklog.
- [ ] توثيق الفرق بين Development وProduction.

## 13. شروط إعادة المراجعة

عند الانتقال إلى Managed Broker، أو اعتماد Streams، أو تغير Throughput وحجم الرسائل جذريًا، أو تغير RPO/RTO.
