# ADR-004: استخدام Transactional Outbox

- **Status:** Accepted and Implemented
- **Related:** ADR-001، ADR-002

## المشكلة

حفظ Request ثم نشر RabbitMQ عمليتان لا تشكلان Distributed Transaction. فشل التطبيق بينهما قد يترك Request بلا رسالة، أو Message بلا حالة متينة.

## القرار

تنشئ `RequestCreationTransactionService` صف Request وصف Outbox Event داخل PostgreSQL Transaction واحدة. يعيد API النجاح بعد Commit فقط.

يعالج دور Outbox Publisher الأحداث كالتالي:

```text
PENDING → PROCESSING → PUBLISHED
                    ↘ FAILED
```

1. يستعيد الأحداث التي انقطعت أثناء PROCESSING عند بدء التطبيق.
2. يطالب Batch محدودًا.
3. يرسل الأحداث بصورة غير متزامنة.
4. ينتظر Publisher Confirm لكل رسالة.
5. يحدث الدفعة المؤكدة إلى PUBLISHED.
6. يسجل Failure ويقيد Retry count عند الخطأ.

## الإكمال من جهة المستهلك

قد يعاد نشر أوتسليم الحدث؛ لذلك يسجل Worker `event_id` في `processed_events` قبل تنفيذ العمل. يغلق ذلك فجوة At-Least-Once من جهة Consumer.

## معيار التحقق

بعد Recovery:

```text
requests accepted = outbox created = outbox published = processed events
```

مع `PENDING=0`, `PROCESSING=0`, Queue drain، وعدم زيادة DLQ غير المتوقعة.
