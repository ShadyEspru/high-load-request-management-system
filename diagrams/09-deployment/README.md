# مخططات النشر لنظام HLRMS

يحتوي هذا المجلد على مخططات النشر الخاصة بنظام إدارة الطلبات عالية الحمل  
**High-Load Request Management System (HLRMS)**.

## 1. مخطط النشر العام

الملفات:

- `deployment-overview.drawio`
- `deployment-overview.png`
- `deployment-overview.pdf`

يوضح هذا المخطط العقد التشغيلية الأساسية:

- جهاز العميل (Client Device)
- عقدة الوصول (Edge / API Node)
- عنقود خدمات التطبيق (Application Services Cluster)
- البنية التحتية للبيانات والرسائل
- عقدة المراقبة (Monitoring Node)

كما يوضح نشر:

- Android App
- Nginx أو Load Balancer
- API Gateway
- Request Service
- Worker Services
- PostgreSQL
- RabbitMQ
- Redis
- Prometheus
- Grafana
- Alertmanager

## 2. مخطط Docker Compose

الملفات:

- `docker-compose-topology.drawio`
- `docker-compose-topology.png`
- `docker-compose-topology.pdf`

يوضح الخدمات المقترحة داخل ملف `docker-compose.yml`:

- `android-client`
- `api-gateway`
- `request-service`
- `worker-1`
- `worker-2`
- `postgres`
- `rabbitmq`
- `redis`
- `prometheus`
- `grafana`

كما يوضح الشبكة المشتركة والحجوم الدائمة:

- `hlrms-network`
- `postgres-data`
- `rabbitmq-data`

## قرارات التصميم

1. يتم نشر Worker Services كنسخ متعددة لإظهار قابلية التوسع الأفقي.
2. تستخدم PostgreSQL لتخزين حالة الطلب وسجل الحالات والمحاولات والنتائج.
3. تستخدم RabbitMQ للطوابير الرئيسية وطوابير Retry وDead Letter Queue.
4. يستخدم Redis للبيانات السريعة وRate Limiting وIdempotency.
5. تجمع Prometheus المقاييس، وتعرض Grafana لوحات المتابعة.
6. يستخدم Docker Compose في بيئة التطوير والعرض، مع إمكانية نقل التصميم لاحقًا إلى بيئة حاويات موزعة.
