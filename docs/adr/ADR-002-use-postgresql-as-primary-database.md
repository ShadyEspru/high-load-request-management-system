# ADR-002: استخدام PostgreSQL كقاعدة البيانات الأساسية

- **Status:** Accepted and Implemented

## القرار

تستخدم PostgreSQL 17 كمصدر الحالة المتينة. يدير Flyway المخططات.

## قواعد البيانات الأساسية

- `hlrms_auth`: users، roles، user_roles، refresh_tokens.
- `hlrms_requests`: requests، outbox_events، processed_events.
- قاعدة Demo مستقلة عن قلب HLRMS.

## ضمانات التنفيذ

- Transaction واحدة لإنشاء Request وOutbox Event.
- Unique `(user_id, idempotency_key)` لحسم الطلبات المتزامنة.
- Primary Key على event_id لمنع تكرار الاستهلاك.
- Optimistic version في requests.
- Indexes على status وcreated_at وrequest_type وuser ownership وOutbox pending scans.

## النتائج

تعطي PostgreSQL اتساقًا ومعاملات وعلاقات وفهارس مناسبة للمشروع. عند التوسع الكبير تصبح Connection Pool وI/O وReplication وPartitioning مواضيع Capacity Planning مستقلة.
