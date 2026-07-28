# مخططات C4 لنظام HLRMS

يحتوي هذا المجلد على مخططات معمارية وفق نموذج **C4 Model** لنظام إدارة الطلبات عالية الحمل  
**High-Load Request Management System (HLRMS)**.

## الملفات

### 1. مخطط سياق النظام - C4 Level 1

- `c4-level1-system-context.drawio`
- `c4-level1-system-context.png`
- `c4-level1-system-context.pdf`

يوضح النظام بوصفه وحدة واحدة، والجهات والأنظمة الخارجية التي تتفاعل معه:

- النظام العميل (Client System)
- مسؤول النظام (System Administrator)
- منصة المراقبة (Monitoring Platform)
- مزوّد الهوية (Identity Provider)

### 2. مخطط الحاويات - C4 Level 2

- `c4-level2-container.drawio`
- `c4-level2-container.png`
- `c4-level2-container.pdf`

يوضح الحاويات الأساسية داخل HLRMS:

- API Gateway
- Request Service
- Worker Service
- PostgreSQL
- RabbitMQ
- Redis
- Metrics Endpoint

## العلاقات الأساسية

1. يرسل Client System الطلبات إلى API Gateway عبر HTTPS وJSON.
2. يمرر API Gateway الطلبات المصرح بها إلى Request Service.
3. يخزن Request Service الطلبات وحالاتها في PostgreSQL.
4. ينشر Request Service رسائل المعالجة في RabbitMQ.
5. يستهلك Worker Service الرسائل ويحدث نتائج المعالجة.
6. يستخدم النظام Redis للبيانات السريعة، وRate Limiting، وIdempotency.
7. تعرض الخدمات Health وMetrics عبر Spring Boot Actuator وMicrometer.
8. تجمع منصة المراقبة هذه البيانات باستخدام Prometheus، ويمكن عرضها عبر Grafana.

## ملاحظات تصميمية

- يمثل **Container** في C4 تطبيقًا أو مخزن بيانات قابلًا للتشغيل والنشر بصورة مستقلة.
- لا يمثل المخطط تفاصيل Classes أو Packages الداخلية.
- تم إبقاء أسماء التقنيات والعناصر البرمجية باللغة الإنجليزية، مع شرح العلاقات باللغة العربية.
- ملفات Draw.io وPNG وPDF تستخدم التصميم البصري نفسه لتجنب الاختلاف بين النسخ.

## المتطلبات المرتبطة

- FR-001 إلى FR-005: استقبال الطلب والتحقق منه.
- FR-006 إلى FR-008: دورة حياة الطلب.
- FR-009 إلى FR-012: إدارة الطوابير.
- FR-013 إلى FR-020: المعالجة وإعادة المحاولة وDLQ.
- FR-021 إلى FR-023: تتبع الطلبات.
- FR-024 إلى FR-025: المراقبة.
- FR-026 إلى FR-029: إدارة الإعدادات.
