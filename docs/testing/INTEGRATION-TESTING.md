# Integration Testing

## Dependencies

تستخدم `application-test.yml` عناوين محلية:

- PostgreSQL: `localhost:5432/hlrms_requests`
- Redis: `localhost:6379`
- RabbitMQ: `localhost:5672`

تشغيلها:

```bash
docker compose up -d postgres redis rabbitmq
```

## السيناريوهات الأساسية

| المجال | السيناريو |
|---|---|
| Request + Outbox | يحفظ الصفان داخل Transaction واحدة |
| User ownership | المستخدم لا يقرأ طلب مستخدم آخر |
| Database Idempotency | القيد الفريد يحسم السباق بين نسختي Request Service |
| Redis Idempotency | Save/Read/TTL/Delete والبيانات التالفة |
| Distributed Lock | Acquire/Release/Timeout وعدم تحرير قفل مالك آخر |
| Request Cache | تخزين الحالات النهائية وTTL وفشل Redis |
| Worker E2E | رسالة RabbitMQ تحول طلبًا PENDING إلى COMPLETED |
| Duplicate event | eventId نفسه لا يعالج مرة ثانية |
| Retry exhaustion | الطلب FAILED والرسالة إلى DLQ |

## التنظيف

تستخدم الاختبارات مفاتيح Redis ذات Prefix خاص، وتفرغ Queue المستخدمة، وتحذف صفوف الاختبار. لا تشغلها على Production Data.

## الدليل

يحفظ مخرج Maven لكل خدمة مع التاريخ وCommit Hash. لا يكتب Passed في التقرير اعتمادًا على وجود Test Classes فقط.
