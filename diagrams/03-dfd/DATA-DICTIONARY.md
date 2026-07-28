# قاموس تدفقات البيانات

## 1. Request Payload

الجهة المرسلة: Client System  
الجهة المستقبلة: عملية استقبال الطلب والتحقق.

المحتوى المقترح:

- Request Type
- Payload
- Priority
- Idempotency Key
- Client Identification
- Correlation ID

## 2. Validated Request

طلب اجتاز قواعد التحقق الأساسية، وأصبح جاهزًا لإنشاء السجل وتعيين الحالة المناسبة.

## 3. Accepted Request

طلب تم قبوله وتخزينه، ويحمل Request ID وحالة `ACCEPTED`.

## 4. Queued Message

رسالة RabbitMQ تتضمن:

- Request ID
- Request Type
- Priority
- Attempt Number
- Correlation ID
- Creation Timestamp

## 5. Failure Metadata

معلومات فشل محاولة المعالجة:

- Failure Type
- Failure Message
- Retryable Flag
- Attempt Number
- Worker ID
- Processing Duration

## 6. Retry Message

رسالة معاد نشرها بعد جدولة محاولة جديدة، وتتضمن رقم المحاولة التالية ووقت إعادة المحاولة.

## 7. Status / Metrics

بيانات حالة الطلب ومؤشرات الأداء التي تحتاج إليها واجهات التتبع والمراقبة.

## 8. Admin Queries

طلبات مسؤول النظام لعرض الطلبات والطوابير وWorkers والإعدادات ومؤشرات التشغيل.

## 9. Metrics / Health

مؤشرات Prometheus ونتائج Health Check المعروضة لمنصة المراقبة.

## 10. Request Data

بيانات الطلب المخزنة، بما يشمل الحالة الحالية والتوقيتات والـPayload.

## 11. Attempt / Result

بيانات محاولة المعالجة والنتيجة المرتبطة بها.

## 12. Retry Limits

سياسة إعادة المحاولة، مثل:

- Maximum Attempts
- Initial Delay
- Maximum Delay
- Backoff Strategy
- Retryable Failure Types
