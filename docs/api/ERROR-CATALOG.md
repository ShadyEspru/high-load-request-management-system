# دليل أخطاء HLRMS
## Error Catalog

## 1. نموذج الخطأ

```json
{
  "timestamp": "2026-07-23T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "فشل التحقق من بيانات الطلب.",
  "path": "/api/v1/requests",
  "correlationId": "87d761f6-7e85-4c8b-b3eb-d70f939e933a",
  "details": {},
  "fieldErrors": [
    {
      "field": "requestType",
      "code": "NOT_BLANK",
      "message": "نوع الطلب مطلوب."
    }
  ]
}
```

لا يجوز إعادة Stack Trace أو SQL أو الأسرار أو تفاصيل الخوادم.

---

# 2. المصادقة والصلاحيات

| HTTP | الرمز | المعنى |
|---:|---|---|
| `401` | `AUTHENTICATION_REQUIRED` | بيانات المصادقة مفقودة. |
| `401` | `INVALID_ACCESS_TOKEN` | Access Token غير صحيح. |
| `401` | `ACCESS_TOKEN_EXPIRED` | انتهت صلاحية Access Token. |
| `403` | `ACCESS_DENIED` | لا توجد الصلاحية المطلوبة. |
| `403` | `REQUEST_ACCESS_DENIED` | طلب تابع لعميل آخر. |

# 3. التحقق من المدخلات

| HTTP | الرمز | المعنى |
|---:|---|---|
| `400` | `VALIDATION_FAILED` | فشل التحقق من حقل أو أكثر. |
| `400` | `MALFORMED_JSON` | JSON غير صالح. |
| `400` | `INVALID_REQUEST_TYPE` | نوع طلب غير مدعوم. |
| `400` | `INVALID_PRIORITY` | أولوية غير مدعومة. |
| `400` | `INVALID_DATE_RANGE` | مجال زمني غير صحيح. |
| `400` | `INVALID_PAGE_PARAMETER` | Pagination غير صحيح. |
| `400` | `INVALID_SORT_PARAMETER` | Sorting غير صحيح. |
| `400` | `MISSING_IDEMPOTENCY_KEY` | المفتاح مفقود. |
| `400` | `INVALID_IDEMPOTENCY_KEY` | المفتاح غير صالح. |
| `413` | `PAYLOAD_TOO_LARGE` | Payload أكبر من الحد. |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | نوع المحتوى غير مدعوم. |

# 4. الموارد

| HTTP | الرمز | المعنى |
|---:|---|---|
| `404` | `REQUEST_NOT_FOUND` | الطلب غير موجود أو غير ظاهر. |
| `404` | `CONFIGURATION_NOT_FOUND` | الإعداد غير موجود. |
| `404` | `WORKER_NOT_FOUND` | Worker غير موجود. |
| `404` | `QUEUE_NOT_FOUND` | Queue غير موجودة. |

# 5. التعارض

| HTTP | الرمز | المعنى |
|---:|---|---|
| `409` | `IDEMPOTENCY_CONFLICT` | المفتاح مستخدم مع بيانات مختلفة. |
| `409` | `INVALID_STATUS_TRANSITION` | انتقال الحالة غير مسموح. |
| `409` | `REQUEST_ALREADY_TERMINAL` | الطلب في حالة نهائية. |
| `409` | `OPTIMISTIC_LOCK_CONFLICT` | تعارض تحديث متزامن. |
| `409` | `CONFIGURATION_UPDATE_CONFLICT` | تعارض تحديث إعداد. |

# 6. Rate Limiting

| HTTP | الرمز | المعنى |
|---:|---|---|
| `429` | `RATE_LIMIT_EXCEEDED` | تجاوز معدل الطلبات. |
| `429` | `CONCURRENT_REQUEST_LIMIT_EXCEEDED` | تجاوز العمليات المتزامنة. |

يجب إعادة `Retry-After`.

# 7. أخطاء المعالجة غير المتزامنة

| الرمز | قابل للإعادة | المعنى |
|---|---:|---|
| `PROCESSING_TIMEOUT` | نعم | تجاوز مهلة التنفيذ. |
| `DEPENDENCY_UNAVAILABLE` | نعم | خدمة تابعة غير متاحة مؤقتًا. |
| `TEMPORARY_DATABASE_ERROR` | نعم | خطأ قاعدة بيانات مؤقت. |
| `TEMPORARY_BROKER_ERROR` | نعم | خطأ RabbitMQ مؤقت. |
| `BUSINESS_RULE_VIOLATION` | لا | مخالفة قاعدة عمل نهائية. |
| `UNSUPPORTED_OPERATION` | لا | عملية غير مدعومة. |
| `INVALID_PROCESSING_PAYLOAD` | لا | Payload غير صالح للتنفيذ. |
| `MAX_RETRY_ATTEMPTS_EXCEEDED` | لا | تجاوز حد المحاولات. |

# 8. أخطاء البنية التحتية

| HTTP | الرمز | المعنى |
|---:|---|---|
| `500` | `INTERNAL_ERROR` | خطأ داخلي غير متوقع. |
| `500` | `PERSISTENCE_ERROR` | تعذر تخزين البيانات. |
| `500` | `MESSAGE_PUBLICATION_ERROR` | تعذر نشر الرسالة. |
| `502` | `UPSTREAM_BAD_RESPONSE` | استجابة غير صالحة من خدمة خارجية. |
| `503` | `SERVICE_UNAVAILABLE` | الخدمة غير متاحة مؤقتًا. |
| `503` | `DATABASE_UNAVAILABLE` | PostgreSQL غير متاح. |
| `503` | `RABBITMQ_UNAVAILABLE` | RabbitMQ غير متاح. |
| `503` | `REDIS_UNAVAILABLE` | Redis غير متاح. |
| `504` | `UPSTREAM_TIMEOUT` | انتهت مهلة خدمة خارجية. |

# 9. الإدارة والإعدادات

| HTTP | الرمز | المعنى |
|---:|---|---|
| `400` | `INVALID_CONFIGURATION_VALUE` | قيمة غير صالحة. |
| `400` | `CONFIGURATION_TYPE_MISMATCH` | نوع قيمة غير مطابق. |
| `403` | `CONFIGURATION_UPDATE_DENIED` | لا توجد صلاحية تعديل. |
| `409` | `CONFIGURATION_UPDATE_CONFLICT` | تعارض تحديث. |

# 10. إرشادات Android

1. الاعتماد على `code` وليس `message` لاتخاذ القرار.
2. عرض رسالة عربية مناسبة.
3. إظهار أو تسجيل `correlationId` للدعم.
4. عدم إعادة `POST` دون نفس `Idempotency-Key`.
5. احترام `Retry-After`.
6. التعامل مع الرموز غير المعروفة برسالة عامة.
7. عدم اعتبار `FAILED` نهاية دائمة قبل قراءة الحالة الحالية.
