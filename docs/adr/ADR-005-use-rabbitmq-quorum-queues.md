# ADR-005: RabbitMQ Quorum Queues

- **Status:** Proposed / Not Implemented
- **Date:** 2026-07-23
- **Related ADRs:** ADR-001, ADR-004

## السياق

يعمل المشروع الحالي على RabbitMQ بعقدة واحدة وDurable Classic Queues. يوفر ذلك متانة عبر Restart المحلي، لكنه لا يوفر Replication أو تحمل فشل عقدة Broker كاملة.

## المقترح

عند الانتقال إلى بيئة متعددة العقد يمكن دراسة:

```text
x-queue-type = quorum
```

مع RabbitMQ Cluster من ثلاث عقد موزعة على Failure Domains مختلفة.

## سبب عدم اعتماده حاليًا

- Docker Compose الحالي يشغل Broker واحدًا.
- QueueBuilder في Request Service وWorker لا يضبط `x-queue-type=quorum`.
- اختبارات الأداء الحالية لا تتضمن Cluster failover.
- تفعيل Quorum على عقدة منفردة لا يثبت High Availability.

## شروط الانتقال إلى Accepted

- إعداد Cluster فعلي من ثلاث عقد على Hosts مناسبة.
- تعديل Queue declarations والتأكد من توافق Existing Queues.
- قياس Publish Confirm latency وDisk/Network overhead.
- اختبار فقد عقدة وعودة الأغلبية.
- حفظ مخرجات Recovery وعدم فقد الطلبات المقبولة.

حتى تحقق الشروط لا يجوز وصف Quorum Queues بأنها ميزة منفذة في HLRMS الحالي.
