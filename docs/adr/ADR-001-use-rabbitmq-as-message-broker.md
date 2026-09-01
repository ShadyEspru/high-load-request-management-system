# ADR-001: استخدام RabbitMQ كوسيط رسائل

- **Status:** Accepted and Implemented
- **Related:** ADR-004

## القرار

يستخدم HLRMS RabbitMQ لفصل زمن قبول HTTP عن زمن التنفيذ. التكوين الحالي:

- Direct Exchange: `hlrms.request.exchange`
- Routing Key: `request.created`
- Durable Queue: `hlrms.request.processing.queue`
- DLX: `hlrms.request.dlx`
- DLQ: `hlrms.request.processing.dlq`
- Dead-letter Routing Key: `request.failed`

يستخدم Producer Publisher Confirms وMandatory publishing. يستخدم Worker Listener acknowledgements التلقائية بعد نجاح المعالجة، مع Retry داخلي ثم Message Recoverer يرفض الرسالة نهائيًا لتصل إلى DLQ.

## النتائج

- API لا ينتظر تنفيذ Worker.
- يمكن زيادة عدد Worker Replicas.
- Redelivery محتملة، لذلك يلزم Idempotent Consumer.
- Broker الحالي عقدة واحدة؛ لا يمثل High Availability Cluster.

## غير منفذ حاليًا

- Priority queues متعددة.
- Retry queue مستقلة مع TTL.
- Quorum queues.
- RabbitMQ cluster متعدد العقد.
