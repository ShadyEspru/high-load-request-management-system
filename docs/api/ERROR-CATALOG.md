# دليل أخطاء API

| HTTP | السبب المعتاد | المعالجة لدى Client |
|---:|---|---|
| `400` | Validation أو Idempotency-Key مفقود أو Query غير صالح | تصحيح الطلب وعدم إعادة المحاولة دون تغيير |
| `401` | JWT مفقود/منتهي أو Refresh Token غير صالح | تجديد الرمز أو إعادة Login |
| `403` | الدور لا يسمح بالعملية | عدم إعادة المحاولة بنفس الهوية |
| `404` | الطلب غير موجود أو غير مملوك للمستخدم | التحقق من Request ID |
| `409` | البريد موجود، أو Idempotency-Key استُخدم لمحتوى مختلف | استخدام مفتاح جديد للعملية الجديدة |
| `429` | Rate Limiter رفض طلب قراءة أو Admin | Exponential Backoff واحترام Retry policy |
| `500` | خطأ داخلي غير متوقع | إعادة المحاولة فقط للعمليات Idempotent مع تسجيل Correlation ID |
| `503` | Dependency غير متاحة أو Circuit Breaker Fallback | Backoff ثم إعادة المحاولة؛ فحص Health إذا كان المشغل |

شكل خطأ Request Service:

```json
{
  "timestamp": "2026-08-31T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Idempotency key was already used with a different request"
}
```

قد يضيف Validation الحقل `details`. أخطاء Auth Service تتضمن أيضًا `path` و`validationErrors` عند الحاجة.
