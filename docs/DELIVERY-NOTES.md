# سجل تسليم المخططات والوثائق

## النطاق

- الفرع المستهدف: `docs/final-delivery`.
- لم يجر تعديل Branch `main` أو `develop`.
- لم يجر تغيير منطق Backend أو Android أو Docker؛ التغييرات وثائق ومخططات وأدوات توليد/تحقق فقط.

## المخططات

- 31 مخططًا نهائيًا.
- 31 ملف drawio قابلًا للتحرير.
- 31 ملف PDF من صفحة واحدة.
- 31 صورة PNG بقياس 1920×1080.
- إضافة C4 Level 3 وTwo-Host Performance وSecurity وIntegration وFault Tolerance وPerformance Evidence وOutbox Idempotency وRepository Workflow وAPI Routing.
- حذف ERD الخاص بالتطبيق التجريبي من حزمة HLRMS الأساسية.
- حذف SVG وBackup files وPDF التجميعي القديم من حزمة التسليم.

## الوثائق

- إعادة تعريف المشروع كمنصة عامة لا كتطبيق مجال محدد.
- توثيق المعمارية والمكونات والمسارات والمنافذ والقيود الحالية.
- تصحيح دورة حياة الطلب إلى أربع حالات منفذة.
- تصحيح عقد API وإزالة Endpoints غير موجودة.
- إعادة كتابة استراتيجية الاختبار ونتائج الأداء وبروتوكول الإثبات.
- توثيق تكامل Client قابل للاستبدال ودور Demo Client.
- تصحيح RabbitMQ وOutbox وQuorum Queue ADRs وفق التنفيذ.
- توثيق Grafana/Prometheus والحدود الحالية للـScraping.
- توثيق نقاط الأمن المتعلقة بمنافذ التشخيص ومسارات Performance.

## التحقق المنفذ

```text
Documentation: 42 Markdown files checked; links resolved; OpenAPI 3.1 parsed.
Diagrams: 31 × 3 formats; PNG 1920×1080; PDF 1440×810 pt; drawio XML parsed.
Git diff: no whitespace errors.
```

لم تشغل اختبارات Backend ضمن هذه المهمة لأن كود الخدمات لم يتغير ولأن Integration Tests تحتاج PostgreSQL وRedis وRabbitMQ محلية. يجب إرفاق نتيجة تشغيل Maven النهائية عند دمج الفرع.

## عناصر معلقة قبل كتاب المشروع

1. إضافة مخرجات k6 الخام للتشغيلات الأساسية، خصوصًا 350 RPS.
2. اختيار صور Grafana الملتقطة أثناء الحمل وربطها بـTEST_RUN_ID.
3. إضافة مواصفات الحاسوب الحالي والحاسوب الثاني.
4. تنفيذ Two-Host Validation وتحديد هل وصل 1000 RPS دون Dropped غير مفسرة.
5. تحديث `BENCHMARK-RESULTS.md` والمخطط المتأثر إذا تغيرت Replicas أوالنتائج.
6. إدراج المخططات المختارة في كتاب المشروع وفق قالب الجامعة.
