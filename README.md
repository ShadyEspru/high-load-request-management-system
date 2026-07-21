# نظام إدارة الطلبات عالية الحمل

**English:** High-Load Request Management System  
**اختصار المشروع:** HLRMS

منصة عامة لاستقبال الطلبات عالية الحمل، تنظيمها داخل RabbitMQ، وتوزيعها على وحدات تنفيذ متوازية، مع التتبع وإعادة المحاولة وDead Letter Queue والمراقبة المباشرة.

تطبيق الحوالات المالية هو حالة استخدام تجريبية فقط، وليس محور النظام.

## المكونات

- API Gateway
- Request Service
- RabbitMQ
- Worker Service قابل لتشغيل نسخ متعددة
- PostgreSQL
- Redis
- Prometheus + Grafana
- Android Demo Client
- k6 Load Generator
- Docker Compose

## بنية المستودع

- `backend/`: خدمات النظام الأساسية
- `clients/`: التطبيق التجريبي وأداة توليد الضغط
- `infrastructure/`: Docker وRabbitMQ والمراقبة وقاعدة البيانات
- `docs/`: التقرير والمتطلبات والمعمارية والاختبارات
- `diagrams/`: ملفات Draw.io مرتبة حسب النوع
- `scripts/`: أوامر التشغيل والمساعدة

## منهجية الفروع

- `main`: النسخة المستقرة فقط
- `develop`: دمج العمل اليومي
- `feature/<name>`: كل ميزة مستقلة
- `docs/<name>`: التقرير والمخططات
- `fix/<name>`: إصلاح الأخطاء

## قاعدة الدمج

لا يُدمج أي فرع في `develop` إلا بعد مراجعة عضو آخر، ونجاح الاختبارات، وتحديث التوثيق المرتبط بالتغيير.
