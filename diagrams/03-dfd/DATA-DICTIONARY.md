# قاموس تدفقات البيانات — DFD Level 1

يعرّف هذا المستند التدفقات `F1–F13` الظاهرة في مخطط `dfd-level1`. الأسماء تمثل البيانات المتبادلة منطقيًا، ولا تعني بالضرورة وجود اتصال شبكي مستقل لكل اتجاه.

| التدفق | المصدر | الوجهة | المحتوى |
|---|---|---|---|
| `F1` | `Client System` | `1.0 Authenticate` | بيانات التسجيل أو تسجيل الدخول أو Refresh Token، بحسب العملية المطلوبة. |
| `F2` | `1.0 Authenticate` | `D1 · Auth Database` | قراءة أو كتابة المستخدمين والأدوار وRefresh Tokens اللازمة لإصدار JWT والتحقق منه. |
| `F3` | `Client System` | `2.0 Admit Request` | JWT و`Idempotency-Key` و`Request Type` وPayload ومعرّفات التتبع. |
| `F4` | `2.0 Admit Request` | `D2 · Redis` | قفل ضمن نطاق المستخدم وبيانات Replay Cache ونتيجة Idempotency Fast Path. |
| `F5` | `2.0 Admit Request` | `3.0 Persist Work` | هوية موثوقة وطلب متحقق منه وقرار قبول عملية جديدة بدل Replay أو Conflict. |
| `F6` | `3.0 Persist Work` | `D3 · Requests Database` | إدراج Request وOutbox Event في Transaction واحدة وإرجاع Request ID والحالة `PENDING`. |
| `F7` | `4.0 Publish Event` | `D3 · Requests Database` | Claim لدفعة Outbox بالحالة `PENDING`، ثم تحديثها إلى `PUBLISHED` بعد Publisher Confirm. |
| `F8` | `4.0 Publish Event` | `D4 · RabbitMQ` | حدث `request.created` المتين مع Event ID وRequest ID وPayload وبيانات التتبع. |
| `F9` | `D4 · RabbitMQ` | `5.0 Process Work` | تسليم الحدث أو إعادة تسليمه وفق ضمان `At-Least-Once` وسياسة DLQ. |
| `F10` | `5.0 Process Work` | `D3 · Requests Database` | تسجيل Event ID لمنع التكرار، وتحديث `PROCESSING` ثم `COMPLETED` أو `FAILED` مع النتيجة أو الخطأ. |
| `F11` | `Client System` | `6.0 Query Status` | JWT وRequest ID لطلب الحالة والنتيجة. |
| `F12` | `6.0 Query Status` | `D3 · Requests Database` | قراءة Request مقيّدة بملكية المستخدم، بما يشمل Status وResult وError والتوقيتات. |
| `F13` | `6.0 Query Status` | `Client System` | Request ID والحالة الحالية والنتيجة أو الخطأ، أو استجابة Not Found عند عدم الملكية أو عدم وجود الطلب. |

## ملاحظات

- `Redis` يسرّع مسار Idempotency، بينما تبقى PostgreSQL هي مصدر الحقيقة المتين.
- يحفظ `Request` و`Outbox Event` في Transaction واحدة لمنع ضياع العمل المقبول قبل النشر.
- قد يعيد RabbitMQ تسليم الحدث؛ لذلك يمنع جدول `processed_events` تكرار أثر الأعمال.
- الحالات المتينة في المجال الأساسي هي `PENDING` و`PROCESSING` و`COMPLETED` و`FAILED`.
