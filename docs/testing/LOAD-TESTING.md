# LOAD-TESTING.md

# Load Testing Guidelines

**Project:** High-Load Request Management System (HLRMS)

**Version:** 1.0

**Status:** Accepted

---

# 1. Purpose

## 1.1 Overview

يحدد هذا المستند المعايير والإجراءات الرسمية لتنفيذ اختبارات الأداء (Performance Testing) واختبارات الأحمال (Load Testing) في مشروع **High-Load Request Management System (HLRMS)**.

نظرًا لأن الهدف الأساسي للمشروع هو بناء نظام قادر على معالجة عدد كبير من الطلبات المتزامنة بكفاءة وموثوقية، فإن اختبارات الأداء تعد جزءًا أساسيًا من عملية التحقق من جودة النظام.

تهدف هذه الوثيقة إلى توضيح كيفية قياس أداء النظام، وتحديد الحدود التشغيلية (Operational Limits)، واكتشاف نقاط الاختناق (Performance Bottlenecks)، وضمان أن النظام يلبي المتطلبات غير الوظيفية المتعلقة بالأداء وقابلية التوسع.

---

## 1.2 Objectives

تهدف اختبارات الأحمال إلى تحقيق الأهداف التالية:

- قياس قدرة النظام على معالجة الطلبات المتزامنة.
- قياس زمن الاستجابة (Response Time).
- قياس معدل المعالجة (Throughput).
- تقييم استقرار النظام تحت الأحمال المختلفة.
- اكتشاف نقاط الاختناق في البنية التحتية.
- تقييم أداء RabbitMQ.
- تقييم أداء PostgreSQL.
- تقييم أداء Redis.
- قياس أداء Workers.
- تقييم فعالية Queue-Based Architecture.
- التحقق من قدرة النظام على التعافي بعد الأحمال العالية.
- توفير بيانات كمية يمكن استخدامها لتحسين الأداء.

---

## 1.3 Importance of Load Testing

قد يعمل النظام بصورة صحيحة أثناء الاختبارات الوظيفية، ولكنه يفشل عند زيادة عدد المستخدمين أو الطلبات.

ومن الأمثلة على ذلك:

- امتلاء Queue بالرسائل.
- ارتفاع زمن الاستجابة.
- زيادة زمن انتظار قاعدة البيانات.
- استهلاك كامل لموارد المعالج.
- استهلاك الذاكرة.
- تأخر معالجة الرسائل بواسطة Workers.
- زيادة معدلات الأخطاء.

تهدف اختبارات الأحمال إلى اكتشاف هذه المشكلات قبل نشر النظام في بيئة الإنتاج.

---

# 2. Scope

## 2.1 Included Components

تشمل اختبارات الأداء جميع المكونات المؤثرة على سرعة واستقرار النظام، ومنها:

- REST APIs
- API Gateway
- Spring Boot Services
- PostgreSQL
- RabbitMQ
- Redis
- Transactional Outbox
- Worker Services
- Monitoring Stack
- Docker Environment

---

## 2.2 Excluded Components

لا تشمل هذه الوثيقة:

- Unit Testing
- Integration Testing
- UI Testing
- Android Application
- Security Testing
- Penetration Testing
- Functional Testing

وسيتم توثيق هذه الأنواع في مستندات مستقلة.

---

## 2.3 Performance Scope

تركز اختبارات الأداء على قياس:

- زمن الاستجابة.
- معدل المعالجة.
- عدد الطلبات الناجحة.
- معدل الأخطاء.
- استهلاك الموارد.
- أداء قواعد البيانات.
- أداء Queue.
- أداء Cache.

---

# 3. Performance Objectives

## 3.1 Overview

تحدد هذه الأهداف الحدود الدنيا المقبولة لأداء النظام أثناء التشغيل تحت الأحمال المختلفة.

تستخدم هذه القيم كأساس لتقييم نجاح أو فشل اختبارات الأداء.

---

## 3.2 Primary Objectives

يسعى النظام إلى تحقيق ما يلي:

- المحافظة على زمن استجابة منخفض.
- معالجة آلاف الطلبات دون فقدان البيانات.
- المحافظة على استقرار الخدمات.
- عدم فقدان الرسائل.
- عدم انهيار Workers.
- المحافظة على سلامة قاعدة البيانات.

---

## 3.3 Scalability Objectives

يجب أن يكون النظام قادرًا على:

- زيادة عدد Workers.
- زيادة عدد Instances.
- توزيع الحمل بصورة متوازنة.
- المحافظة على الأداء بعد التوسع الأفقي.

---

## 3.4 Reliability Objectives

حتى عند زيادة الضغط يجب أن:

- تستمر الخدمات بالعمل.
- لا يتم فقدان الرسائل.
- تستمر عمليات Retry.
- تستمر عملية Transactional Outbox.
- لا يحدث Data Corruption.

---

# 4. Performance Metrics

## 4.1 Overview

تعتمد جميع اختبارات الأداء على مجموعة من المؤشرات القياسية (Performance Metrics).

تستخدم هذه المؤشرات لتقييم أداء النظام بصورة موضوعية.

---

## 4.2 Response Time

يمثل الزمن بين إرسال الطلب واستلام الاستجابة.

يعد أهم مؤشر يلاحظه المستخدم النهائي.

---

## 4.3 Throughput

يمثل عدد العمليات التي يستطيع النظام معالجتها خلال فترة زمنية محددة.

ويقاس عادة بـ:

- Requests Per Second (RPS)
- Transactions Per Second (TPS)

---

## 4.4 Latency

يقيس الزمن الذي تستغرقه العملية داخل النظام.

يشمل:

- Network Delay
- Queue Waiting Time
- Processing Time
- Database Time

---

## 4.5 Error Rate

يمثل نسبة الطلبات التي انتهت بخطأ.

يحسب باستخدام:

```text
(Number of Failed Requests / Total Requests) × 100%
```

---

## 4.6 Success Rate

يمثل نسبة الطلبات الناجحة.

ويعد المؤشر الأساسي لاستقرار النظام أثناء الضغط.

---

## 4.7 Percentiles

يتم تحليل:

- P50
- P90
- P95
- P99

لفهم توزيع أزمنة الاستجابة وليس المتوسط فقط.

---

# 5. Test Environment

## 5.1 Overview

تنفذ جميع اختبارات الأداء داخل بيئة تشغيل مستقلة تحاكي بيئة الإنتاج قدر الإمكان.

---

## 5.2 Environment Components

تشمل بيئة الاختبار:

- Java 21
- Spring Boot 3.x
- PostgreSQL
- RabbitMQ
- Redis
- Docker
- Docker Compose
- Prometheus
- Grafana
- k6

---

## 5.3 Environment Isolation

يجب ألا تؤثر اختبارات الأداء على:

- بيئة التطوير.
- بيئة الإنتاج.
- قواعد البيانات الحقيقية.

وتنفذ جميع الاختبارات على بيئة معزولة بالكامل.

---

## 5.4 Configuration

يجب توثيق:

- عدد Workers.
- عدد CPU Cores.
- حجم الذاكرة.
- إصدار Docker.
- إصدار Java.
- إصدار قاعدة البيانات.

حتى تكون نتائج الاختبارات قابلة لإعادة الإنتاج.

---

# 6. Infrastructure

## 6.1 Overview

تتكون بيئة الاختبار من نفس المكونات المستخدمة في النظام الفعلي لضمان دقة النتائج.

---

## 6.2 Infrastructure Components

تشمل:

- API Gateway
- Spring Boot Services
- PostgreSQL
- RabbitMQ
- Redis
- Worker Services
- Prometheus
- Grafana

---

## 6.3 Monitoring Infrastructure

تراقب جميع المكونات أثناء الاختبار باستخدام:

- Micrometer
- Prometheus
- Grafana
- Spring Boot Actuator

---

## 6.4 Network Configuration

يجب أن تكون جميع الخدمات متصلة بنفس شبكة Docker لضمان استقرار الاتصال وتقليل تأثير العوامل الخارجية على نتائج الاختبار.

---

# 7. Workload Model

## 7.1 Purpose

يحدد نموذج الحمل كيفية وصول الطلبات إلى النظام أثناء تنفيذ الاختبارات.

ويجب أن يحاكي الاستخدام الحقيقي قدر الإمكان.

---

## 7.2 User Behavior

يفترض النموذج أن المستخدمين يقومون بالعمليات التالية:

- تسجيل الدخول.
- إنشاء طلب.
- الاستعلام عن الحالة.
- متابعة حالة الطلب.
- تكرار العمليات بصورة غير متزامنة.

---

## 7.3 Concurrent Users

تعتمد اختبارات الأداء على عدد متزايد من المستخدمين المتزامنين (Virtual Users) لمحاكاة سيناريوهات الاستخدام الواقعية.

---

## 7.4 Request Distribution

يجب توزيع الطلبات على جميع نقاط النهاية (Endpoints) وفقًا لنمط الاستخدام المتوقع، وعدم التركيز على عملية واحدة فقط.

---

# 8. Load Profiles

## 8.1 Purpose

تحدد Load Profiles كيفية تغير الحمل أثناء تنفيذ الاختبارات.

---

## 8.2 Baseline Load

يمثل الحمل الطبيعي المتوقع للنظام.

ويستخدم كخط أساس للمقارنة.

---

## 8.3 Peak Load

يمثل أعلى حمل يتوقع أن يواجهه النظام أثناء التشغيل الطبيعي.

---

## 8.4 Burst Load

يمثل الزيادة المفاجئة في عدد الطلبات خلال فترة زمنية قصيرة.

---

## 8.5 Sustained Load

يمثل حملًا ثابتًا لفترة زمنية طويلة بهدف قياس الاستقرار.

---

# 9. Performance Requirements

## 9.1 Purpose

تحدد هذه المتطلبات الحدود الدنيا المقبولة لأداء النظام.

وتستخدم كمعايير رسمية لقبول نتائج اختبارات الأداء.

---

## 9.2 Response Time Requirements

يجب أن تحقق واجهات النظام زمن استجابة مناسبًا تحت الحمل المتوقع، مع الحفاظ على استقرار الأداء وعدم وجود ارتفاعات حادة في زمن الاستجابة.

---

## 9.3 Availability Requirements

يجب أن تبقى جميع الخدمات الأساسية متاحة أثناء تنفيذ اختبارات الحمل، وألا يؤدي الضغط إلى توقف النظام أو فقدان البيانات.

---

## 9.4 Resource Utilization

يجب مراقبة استهلاك:

- CPU
- Memory
- Database Connections
- Queue Size
- Worker Threads

ويجب أن يبقى الاستهلاك ضمن الحدود المقبولة طوال مدة الاختبار.

---

## 9.5 Acceptance Criteria

تعتبر اختبارات الأداء ناجحة إذا حقق النظام المتطلبات المحددة لهذه الوثيقة، ولم تظهر اختناقات أو أخطاء تؤثر على استقرار النظام أو سلامة البيانات.

---

# 10. k6 Testing Architecture

## 10.1 Purpose

يعتمد مشروع HLRMS على أداة **k6** كمنصة رئيسية لتنفيذ اختبارات الأداء والأحمال، وذلك لقدرتها على محاكاة آلاف المستخدمين المتزامنين، وإنتاج تقارير دقيقة عن أداء النظام.

توفر k6 بيئة برمجية تعتمد على JavaScript، مما يسمح ببناء سيناريوهات اختبار قابلة لإعادة الاستخدام، وسهلة التكامل مع أنظمة CI/CD.

---

## 10.2 Why k6

تم اختيار k6 للأسباب التالية:

- مفتوحة المصدر.
- سهلة التكامل مع GitHub Actions.
- تدعم سيناريوهات متعددة.
- تدعم Thresholds.
- تدعم Metrics مخصصة.
- منخفضة استهلاك الموارد.
- تدعم Prometheus وGrafana.
- مناسبة لاختبار REST APIs.

---

## 10.3 Test Architecture

تعتمد بنية اختبارات الأداء على المكونات التالية:

```text
k6 Scripts

↓

HTTP Requests

↓

API Gateway

↓

Spring Boot Services

↓

RabbitMQ

↓

Workers

↓

PostgreSQL / Redis

↓

Prometheus Metrics

↓

Grafana Dashboards
```

---

## 10.4 Script Organization

يجب تقسيم اختبارات k6 إلى ملفات مستقلة حسب نوع الاختبار، وعدم دمج جميع السيناريوهات داخل ملف واحد.

يسهل ذلك الصيانة وإعادة الاستخدام وتشغيل كل سيناريو بصورة منفصلة.

---

# 11. Test Scripts

## 11.1 Purpose

يمثل كل Script سيناريو أداء مستقل يختبر جانبًا محددًا من النظام.

---

## 11.2 Recommended Structure

ينظم المشروع ملفات k6 بالشكل التالي:

```text
tests/performance/k6/

├── smoke.js
├── load.js
├── stress.js
├── spike.js
├── soak.js
├── recovery.js
├── thresholds.js
└── README.md
```

---

## 11.3 Shared Components

يفضل وضع العناصر المشتركة في ملفات مستقلة مثل:

- Base URL
- Authentication
- Request Builders
- Utility Functions
- Threshold Definitions

لتجنب تكرار الكود.

---

## 11.4 Naming Convention

تستخدم أسماء واضحة تعبر عن الغرض من كل سيناريو.

أمثلة:

- load.js
- stress.js
- spike.js
- soak.js

---

# 12. Virtual Users (VUs)

## 12.1 Overview

تعتمد k6 على مفهوم **Virtual Users (VUs)** لمحاكاة المستخدمين الحقيقيين.

يمثل كل Virtual User مستخدمًا مستقلاً ينفذ السيناريو المحدد بصورة متكررة.

---

## 12.2 User Simulation

يقوم كل مستخدم افتراضي بتنفيذ:

- إرسال الطلب.
- انتظار الاستجابة.
- تنفيذ التحقق.
- إعادة الطلب.

بصورة مستقلة عن بقية المستخدمين.

---

## 12.3 Scaling Strategy

يتم زيادة عدد المستخدمين تدريجيًا لقياس تأثير الحمل على النظام.

على سبيل المثال:

- 10 مستخدمين.
- 100 مستخدم.
- 500 مستخدم.
- 1000 مستخدم.
- 5000 مستخدم.

وفق قدرة بيئة الاختبار.

---

## 12.4 Think Time

عند الحاجة يمكن إضافة فترات انتظار بين الطلبات لمحاكاة سلوك المستخدم الحقيقي، بدلاً من إرسال الطلبات بأقصى سرعة ممكنة.

---

# 13. Ramp-up Strategy

## 13.1 Purpose

تهدف مرحلة Ramp-up إلى زيادة الحمل تدريجيًا بدلاً من بدء الاختبار بأقصى عدد من المستخدمين.

يساعد ذلك في مراقبة سلوك النظام أثناء نمو الحمل.

---

## 13.2 Incremental Load

يزداد عدد المستخدمين على مراحل متتالية حتى الوصول إلى الحمل المستهدف.

---

## 13.3 Monitoring During Ramp-up

أثناء زيادة الحمل تتم مراقبة:

- Response Time.
- CPU Usage.
- Memory Usage.
- Queue Length.
- Database Connections.

---

## 13.4 Expected Behaviour

يجب أن يبقى النظام مستقرًا أثناء زيادة الحمل دون حدوث ارتفاعات مفاجئة في زمن الاستجابة أو معدلات الأخطاء.

---

# 14. Constant Load Testing

## 14.1 Purpose

يقيس هذا الاختبار قدرة النظام على العمل تحت حمل ثابت لفترة زمنية محددة.

---

## 14.2 Test Characteristics

يتضمن:

- عدد ثابت من المستخدمين.
- معدل طلبات ثابت.
- مدة تشغيل ثابتة.

---

## 14.3 Objectives

التحقق من:

- استقرار الأداء.
- استقرار استهلاك الموارد.
- عدم وجود تسرب للذاكرة.
- استقرار Workers.

---

## 14.4 Success Criteria

يعتبر الاختبار ناجحًا إذا حافظ النظام على أداء مستقر طوال مدة التنفيذ دون تدهور ملحوظ.

---

# 15. Spike Testing

## 15.1 Purpose

يختبر قدرة النظام على التعامل مع الزيادات المفاجئة في عدد الطلبات.

---

## 15.2 Spike Scenario

يزداد عدد المستخدمين بصورة كبيرة خلال فترة قصيرة جدًا، ثم يعود إلى المستوى الطبيعي.

---

## 15.3 Verification

يجب التأكد من:

- استمرار عمل النظام.
- عدم فقدان الرسائل.
- عدم انهيار الخدمات.
- تعافي النظام بعد انتهاء الزيادة.

---

## 15.4 Recovery Time

يجب قياس الزمن اللازم لعودة النظام إلى الأداء الطبيعي بعد انتهاء الحمل المفاجئ.

---

# 16. Stress Testing

## 16.1 Purpose

يهدف Stress Testing إلى معرفة الحد الأقصى الذي يستطيع النظام تحمله قبل حدوث تدهور واضح في الأداء أو توقف إحدى الخدمات.

---

## 16.2 Stress Strategy

يتم زيادة الحمل تدريجيًا إلى ما بعد الحدود التشغيلية المتوقعة، مع الاستمرار في مراقبة جميع المكونات.

---

## 16.3 Monitoring

يجب مراقبة:

- Error Rate.
- Response Time.
- Queue Size.
- Worker Utilization.
- CPU.
- Memory.

---

## 16.4 Breaking Point

يتم تحديد نقطة الانهيار (Breaking Point) عندما:

- ترتفع نسبة الأخطاء بصورة كبيرة.
- يتجاوز زمن الاستجابة الحدود المقبولة.
- تتوقف إحدى الخدمات عن الاستجابة.
- تمتلئ Queue بصورة مستمرة.

ويجب توثيق هذه النقطة للاستفادة منها في تحسين النظام.

---

# 17. Soak Testing

## 17.1 Purpose

يقيس Soak Testing قدرة النظام على العمل لفترات طويلة تحت حمل ثابت.

ويهدف إلى اكتشاف المشكلات التي لا تظهر في الاختبارات القصيرة.

---

## 17.2 Test Duration

يستمر الاختبار عادةً لعدة ساعات وفق الخطة المعتمدة، مع الحفاظ على حمل ثابت طوال فترة التشغيل.

---

## 17.3 Target Issues

يساعد هذا الاختبار في اكتشاف:

- Memory Leaks.
- Connection Leaks.
- Queue Growth.
- Database Saturation.
- Resource Exhaustion.
- Thread Starvation.

---

## 17.4 Success Criteria

يعتبر الاختبار ناجحًا إذا حافظ النظام على:

- زمن استجابة مستقر.
- معدل أخطاء منخفض.
- استهلاك موارد ثابت.
- عدم وجود تدهور تدريجي في الأداء.

---

# 18. Throughput Testing

## 18.1 Purpose

يمثل معدل المعالجة (Throughput) عدد العمليات التي يستطيع النظام تنفيذها خلال فترة زمنية محددة.

ويعد أحد أهم مؤشرات الأداء في الأنظمة عالية الأحمال، لأنه يعكس القدرة الفعلية للنظام على معالجة الطلبات مع زيادة الحمل.

---

## 18.2 Measurement

يقاس Throughput باستخدام مؤشرات مثل:

- Requests Per Second (RPS)
- Transactions Per Second (TPS)
- Messages Per Second (MPS)

ويجب تسجيل هذه القيم طوال مدة الاختبار وليس في بدايته أو نهايته فقط.

---

## 18.3 Analysis

يجب تحليل:

- متوسط معدل المعالجة.
- أعلى قيمة تم الوصول إليها.
- أقل قيمة أثناء الضغط.
- تغير المعدل مع زيادة عدد المستخدمين.

---

## 18.4 Acceptance Criteria

يعتبر Throughput مقبولًا إذا بقي مستقرًا مع زيادة الحمل حتى الوصول إلى الحدود التصميمية للنظام، دون انخفاض حاد أو غير متوقع.

---

# 19. Latency Analysis

## 19.1 Purpose

يقيس تحليل Latency الزمن الذي تستغرقه العملية داخل النظام منذ استقبال الطلب وحتى اكتمال معالجته.

---

## 19.2 Response Time Distribution

لا يعتمد تقييم الأداء على المتوسط فقط، بل يجب تحليل التوزيع الكامل لزمن الاستجابة.

تشمل المؤشرات:

- Minimum
- Average
- Maximum
- P50
- P90
- P95
- P99

---

## 19.3 Tail Latency

تمثل قيم P95 وP99 الطلبات الأبطأ داخل النظام.

وتعد هذه القيم مؤشرًا مهمًا لتقييم تجربة المستخدم أثناء الأحمال العالية.

---

## 19.4 Performance Degradation

يجب مراقبة تغير Latency مع زيادة الحمل.

الهدف هو اكتشاف اللحظة التي يبدأ فيها زمن الاستجابة بالارتفاع بصورة غير متناسبة مع عدد المستخدمين.

---

# 20. Error Rate Analysis

## 20.1 Purpose

يقيس Error Rate نسبة الطلبات التي انتهت بفشل أثناء تنفيذ اختبارات الأداء.

---

## 20.2 Failure Categories

تشمل الأخطاء:

- HTTP Errors
- Validation Errors
- Timeout Errors
- Database Errors
- RabbitMQ Errors
- Redis Errors
- Internal Server Errors

---

## 20.3 Error Distribution

يجب تحديد:

- أكثر الأخطاء تكرارًا.
- توقيت ظهورها.
- ارتباطها بمستوى الحمل.
- تأثيرها على بقية النظام.

---

## 20.4 Acceptance Criteria

يجب أن تبقى نسبة الأخطاء ضمن الحدود المقبولة طوال فترة الاختبار، وألا ترتفع بصورة مستمرة مع الأحمال الطبيعية.

---

# 21. Queue Performance

## 21.1 Purpose

نظرًا لاعتماد HLRMS على RabbitMQ، فإن أداء Queue يعد جزءًا أساسيًا من تقييم النظام.

---

## 21.2 Queue Metrics

تشمل المؤشرات:

- Queue Depth
- Queue Growth Rate
- Publish Rate
- Consume Rate
- Acknowledgement Rate

---

## 21.3 Queue Saturation

يجب اختبار قدرة النظام على التعامل مع امتلاء Queue.

ويتم مراقبة:

- زمن الانتظار.
- معدل استهلاك الرسائل.
- قدرة Workers على اللحاق بالحمل.

---

## 21.4 Queue Stability

يعتبر أداء Queue مستقرًا إذا لم يستمر عدد الرسائل المعلقة بالازدياد بعد استقرار الحمل.

---

# 22. Database Performance

## 22.1 Purpose

تهدف هذه الاختبارات إلى تقييم أداء PostgreSQL تحت الأحمال المختلفة.

---

## 22.2 Database Metrics

تشمل:

- Query Response Time
- Active Connections
- Transaction Rate
- Lock Contention
- Index Usage

---

## 22.3 Connection Pool

يجب مراقبة:

- عدد الاتصالات النشطة.
- عدد الاتصالات المتاحة.
- زمن انتظار الحصول على اتصال.

---

## 22.4 Database Bottlenecks

تشمل الاختناقات المحتملة:

- Slow Queries
- Missing Indexes
- Lock Contention
- Full Table Scans
- Connection Exhaustion

---

# 23. Redis Performance

## 23.1 Purpose

يقيس هذا الاختبار أداء Redis باعتباره طبقة التخزين المؤقت (Caching Layer).

---

## 23.2 Cache Metrics

يجب مراقبة:

- Cache Hits
- Cache Misses
- Hit Ratio
- Memory Usage
- Key Expiration

---

## 23.3 Cache Efficiency

يعد Cache فعالًا إذا ساهم في تقليل عدد الاستعلامات المرسلة إلى PostgreSQL وتحسين زمن الاستجابة.

---

## 23.4 Resource Monitoring

تتم مراقبة:

- استهلاك الذاكرة.
- معدل العمليات.
- زمن تنفيذ أوامر Redis.

---

# 24. RabbitMQ Performance

## 24.1 Purpose

يقيس هذا الاختبار أداء RabbitMQ أثناء معالجة الرسائل تحت الأحمال المختلفة.

---

## 24.2 Broker Metrics

تشمل:

- Publish Rate
- Delivery Rate
- Ack Rate
- Queue Size
- Consumer Count

---

## 24.3 Message Flow

يجب التحقق من أن الرسائل تنتقل بصورة مستمرة من:

Producer

↓

Exchange

↓

Queue

↓

Worker

دون تراكم غير مبرر.

---

## 24.4 Broker Stability

يجب ألا يؤدي ارتفاع الحمل إلى:

- توقف Broker.
- فقدان الرسائل.
- انخفاض كبير في معدل النشر أو الاستهلاك.

---

# 25. Worker Performance

## 25.1 Purpose

تمثل Workers العنصر المسؤول عن معالجة الرسائل، لذلك يعد قياس أدائها جزءًا رئيسيًا من تقييم النظام.

---

## 25.2 Processing Metrics

تشمل:

- Messages Processed
- Processing Time
- Retry Count
- Failure Count
- Success Rate

---

## 25.3 Worker Utilization

يجب مراقبة:

- عدد Workers النشطين.
- نسبة استخدام Threads.
- زمن الانتظار.
- معدل استهلاك الرسائل.

---

## 25.4 Processing Stability

يعتبر أداء Workers مستقرًا إذا حافظت على معدل معالجة ثابت ولم تتراكم الرسائل داخل Queue مع استمرار الحمل.

---

# 26. Horizontal Scaling

## 26.1 Purpose

يهدف هذا الاختبار إلى التحقق من قدرة النظام على زيادة الأداء من خلال إضافة Instances أو Workers جديدة.

---

## 26.2 Scaling Strategy

تشمل سيناريوهات الاختبار:

- زيادة عدد Workers.
- زيادة عدد Application Instances.
- توزيع الحمل على عدة عقد.

---

## 26.3 Expected Behaviour

بعد إضافة موارد جديدة يجب أن:

- يزداد Throughput.
- ينخفض زمن الانتظار.
- ينخفض عدد الرسائل المعلقة.
- تتحسن استجابة النظام.

---

## 26.4 Scalability Evaluation

يتم تقييم نجاح التوسع الأفقي بناءً على:

- نسبة تحسن Throughput.
- انخفاض متوسط Latency.
- استقرار Error Rate.
- قدرة النظام على الاستمرار في معالجة الحمل دون ظهور اختناقات جديدة.

---

# 27. Bottleneck Detection

## 27.1 Purpose

يعد اكتشاف نقاط الاختناق (Performance Bottlenecks) الهدف الرئيسي لاختبارات الأداء، إذ يساعد فريق التطوير على تحديد المكونات التي تحد من قدرة النظام على التوسع وتحسينها قبل الانتقال إلى بيئة الإنتاج.

---

## 27.2 Common Bottlenecks

قد تظهر الاختناقات في أحد المكونات التالية:

- CPU Utilization
- Memory Consumption
- Database Connections
- Slow SQL Queries
- RabbitMQ Queues
- Worker Processing
- Network Latency
- Disk I/O
- Thread Pools
- Garbage Collection

---

## 27.3 Identification Strategy

يجب تحليل جميع المقاييس بصورة مترابطة وعدم الاعتماد على مؤشر واحد فقط.

على سبيل المثال:

- ارتفاع Response Time مع انخفاض CPU قد يشير إلى مشكلة في قاعدة البيانات.
- زيادة Queue Size مع انخفاض استهلاك Workers قد تشير إلى مشكلة في معالجة الرسائل.
- ارتفاع CPU مع ثبات Throughput قد يشير إلى وجود خوارزمية غير فعالة.

---

## 27.4 Improvement Cycle

بعد اكتشاف الاختناق يجب اتباع دورة تحسين واضحة:

```text
Measure

↓

Identify Bottleneck

↓

Optimize

↓

Retest

↓

Compare Results

↓

Document Findings
```

---

# 28. Monitoring During Load Tests

## 28.1 Purpose

لا تقتصر اختبارات الأداء على قياس زمن الاستجابة فقط، بل تتطلب مراقبة جميع مكونات النظام أثناء تنفيذ الاختبارات.

---

## 28.2 Monitored Components

تشمل عملية المراقبة:

- Spring Boot
- JVM
- PostgreSQL
- RabbitMQ
- Redis
- Workers
- Docker Containers
- Operating System

---

## 28.3 Real-Time Monitoring

يجب مراقبة النظام بشكل لحظي أثناء تنفيذ الاختبارات لاكتشاف أي تغيرات غير طبيعية في الأداء.

---

## 28.4 Metrics Collection

يجب جمع جميع المقاييس بصورة مستمرة طوال مدة الاختبار، وعدم الاكتفاء بالنتائج النهائية فقط.

---

# 29. Grafana Dashboards

## 29.1 Purpose

تستخدم Grafana لعرض مؤشرات الأداء بصورة مرئية تساعد في تحليل النتائج واكتشاف الأنماط بسهولة.

---

## 29.2 Recommended Dashboards

ينصح بإنشاء لوحات مستقلة لكل مكون:

- Application Dashboard
- JVM Dashboard
- PostgreSQL Dashboard
- RabbitMQ Dashboard
- Redis Dashboard
- Docker Dashboard
- System Dashboard

---

## 29.3 Dashboard Metrics

تشمل الرسوم البيانية:

- Response Time
- Throughput
- Error Rate
- Queue Size
- CPU Usage
- Memory Usage
- Database Connections
- Cache Hit Ratio

---

# 30. Prometheus Metrics

## 30.1 Purpose

يستخدم Prometheus لجمع المقاييس من جميع خدمات النظام بصورة دورية.

---

## 30.2 Metrics Sources

تشمل:

- Spring Boot Actuator
- Micrometer
- PostgreSQL Exporter
- RabbitMQ Exporter
- Redis Exporter
- Node Exporter

---

## 30.3 Custom Metrics

يمكن إضافة مؤشرات خاصة بالمشروع مثل:

- Requests Created
- Requests Completed
- Outbox Events Published
- Worker Success Count
- Worker Failure Count
- Retry Count

---

## 30.4 Metrics Retention

يجب الاحتفاظ بنتائج الاختبارات لفترة تسمح بمقارنتها مع الإصدارات المستقبلية للنظام.

---

# 31. Result Analysis

## 31.1 Purpose

بعد انتهاء الاختبارات يجب تحليل النتائج بصورة منهجية لاستخلاص الاستنتاجات واتخاذ قرارات التحسين.

---

## 31.2 Analysis Process

يشمل التحليل:

- مقارنة النتائج مع المتطلبات.
- مقارنة السيناريوهات المختلفة.
- تحديد الاتجاهات.
- تحديد الاختناقات.
- اقتراح التحسينات.

---

## 31.3 Trend Analysis

ينصح بمقارنة نتائج كل إصدار مع الإصدارات السابقة لمعرفة ما إذا كان الأداء يتحسن أو يتراجع مع مرور الوقت.

---

## 31.4 Reporting

يجب أن يتضمن تقرير الأداء:

- وصف سيناريو الاختبار.
- إعدادات البيئة.
- المؤشرات الرئيسية.
- الرسوم البيانية.
- المشكلات المكتشفة.
- التوصيات.

---

# 32. Pass / Fail Criteria

## 32.1 Purpose

تحدد هذه المعايير ما إذا كان النظام قد اجتاز اختبارات الأداء أم لا.

---

## 32.2 Pass Conditions

يعتبر الاختبار ناجحًا إذا:

- حقق النظام مؤشرات الأداء المطلوبة.
- بقي Error Rate ضمن الحدود المقبولة.
- لم يحدث فقدان للبيانات.
- لم يحدث فقدان للرسائل.
- لم تنهار أي خدمة.
- بقي النظام مستقرًا طوال مدة الاختبار.

---

## 32.3 Failure Conditions

يعتبر الاختبار فاشلًا إذا:

- تجاوز زمن الاستجابة الحدود المحددة.
- ارتفعت نسبة الأخطاء بصورة كبيرة.
- توقفت إحدى الخدمات.
- امتلأت Queue دون معالجتها.
- فقدت رسائل أو بيانات.

---

# 33. Best Practices

توصي الوثيقة بالالتزام بالممارسات التالية:

- تنفيذ الاختبارات على بيئة معزولة.
- استخدام بيانات اختبار واقعية.
- توثيق إعدادات البيئة بالكامل.
- تشغيل نفس السيناريو أكثر من مرة.
- مقارنة النتائج بين الإصدارات.
- مراقبة جميع الخدمات أثناء الاختبار.
- الاحتفاظ بجميع التقارير.
- أتمتة الاختبارات داخل CI/CD.
- مراجعة النتائج قبل أي إصدار جديد.

---

# 34. References

## Official Documentation

- k6 Documentation
- Spring Boot Actuator Documentation
- Micrometer Documentation
- Prometheus Documentation
- Grafana Documentation
- PostgreSQL Documentation
- RabbitMQ Documentation
- Redis Documentation

---

## Books

- Designing Data-Intensive Applications — Martin Kleppmann
- Release It! — Michael T. Nygard
- Building Microservices — Sam Newman
- Site Reliability Engineering — Google

---

## Internal Documents

- TESTING-STRATEGY.md
- UNIT-TESTING.md
- INTEGRATION-TESTING.md
- API Specification
- Deployment Architecture
- Monitoring Architecture

---

# 35. Summary

تمثل اختبارات الأداء المرحلة الأساسية للتحقق من أن **High-Load Request Management System (HLRMS)** قادر على معالجة الأحمال العالية بصورة مستقرة وموثوقة.

توثق هذه الوثيقة منهجية تنفيذ اختبارات الأداء باستخدام **k6**، وآلية قياس مؤشرات الأداء، ومراقبة مكونات النظام، وتحليل النتائج، وتحديد الاختناقات، وقياس قابلية التوسع.

الالتزام بالإرشادات الواردة في هذه الوثيقة يساعد على بناء نظام قادر على معالجة آلاف الطلبات المتزامنة مع الحفاظ على سلامة البيانات واستقرار الخدمات.

وتعد هذه الوثيقة المرجع الرسمي لجميع اختبارات الأداء في مشروع HLRMS.

---

# Document Information

| Property | Value |
|----------|-------|
| Document | LOAD-TESTING.md |
| Version | 1.0 |
| Status | Approved |
| Owner | HLRMS Development Team |
| Category | Testing Documentation |
| Last Updated | Load Testing Phase |