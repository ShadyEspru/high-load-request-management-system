# HLRMS System Context Diagram

يمثل هذا المخطط **HLRMS Core Platform** باعتباره `System of Interest`، ويوضح علاقاته مع الأطراف والأنظمة الخارجية من دون النزول إلى مستوى الـContainers أو تفاصيل الـDeployment.

## العناصر الخارجية

- **Mobile User**: المستخدم النهائي لتطبيق Android التجريبي.
- **Financial Transfer Demo**: نطاق تجريبي منفذ باستخدام Android + Transfer API ويستخدم HLRMS لتنفيذ وتتبع `MONEY_TRANSFER`.
- **Generic Client System**: أي Web أو External API consumer يستخدم واجهات HLRMS العامة.
- **Performance Engineer**: ينفذ اختبارات الحمل والمرونة باستخدام k6.
- **Operator / Administrator**: يشغّل ويفحص ويدير منصة HLRMS.
- **Observability Platform**: Prometheus + Grafana لجمع وعرض metrics وhealth information.

## العلاقات

- الخط المتصل: `Direct Interaction / Request Flow`.
- الخط المتقطع: `Monitoring / Information Flow`.

## حدود التجريد

لا تظهر في هذا المخطط المكونات الداخلية مثل RabbitMQ وRedis وPostgreSQL وWorkers وOutbox Publisher وAPI Gateway وHAProxy، ولا تظهر ports أو containers أو networks. يتم توثيق هذه التفاصيل في مخططات C4 Container وDeployment والمخططات التقنية المتخصصة.

**Financial Transfer Demo هو example domain منفذ فوق HLRMS، وليس تعريف نطاق HLRMS الأساسي.**

## الملفات

- `system-context.png`: الصورة المعتمدة.
- `system-context.pdf`: نسخة PDF مطابقة للصورة.
- `system-context.drawio`: نسخة Draw.io قابلة للتحرير.
