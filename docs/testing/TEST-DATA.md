# بيانات الاختبار

## حسابات الاختبار

تنشأ الحسابات عبر `POST /api/v1/auth/register`. لا تحفظ كلمات المرور أو Access Tokens داخل المستودع. تمرر إلى k6 عبر `AUTH_TOKEN` أو `TEST_USERNAME/TEST_PASSWORD`.

## طلب عام

```json
{
  "requestType": "STANDARD",
  "payload": "{\"source\":\"test\",\"operation\":\"integration\"}"
}
```

## بيانات Performance

يضيف k6 القيم الآتية إلى Payload:

- `source: k6`
- `operation: performance-test`
- `testRunId`
- `uniqueId`

ويولد `Idempotency-Key` من TEST_RUN_ID وVU وIteration. يجب ألا يعاد استخدام المفتاح لبيانات مختلفة.

## عزل البيانات

- Prefix منفصل لمفاتيح Redis الاختبارية.
- TEST_RUN_ID مستقل لكل تشغيل.
- Snapshot للعدادات قبل وبعد الاختبار.
- حذف بيانات الاختبار لا يتم قبل حفظ Reconciliation Evidence.
