# مخططات قواعد البيانات النهائية - HLRMS

يحتوي هذا المجلد على الحزمة النهائية لمخططات قواعد البيانات في نظام **High-Load Request Management System (HLRMS)**.

## مصدر الحقيقة

تم بناء محتوى مخططات ERD بالاعتماد على ملفات **Flyway migrations** الفعلية في المشروع، وليس على مخطط التصميم القديم:

- `backend/request-service/src/main/resources/db/migration/`
- `backend/auth-service/src/main/resources/db/migration/`
- `android-app/transfer-api/src/main/resources/db/migration/`

## المخططات

### 1. `requests-database-erd`
يمثل قاعدة `hlrms_requests` ويعرض:

- `requests`
- `outbox_events`
- `processed_events`

العلاقة بين `processed_events.request_id` و`requests.id` هي **Physical Foreign Key** حقيقية مع `ON DELETE CASCADE`.
أما `outbox_events.aggregate_id` فهو **Logical Reference** إلى Request Aggregate ولا توجد عليه Foreign Key فعلية.

### 2. `authentication-database-erd`
يمثل قاعدة `hlrms_auth` ويعرض:

- `users`
- `roles`
- `user_roles`
- `refresh_tokens`

جميع علاقات هذا المخطط هي Foreign Keys فعلية كما هي معرفة في Flyway.

### 3. `transfer-demo-database-erd`
يمثل قاعدة `hlrms_transfer` الخاصة بحالة الاستخدام التجريبية للحوالات ويعرض:

- `transfer_profiles`
- `wallet_balances`
- `transfer_transactions`

العلاقة الفيزيائية الأساسية هي:

`wallet_balances.user_id -> transfer_profiles.user_id`

أما `request_id` وحقول هوية المرسل والمستلم فهي مراجع منطقية عبر حدود المجالات، وليست Cross-Database Foreign Keys.

### 4. `database-landscape`
يوضح ملكية البيانات على مستوى المجالات والخدمات:

- Auth Service -> `hlrms_auth`
- Request Service -> `hlrms_requests`
- Transfer API (Demo) -> `hlrms_transfer`

العلاقات بين هذه القواعد هي **Logical Cross-Domain References** فقط، ولا توجد Foreign Keys بين قواعد البيانات المختلفة.

## الصيغ

لكل مخطط ثلاث صيغ:

- `.png`: للاستخدام في GitHub والعروض والتوثيق الرقمي.
- `.pdf`: للتقرير والطباعة.
- `.drawio`: مصدر Draw.io يحافظ على التصميم المعتمد بدقة ويمكن فتحه في diagrams.net / Draw.io وإضافة أو تعديل عناصر الصفحة.

كما يحتوي المجلد على:

- `hlrms-database-diagrams.pdf`: ملف PDF موحد من أربع صفحات يضم المخططات الأربعة.

## دلالة العلاقات

- الخط المتصل: Physical Foreign Key.
- الخط المتقطع: Logical Reference بدون Physical Foreign Key.
- `PK`: Primary Key.
- `FK`: Foreign Key.
- `UQ`: Unique Constraint.

## ملاحظة حول حدود البيانات

HLRMS يفصل ملكية البيانات حسب حدود المجال. لذلك لا تُرسم علاقة Foreign Key بين قاعدتين منفصلتين ما لم تكن موجودة فعلًا في قاعدة البيانات. المعرّفات العابرة للخدمات مثل `user_id` و`request_id` تُعرض كمراجع منطقية فقط.
