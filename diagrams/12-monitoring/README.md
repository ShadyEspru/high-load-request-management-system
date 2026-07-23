# بنية المراقبة والرصد لنظام HLRMS

يحتوي هذا المجلد على مخططات وتصميم أولي لمنظومة المراقبة الخاصة بنظام إدارة الطلبات عالية الحمل.

## الملفات

### مخطط البنية

- `monitoring-architecture.drawio`
- `monitoring-architecture.png`
- `monitoring-architecture.pdf`

يوضح العلاقة بين:

- Spring Boot Actuator
- Micrometer
- Prometheus
- Grafana
- Alertmanager
- RabbitMQ Prometheus Plugin
- PostgreSQL Exporter
- Redis Exporter
- Node Exporter أو cAdvisor

### مخطط المؤشرات والتنبيهات

- `metrics-and-alerts.drawio`
- `metrics-and-alerts.png`
- `metrics-and-alerts.pdf`

يوضح أهم فئات المقاييس:

- Requests
- Queues
- Workers
- Infrastructure

كما يوضح أهم التنبيهات المقترحة.

### ملفات الإعداد

- `prometheus.yml`
- `alerts.yml`

تمثل هذه الملفات مسودة أولية قابلة للاستخدام لاحقًا داخل Docker Compose.

## مسار البيانات

1. تعرض خدمات Spring Boot المقاييس عبر `/actuator/prometheus`.
2. تجمع Prometheus المقاييس دوريًا.
3. تعرض Grafana البيانات باستخدام PromQL.
4. تقيم Prometheus قواعد التنبيه.
5. تستقبل Alertmanager التنبيهات وتجمعها وتوجهها.
6. يصل التنبيه إلى مسؤول النظام عبر القناة المعتمدة.

## مؤشرات الطلبات

- عدد الطلبات المستلمة.
- عدد الطلبات الناجحة.
- عدد الطلبات الفاشلة.
- معدل النجاح والفشل.
- زمن الاستجابة للـAPI.
- الزمن الكامل من الاستلام حتى الوصول إلى حالة نهائية.

## مؤشرات RabbitMQ

- عدد الرسائل الجاهزة.
- عدد الرسائل غير المؤكدة.
- عدد Consumers.
- معدلات Publish وDeliver وACK.
- عدد رسائل Retry.
- عدد الرسائل التي انتقلت إلى DLQ.

## مؤشرات Workers

- عدد Workers النشطة.
- عدد الطلبات الجاري تنفيذها.
- زمن المعالجة.
- عدد محاولات التنفيذ.
- عدد الفشل القابل للاسترداد وغير القابل للاسترداد.
- وقت آخر Heartbeat.

## مؤشرات البنية التحتية

- استخدام CPU والذاكرة.
- ذاكرة JVM وGarbage Collection.
- عدد Threads.
- اتصالات HikariCP.
- حالة PostgreSQL وRedis.
- حالة Containers والشبكة والتخزين.

## التنبيهات المقترحة

- `HighRequestFailureRate`
- `QueueBacklogGrowing`
- `NoActiveWorkers`
- `DLQMessagesDetected`
- `HighProcessingLatency`
- `ServiceDown`

## ملاحظة مهمة

قيم Thresholds الحالية هي قيم أولية فقط. يجب ضبطها بعد تنفيذ اختبارات الحمل باستخدام k6، بناءً على الأداء الحقيقي للنظام ومتطلبات NFR المعتمدة.
