# ADR-003: استخدام Redis للتخزين المؤقت والحالة المؤقتة

- **Status:** Accepted
- **Date:** 2026-07-23
- **Decision Owners:** HLRMS Team
- **Related Requirements:** Low Latency, Rate Limiting, Reduced Database Load
- **Related ADRs:** ADR-002

## 1. السياق

توجد عمليات عالية التردد مثل قراءة حالة الطلب دوريًا، Rate Limiting، Configuration Cache، Counters، وتسريع التحقق من Idempotency. لا ينبغي أن تضيف ضغطًا غير ضروري إلى PostgreSQL.

## 2. محركات القرار

- زمن وصول منخفض.
- TTL.
- Atomic Counters.
- Cache مشترك بين عدة Instances.
- تكامل Spring Data Redis.

## 3. البدائل المدروسة

### Redis

سريع، موزع، يدعم TTL وCounters، لكنه يحتاج سياسة Eviction ولا يجب اعتباره مصدر الحقيقة.

### Caffeine Local Cache

بسيط وسريع، لكنه غير مشترك بين Instances ولا يناسب Rate Limiting الموزع.

### PostgreSQL فقط

يبسط التشغيل، لكنه يزيد الضغط ولا يناسب كل حالات TTL والعدادات.

## 4. القرار

سيستخدم Redis للتخزين المؤقت والحالة قصيرة العمر فقط. يبقى PostgreSQL المصدر النهائي لجميع بيانات الأعمال.

## 5. حالات الاستخدام المعتمدة

```text
rate-limit:{clientSystemId}:{endpoint}:{window}
request-status:{requestId}
configuration:{key}
```

يمكن استخدامه لتسريع Idempotency، لكن الضمان الدائم يبقى في PostgreSQL.

## 6. النتائج الإيجابية

- تقليل زمن قراءة الحالة.
- تقليل ضغط PostgreSQL.
- Rate Limiting موزع.
- دعم TTL وCounters.

## 7. النتائج السلبية والمخاطر

- Cache Staleness.
- Cache Stampede.
- تعقيد Invalidation.
- استهلاك ذاكرة واعتماد تشغيلي إضافي.

## 8. إجراءات التخفيف

- Cache-Aside Pattern.
- TTL لكل نوع بيانات.
- Fallback إلى PostgreSQL.
- Max Memory وEviction Policy.
- Prefix واضح للمفاتيح.
- مراقبة Hit Rate وEvictions.
- عدم تخزين بيانات لا يمكن إعادة بنائها.

## 9. سياسة الفشل

| الوظيفة | السلوك عند توقف Redis |
|---|---|
| قراءة حالة الطلب | الرجوع إلى PostgreSQL |
| Configuration Cache | القراءة من PostgreSQL |
| Idempotency | الاعتماد على PostgreSQL |
| Rate Limiting | حسب سياسة Endpoint: Fail-open أو Fail-closed |

## 10. معايير القبول

- [ ] لا توجد بيانات أعمال موجودة فقط في Redis.
- [ ] لكل مفتاح مؤقت TTL.
- [ ] Cache Miss يعيد القراءة من PostgreSQL.
- [ ] توقف Redis لا يفقد طلبًا أو نتيجة.
- [ ] Rate Limiting يعمل عبر عدة Instances.

## 11. شروط إعادة المراجعة

عند عدم وجود فائدة قابلة للقياس، أو تحول Redis إلى نقطة فشل للمسار الأساسي، أو تغير متطلبات الاتساق.
