# مواصفات حالات الاستخدام

## الجهات المتفاعلة

| Actor | الوصف |
|---|---|
| End User | يستخدم تطبيقًا أو موقعًا مرتبطًا بالمنصة |
| Client System | Mobile/Web/Partner software يستدعي REST API |
| System Administrator | يقرأ كل الطلبات ويراقب الصحة والمقاييس |
| Replaceable Business Service | Adapter اختياري ينفذ منطق مجال خارجي |

## UC-001 التسجيل وتسجيل الدخول

- **Actor:** End User عبر Client System.
- **Precondition:** Auth Service متاحة.
- **Flow:** Register أو Login، تحقق البيانات، إصدار Access/Refresh Token.
- **Alternatives:** بريد مكرر 409، بيانات غير صالحة 400، Credentials خاطئة 401.
- **Postcondition:** يمتلك Client JWT صالحًا.

## UC-002 إرسال طلب

- **Actor:** Client System.
- **Precondition:** JWT صالح وIdempotency-Key موجود.
- **Flow:** Gateway يتحقق من الهوية، Request Service يفحص Idempotency، يحفظ Request وOutbox Event في Transaction واحدة، ثم يعيد Request ID وحالة PENDING.
- **Replay:** المفتاح والمحتوى نفسيهما يعيدان الطلب السابق بـ200.
- **Conflict:** المفتاح نفسه مع محتوى مختلف يعيد 409.
- **Failure:** إذا فشل Database commit فلا يعتبر الطلب مقبولًا.

## UC-003 تتبع طلب

- **Actor:** Client System.
- **Precondition:** JWT وRequest ID.
- **Flow:** `GET /api/v1/requests/{id}`، تحقق الملكية، إعادة الحالة والنتيجة.
- **Alternative:** ID غير موجود أو غير مملوك يعيد 404.
- **Postcondition:** يعرف Client إن كانت الحالة PENDING أو PROCESSING أو COMPLETED أو FAILED.

## UC-004 عرض طلبات المستخدم

- **Actor:** Client System.
- **Flow:** `GET /api/v1/requests` مع status/page/size اختياريًا.
- **Result:** صفحة مرتبة حسب createdAt تنازليًا ولا تضم طلبات مستخدم آخر.

## UC-005 معالجة غير متزامنة

- **Actor:** النظام داخليًا.
- **Flow:** Publisher يطالب Outbox، ينشر مع Confirm، Worker يسجل eventId، يحدث PROCESSING، ينفذ، ثم COMPLETED أو FAILED.
- **Duplicate:** إذا كان eventId مسجلًا يتجاهل Worker الرسالة.
- **Failure:** بعد استنفاد Retry يصبح الطلب FAILED وتنتقل الرسالة إلى DLQ.

## UC-006 قراءة إدارية

- **Actor:** System Administrator.
- **Precondition:** JWT يحوي Role باسم ADMIN.
- **Flow:** قراءة كل الطلبات أو طلب محدد عبر `/api/v1/admin/requests`.
- **Alternative:** المستخدم العادي يحصل على 403.

## UC-007 مراقبة النظام

- **Actor:** System Administrator.
- **Flow:** Prometheus يجمع Metrics وتعرض Grafana الصحة وRPS وLatency وErrors وWorker وRabbitMQ وPostgreSQL وRedis.
- **قيد:** Dashboard لا يمثل دليل Benchmark إلا عند ربط اللقطات بـTEST_RUN_ID ونتائج k6 والتوفيق.

## UC-008 دمج Client قابل للاستبدال

- **Actor:** فريق تطبيق خارجي.
- **Flow:** يطبق Auth وPOST وRequest ID وPolling وفق عقد API.
- **Postcondition:** يعمل التطبيق فوق HLRMS دون معرفة تفاصيل RabbitMQ أو Workers.
- **قيد:** Demo Client الموجود مثال تحقق وليس نطاق المشروع.
