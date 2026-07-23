# مصفوفة الأدوار والصلاحيات
## Role-Based Access Control Matrix

# 1. الأدوار

| الدور | الوصف |
|---|---|
| `CLIENT` | يرسل الطلبات ويتابع طلباته فقط. |
| `ADMIN` | يعرض جميع البيانات والإعدادات التشغيلية. |
| `MONITORING` | يقرأ Health وMetrics فقط. |
| `SERVICE` | هوية داخلية بين الخدمات عند اعتماد Service-to-Service Authentication. |

# 2. صلاحيات API

| Endpoint | CLIENT | ADMIN | MONITORING | SERVICE |
|---|:---:|:---:|:---:|:---:|
| `POST /api/v1/requests` | نعم | اختياري | لا | نعم |
| `GET /api/v1/requests` | ضمن نطاقه | نعم | لا | نعم |
| `GET /api/v1/requests/{id}` | ضمن نطاقه | نعم | لا | نعم |
| `GET /api/v1/requests/{id}/status` | ضمن نطاقه | نعم | لا | نعم |
| `GET /api/v1/requests/{id}/history` | ضمن نطاقه | نعم | لا | نعم |
| `GET /api/v1/admin/requests` | لا | نعم | لا | اختياري |
| `GET /api/v1/admin/queues` | لا | نعم | قراءة اختيارية | اختياري |
| `GET /api/v1/admin/workers` | لا | نعم | قراءة اختيارية | اختياري |
| `GET /api/v1/admin/configurations` | لا | نعم | لا | اختياري |
| `PATCH /api/v1/admin/configurations/{key}` | لا | نعم | لا | لا افتراضيًا |
| `GET /actuator/health` | لا عام | داخليًا | نعم | نعم |
| `GET /actuator/prometheus` | لا | لا افتراضيًا | نعم | نعم |

# 3. قواعد التحقق

كل طلب يمر بمرحلتين:

1. التحقق من الدور.
2. التحقق من ملكية المورد أو نطاق الوصول.

مثال:

```text
role == CLIENT
AND request.clientSystemId == token.clientSystemId
```

# 4. العمليات الحساسة

تحتاج العمليات التالية إلى دور `ADMIN` وAudit Log وCorrelation ID وسبب واضح:

- تعديل Retry Limit.
- تعديل Queue أو Worker Configuration.
- إعادة معالجة DLQ.
- تعطيل Client System.
- تغيير Rate Limit.
- تدوير Credentials.
