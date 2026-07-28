# بنية RabbitMQ لنظام HLRMS

يحتوي هذا المجلد على مخططات بنية RabbitMQ ومسار الرسائل داخل نظام إدارة الطلبات عالية الحمل.

## الملفات

### مخطط البنية

- `rabbitmq-topology.drawio`
- `rabbitmq-topology.png`
- `rabbitmq-topology.pdf`

يوضح:

- Exchanges
- Main Queues حسب الأولوية
- Retry Exchange وRetry Queue
- Dead-Letter Exchange
- Dead Letter Queue
- Producer وWorker Services
- Monitoring وRabbitMQ Management

### مخطط دورة حياة الرسالة

- `message-lifecycle.drawio`
- `message-lifecycle.png`
- `message-lifecycle.pdf`

يوضح الرحلة الكاملة:

1. إنشاء الرسالة في Request Service.
2. نشرها في `hlrms.requests.exchange`.
3. استلام Publisher Confirm من RabbitMQ.
4. توجيهها إلى Main Queue حسب Routing Key والأولوية.
5. استهلاك الرسالة بواسطة Worker.
6. تحديث الطلب إلى `PROCESSING`.
7. تنفيذ الطلب.
8. عند النجاح: تخزين النتيجة، تعيين `SUCCEEDED`، ثم إرسال ACK.
9. عند فشل قابل للاسترداد: تعيين `FAILED` ثم `RETRY_SCHEDULED`، والنشر إلى Retry Queue.
10. بعد انتهاء TTL تعاد الرسالة إلى Main Exchange.
11. عند تجاوز الحد الأقصى أو حدوث فشل غير قابل للاسترداد: تنقل إلى DLQ وتصبح `DEAD_LETTERED`.

## Exchanges

### `hlrms.requests.exchange`

- النوع: `topic`
- الغرض: توجيه الطلبات إلى الطوابير الرئيسية.
- Routing Keys:
  - `request.high`
  - `request.normal`
  - `request.low`

### `hlrms.retry.exchange`

- النوع: `direct`
- الغرض: استقبال الرسائل التي تحتاج إلى Retry Delay.

### `hlrms.dlx.exchange`

- النوع: `direct`
- الغرض: عزل الرسائل التي لا يمكن معالجتها.

## Queues

- `hlrms.requests.high.q`
- `hlrms.requests.normal.q`
- `hlrms.requests.low.q`
- `hlrms.retry.q`
- `hlrms.dlq`

## قرارات الموثوقية

- الرسائل `Persistent`.
- الـExchanges والـQueues من النوع `Durable`.
- استخدام `Publisher Confirms`.
- استخدام `mandatory=true` لاكتشاف الرسائل غير القابلة للتوجيه.
- استخدام `Manual ACK`.
- إرسال ACK فقط بعد نجاح معاملة قاعدة البيانات.
- عدم إعادة الرسائل الفاشلة عشوائيًا إلى الطابور الرئيسي.
- تصنيف الخطأ إلى قابل للاسترداد أو غير قابل للاسترداد.
- استخدام DLQ للتحليل وإعادة المعالجة اليدوية لاحقًا.
- يوصى باستخدام `Quorum Queues` في بيئة الإنتاج.

## ملف التعريف

يحتوي `rabbitmq-topology.yaml` على مسودة قابلة للتحويل لاحقًا إلى إعدادات Spring AMQP أو RabbitMQ Definitions.

## ملاحظة عن Retry Delay

تستخدم النسخة الحالية Retry Queue مع `TTL` وDead-Letter Exchange. عند الحاجة إلى فترات Retry مختلفة، يمكن إنشاء عدة طوابير مثل:

- `hlrms.retry.10s.q`
- `hlrms.retry.30s.q`
- `hlrms.retry.5m.q`

أو استخدام RabbitMQ Delayed Message Exchange Plugin إذا تم اعتماده ضمن بيئة النشر.
