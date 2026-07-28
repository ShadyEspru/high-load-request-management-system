# تثبيت التقنيات المعتمدة

## Backend
- Java 21
- Spring Boot 3.x
- Spring Web
- Spring AMQP
- Spring Data JPA
- Spring Validation
- Spring Security لاحقًا عند الحاجة
- Maven

## Messaging
- RabbitMQ 4.x
- Direct وTopic Exchanges
- Durable Queues
- Manual Acknowledgement
- Publisher Confirms
- Retry Queues
- Dead Letter Exchanges

## Data
- PostgreSQL 17
- Redis 8 لمنع التكرار والتخزين المؤقت القصير
- Flyway لإدارة تغييرات قاعدة البيانات

## Monitoring
- Prometheus
- Grafana
- RabbitMQ Management UI
- Spring Boot Actuator
- Micrometer
- Loki اختياري للسجلات إذا سمح الوقت

## Clients and Testing
- Android: Kotlin + Jetpack Compose
- API documentation: OpenAPI / Swagger
- Load testing: k6
- API testing: Postman أو Bruno

## Deployment
- Docker
- Docker Compose
- GitHub Actions للتكامل المستمر

## Diagrams and Documentation
- Draw.io
- Markdown للمستندات داخل المستودع
- التقرير النهائي باستخدام Word أو Google Docs

## قرار معماري
يبدأ التنفيذ بثلاث خدمات قابلة للتشغيل مستقلًا: API Gateway، Request Service، Worker Service. يمكن تشغيل عدة نسخ من Worker Service لإثبات توزيع الحمل، دون تحويل المشروع إلى عدد كبير من الخدمات المصغرة.
