# وثائق HLRMS

هذه الصفحة هي نقطة الدخول إلى الوثائق المطابقة للتنفيذ الحالي. يبقى الكود وملفات الإعداد وقواعد البيانات المصدر الحاسم عند وجود تعارض.

ملخص ما تم تغييره وما بقي قبل الكتاب موجود في [DELIVERY-NOTES.md](DELIVERY-NOTES.md).

## الوثائق المرجعية

| المجال | الوثيقة | الحالة |
|---|---|---|
| المعمارية | [SYSTEM-ARCHITECTURE.md](architecture/SYSTEM-ARCHITECTURE.md) | مطابق للتنفيذ الحالي |
| التقنيات | [TECHNOLOGY-STACK.md](architecture/TECHNOLOGY-STACK.md) | مطابق للتنفيذ الحالي |
| المتطلبات | [FUNCTIONAL-REQUIREMENTS.md](requirements/FUNCTIONAL-REQUIREMENTS.md) | متطلبات منفذة وقابلة للتتبع |
| دورة حياة الطلب | [REQUEST-LIFECYCLE.md](requirements/REQUEST-LIFECYCLE.md) | أربع حالات منفذة |
| API | [API-SPECIFICATION.md](api/API-SPECIFICATION.md) | الواجهات الموجودة فعليًا |
| الأمن | [SECURITY-DESIGN.md](security/SECURITY-DESIGN.md) | حدود الثقة والتحكم في الوصول |
| المراقبة | [OBSERVABILITY.md](operations/OBSERVABILITY.md) | Prometheus وGrafana والقيود الحالية |
| الاختبارات | [testing/README.md](testing/README.md) | فهرس طبقات الاختبار |
| الأداء | [BENCHMARK-RESULTS.md](performance/BENCHMARK-RESULTS.md) | نتائج مسجلة مع حدود تفسيرها |
| إثبات النتائج | [EVIDENCE-PROTOCOL.md](performance/EVIDENCE-PROTOCOL.md) | ربط k6 وGrafana وقاعدة البيانات |
| تكامل العملاء | [CLIENT-INTEGRATION.md](integration/CLIENT-INTEGRATION.md) | تطبيق أو موقع قابل للاستبدال |
| المصطلحات | [GLOSSARY.md](GLOSSARY.md) | عربي / English |
| المخططات | [../diagrams/README.md](../diagrams/README.md) | 31 مخططًا بثلاث صيغ |

## تصنيف أدلة الأداء

- **Raw Evidence Available:** توجد مخرجات أصلية داخل المستودع وقابلة لإعادة الفحص.
- **Recorded Run:** نتيجة مسجلة أثناء التنفيذ، لكن ملفات `JSON/CSV` أو ملخص k6 الخام غير مرفوعة بعد.
- **Planned Validation:** اختبار لم يُنفذ بعد، ولا يجوز عرضه بوصفه نتيجة.

توجد أدلة خام لاختبارات `P19-R10` داخل `docs/performance/results/P19-R10/`. بقية أرقام الأداء تبقى `Recorded Run` إلى أن تضاف مخرجاتها الخام ولقطات Grafana أثناء التشغيل.

## المواد التاريخية

تمثل ملفات `docs/meetings/` سجلًا مرحليًا لتنظيم العمل، وليست مرجعًا للمعمارية النهائية أو النتائج. التقرير المرحلي القديم `HLRMS_02` ليس ضمن وثائق التسليم النهائية.

## قاعدة التحديث

عند تغيير عدد النسخ، بنية Docker، أو قيمة RPS مثبتة، يجب تحديث العناصر الآتية معًا:

1. ملف الإعداد أو سكربت k6 المعني.
2. `docs/performance/BENCHMARK-RESULTS.md`.
3. المخطط المعماري المتأثر.
4. المخرجات الخام واللقطات المرتبطة بـ`TEST_RUN_ID` نفسه.
