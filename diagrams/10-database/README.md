# مخطط قاعدة البيانات لنظام HLRMS

يحتوي هذا المجلد على مخطط الكيانات والعلاقات  
**Entity Relationship Diagram (ERD)** وقالب أولي لمخطط PostgreSQL.

## الملفات

- `hlrms-database-erd.drawio`
- `hlrms-database-erd.png`
- `hlrms-database-erd.pdf`
- `database-schema.sql`

## الجداول الأساسية

### `client_system`

يمثل النظام الخارجي الذي يرسل الطلبات إلى HLRMS.

### `request`

يمثل الطلب الأساسي، ويحفظ نوعه، وأولويته، وحالته الحالية، والبيانات المرسلة، ومفتاح Idempotency.

### `request_status_history`

يحفظ سجلًا غير قابل للاستبدال لتغيرات حالة كل طلب.

### `processing_attempt`

يمثل كل محاولة منفصلة لمعالجة الطلب، بما يشمل العامل المنفذ، وأوقات التنفيذ، وسبب الفشل، وموعد المحاولة التالية.

### `processing_result`

يحفظ نتيجة محاولة المعالجة. العلاقة مع `processing_attempt` هي واحد إلى صفر أو واحد.

### `retry_policy`

يحفظ إعدادات إعادة المحاولة، مثل الحد الأقصى للمحاولات، والتأخير، واستراتيجية Backoff، وأنواع الأخطاء القابلة لإعادة المحاولة.

### `configuration_change`

يحفظ سجل التغييرات التي يجريها مسؤول النظام على الإعدادات التشغيلية.

### `outbox_event`

يدعم نمط **Transactional Outbox** لمنع فقدان الرسائل بين PostgreSQL وRabbitMQ.

## العلاقات

- `client_system 1 --- N request`
- `retry_policy 1 --- N request`
- `request 1 --- N request_status_history`
- `request 1 --- N processing_attempt`
- `processing_attempt 1 --- 0..1 processing_result`
- `request 1 --- N outbox_event`

## قرارات التصميم

1. استخدم `UUID` لمعرفات الطلبات والكيانات الموزعة.
2. استخدم `JSONB` للـPayload والنتائج المرنة دون ربط النظام بنوع طلب واحد.
3. أضيف `version` إلى جدول `request` لدعم Optimistic Locking.
4. قُيد رقم المحاولة ليكون فريدًا داخل كل طلب.
5. أضيف قيد Idempotency لكل Client System لمنع إنشاء طلبات مكررة.
6. فُصل سجل الحالات عن الطلب للحفاظ على تاريخ كامل لجميع الانتقالات.
7. أضيف جدول Outbox لدعم نشر الرسائل بصورة موثوقة.
8. ملف SQL يمثل مسودة تصميمية قابلة للتعديل عند بدء تنفيذ الـBackend.
