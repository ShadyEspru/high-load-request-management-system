# دورة حياة الطلب

الحالات المخزنة فعليًا في `RequestStatus` هي أربع فقط:

| الحالة | المعنى | نهائية |
|---|---|---|
| `PENDING` | حُفظ الطلب وحدث Outbox في Transaction واحدة، وينتظر النشر أو الاستهلاك | لا |
| `PROCESSING` | سجل Worker الحدث وبدأ تنفيذ الطلب | لا |
| `COMPLETED` | حُفظت نتيجة التنفيذ بنجاح | نعم |
| `FAILED` | حُفظ سبب فشل نهائي أو استنفدت محاولات المعالجة | نعم |

## المسار الطبيعي

```text
PENDING → PROCESSING → COMPLETED
```

## مسار الفشل

```text
PENDING → PROCESSING → FAILED
```

وقد ينتقل الطلب إلى `FAILED` عند استنفاد Retry حتى لو تعذر إكمال المعالجة المنطقية.

## ما ليس حالة طلب

العناصر الآتية سلوك Messaging أو Outbox وليست قيمًا في جدول requests:

- `PROCESSING` و`PUBLISHED`: حالتان في `outbox_events` وليستا حالتي Request.
- Retry وRedelivery: سلوك Spring AMQP وRabbitMQ.
- DLQ: موقع الرسالة بعد رفضها النهائي.
- `RECEIVED`, `VALIDATING`, `ACCEPTED`, `QUEUED`, `SUCCEEDED`, `CANCELLED`: غير منفذة كحالات مخزنة.

إبقاء الحالات الأربع فقط يمنع توثيق State Machine لا يطابق الكود.
