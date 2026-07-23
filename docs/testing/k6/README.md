# k6 Performance Testing

**Project:** High-Load Request Management System (HLRMS)  
**Category:** Performance and Load Testing  
**Tool:** k6  
**Version:** 1.0  
**Status:** In Progress  

---

# 1. Purpose

## 1.1 Overview

يحتوي هذا المجلد على سكربتات اختبارات الأداء والحمل الخاصة بمشروع **High-Load Request Management System (HLRMS)** باستخدام أداة **k6**.

تهدف هذه السكربتات إلى قياس قدرة النظام على استقبال ومعالجة عدد كبير من الطلبات المتزامنة، والتحقق من استقراره تحت ظروف تشغيل مختلفة، بدءًا من الاختبارات البسيطة وحتى اختبارات الضغط العالي والتعافي.

---

## 1.2 Testing Goals

تهدف اختبارات k6 إلى التحقق من:

- صحة عمل REST APIs تحت الحمل.
- قدرة النظام على معالجة عدد كبير من الطلبات المتزامنة.
- زمن استجابة النظام.
- معدل نجاح وفشل الطلبات.
- قدرة النظام على التوسع الأفقي.
- استقرار RabbitMQ تحت الضغط.
- استقرار Workers أثناء معالجة الرسائل.
- أداء PostgreSQL تحت الحمل.
- فعالية Redis في تقليل زمن الاستجابة.
- قدرة النظام على التعافي بعد ارتفاع الحمل.
- اكتشاف الاختناقات قبل نشر النظام.

---

## 1.3 Scope

تغطي سكربتات k6 الجوانب التالية:

- Authentication API
- Request Creation API
- Request Retrieval API
- Request Listing API
- Request Processing Flow
- Concurrent Requests
- Sustained Load
- Sudden Traffic Spikes
- System Stress
- Long-Running Stability
- Recovery after Load

---

## 1.4 Out of Scope

لا تغطي هذه السكربتات بصورة مباشرة:

- Unit Testing
- Component Testing
- UI Testing
- Android UI Performance
- Database Migration Testing
- Penetration Testing
- Manual Acceptance Testing

يتم توثيق هذه الأنواع من الاختبارات في مستندات أخرى داخل مجلد:

```text
docs/testing/
```

---

# 2. Directory Structure

## 2.1 Folder Layout

يتكون مجلد اختبارات k6 من الملفات التالية:

```text
docs/testing/k6/
├── README.md
├── config.js
├── helpers.js
├── thresholds.js
├── smoke.js
├── load.js
├── stress.js
├── spike.js
├── soak.js
└── recovery.js
```

---

## 2.2 File Responsibilities

| File | Responsibility |
|------|----------------|
| `README.md` | شرح إعداد وتشغيل اختبارات k6 |
| `config.js` | الإعدادات المشتركة وREST API endpoints |
| `helpers.js` | الدوال المساعدة لإنشاء الطلبات والتحقق من الاستجابات |
| `thresholds.js` | حدود النجاح والفشل الخاصة بالاختبارات |
| `smoke.js` | اختبار سريع للتأكد من أن النظام يعمل |
| `load.js` | اختبار الحمل الطبيعي المتوقع |
| `stress.js` | اختبار النظام بحمل أعلى من طاقته الطبيعية |
| `spike.js` | اختبار الارتفاع المفاجئ في عدد المستخدمين |
| `soak.js` | اختبار استقرار النظام لمدة طويلة |
| `recovery.js` | اختبار تعافي النظام بعد الحمل المرتفع |

---

## 2.3 Shared Components

تعتمد جميع سكربتات الاختبار على الملفات المشتركة التالية:

```text
config.js
helpers.js
thresholds.js
```

الهدف من ذلك هو:

- منع تكرار الكود.
- توحيد إعدادات الاختبارات.
- توحيد قيم Thresholds.
- تسهيل تعديل API endpoints.
- تسهيل إضافة سيناريوهات جديدة.
- تحسين قابلية الصيانة.

---

# 3. Test Scenarios

## 3.1 Smoke Test

يستخدم `smoke.js` للتحقق السريع من أن النظام يعمل بصورة أساسية قبل تنفيذ اختبارات الحمل الأكبر.

يتحقق الاختبار من:

- إمكانية الوصول إلى النظام.
- نجاح Authentication.
- نجاح إنشاء Request.
- نجاح استرجاع Request.
- صحة رموز HTTP الأساسية.

مثال التشغيل:

```bash
k6 run docs/testing/k6/smoke.js
```

---

## 3.2 Load Test

يستخدم `load.js` لمحاكاة الحمل الطبيعي المتوقع أثناء تشغيل النظام.

يهدف الاختبار إلى قياس:

- Response Time.
- Throughput.
- Error Rate.
- Requests per Second.
- استقرار النظام تحت الحمل المتوقع.

مثال التشغيل:

```bash
k6 run docs/testing/k6/load.js
```

---

## 3.3 Stress Test

يستخدم `stress.js` لزيادة الحمل تدريجيًا حتى الوصول إلى حدود قدرة النظام.

يساعد الاختبار على تحديد:

- الحد الأقصى للمستخدمين المتزامنين.
- نقطة بداية تدهور الأداء.
- نقطة فشل النظام.
- سلوك النظام بعد تجاوز سعته الطبيعية.
- الموارد التي تمثل Bottleneck.

مثال التشغيل:

```bash
k6 run docs/testing/k6/stress.js
```

---

## 3.4 Spike Test

يستخدم `spike.js` لمحاكاة ارتفاع مفاجئ وكبير في عدد الطلبات.

يهدف الاختبار إلى التحقق من:

- استجابة النظام للزيادات المفاجئة.
- قدرة RabbitMQ على استيعاب الرسائل.
- قدرة Workers على معالجة تراكم Queue.
- استقرار REST API.
- سرعة عودة النظام إلى حالته الطبيعية.

مثال التشغيل:

```bash
k6 run docs/testing/k6/spike.js
```

---

## 3.5 Soak Test

يستخدم `soak.js` لتشغيل حمل مستمر لمدة طويلة.

يهدف الاختبار إلى اكتشاف:

- Memory Leaks.
- تدهور الأداء مع مرور الوقت.
- تراكم الاتصالات.
- تراكم الرسائل داخل RabbitMQ.
- مشكلات Connection Pools.
- مشكلات استهلاك الموارد.

مثال التشغيل:

```bash
k6 run docs/testing/k6/soak.js
```

---

## 3.6 Recovery Test

يستخدم `recovery.js` للتحقق من قدرة النظام على التعافي بعد التعرض لحمل مرتفع.

يتضمن السيناريو عادة المراحل التالية:

```text
Normal Load
     ↓
High Load
     ↓
Peak Load
     ↓
Load Reduction
     ↓
Recovery Observation
```

يتم التحقق من:

- انخفاض Response Time بعد انتهاء الحمل.
- انخفاض Error Rate.
- معالجة الرسائل المتراكمة.
- عودة Queue Depth إلى المستوى الطبيعي.
- استقرار النظام بعد التعافي.

مثال التشغيل:

```bash
k6 run docs/testing/k6/recovery.js
```

---

# 4. Prerequisites

## 4.1 Required Software

قبل تشغيل الاختبارات، يجب توفر المكونات التالية:

| Component | Purpose |
|----------|---------|
| k6 | تنفيذ اختبارات الأداء |
| Java 21 | تشغيل خدمات Spring Boot |
| Maven | بناء وتشغيل Backend |
| Docker | تشغيل البنية التحتية |
| PostgreSQL | قاعدة البيانات |
| RabbitMQ | Message Broker |
| Redis | Cache |
| Prometheus | جمع Metrics |
| Grafana | عرض ومراقبة Metrics |

---

## 4.2 Required Services

يجب تشغيل الخدمات التالية قبل بدء الاختبارات:

```text
API Gateway
Authentication Service
Request Service
Worker Service
PostgreSQL
RabbitMQ
Redis
Prometheus
Grafana
```

قد تختلف الخدمات المطلوبة وفق سيناريو الاختبار.

---

## 4.3 System Availability

قبل تشغيل الاختبار، يجب التأكد من أن النظام متاح عبر:

```text
http://localhost:8080
```

أو عبر القيمة المحددة في متغير البيئة:

```text
BASE_URL
```

---

## 4.4 Health Check

ينصح بالتحقق من صحة النظام قبل تشغيل k6 باستخدام:

```bash
curl http://localhost:8080/actuator/health
```

الاستجابة المتوقعة:

```json
{
  "status": "UP"
}
```

يجب عدم تشغيل اختبارات الحمل إذا كانت إحدى الخدمات الأساسية في حالة غير سليمة.

---

## 4.5 Test User

يجب إنشاء مستخدم خاص بالاختبارات داخل بيئة الاختبار.

مثال:

```text
Username: performance-user
Password: performance-password
```

يجب عدم استخدام حسابات حقيقية أو بيانات Production داخل سكربتات الاختبار.

---

# 5. Installing k6

## 5.1 Verify Installation

للتحقق من أن k6 مثبت بصورة صحيحة:

```bash
k6 version
```

يجب أن يعرض الأمر إصدار k6 المثبت.

---

## 5.2 Docker Execution

يمكن تشغيل k6 باستخدام Docker دون تثبيته محليًا.

مثال:

```bash
docker run --rm -i grafana/k6 run - < docs/testing/k6/smoke.js
```

لكن عند استخدام ملفات JavaScript متعددة مترابطة، يفضل ربط مجلد المشروع داخل Container:

```bash
docker run --rm \
  -v "$(pwd):/project" \
  -w /project \
  grafana/k6 \
  run docs/testing/k6/smoke.js
```

---

## 5.3 Docker Networking

عند تشغيل Backend داخل Docker، قد لا يشير:

```text
localhost
```

إلى خدمة Backend من داخل Container الخاص بـ k6.

في هذه الحالة يجب استخدام اسم الخدمة الموجود داخل Docker Network، مثل:

```bash
docker run --rm \
  --network hlrms-network \
  -v "$(pwd):/project" \
  -w /project \
  -e BASE_URL=http://api-gateway:8080 \
  grafana/k6 \
  run docs/testing/k6/load.js
```

---

# 6. Environment Variables

## 6.1 Overview

تعتمد سكربتات الاختبار على Environment Variables بدل تثبيت القيم داخل الكود.

يسمح ذلك بتشغيل الاختبارات على بيئات مختلفة دون تعديل ملفات JavaScript.

---

## 6.2 Supported Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `BASE_URL` | العنوان الأساسي للنظام | `http://localhost:8080` |
| `API_PREFIX` | بادئة REST APIs | `/api/v1` |
| `LOGIN_ENDPOINT` | مسار تسجيل الدخول | `/auth/login` |
| `REQUEST_ENDPOINT` | مسار إدارة الطلبات | `/requests` |
| `TEST_USERNAME` | اسم مستخدم الاختبار | لا توجد قيمة آمنة افتراضية |
| `TEST_PASSWORD` | كلمة مرور مستخدم الاختبار | لا توجد قيمة آمنة افتراضية |
| `AUTH_TOKEN` | JWT Token اختياري | فارغ |
| `REQUEST_TIMEOUT` | مهلة HTTP Request | `30s` |
| `TEST_RUN_ID` | معرف تشغيل الاختبار | Generated |
| `DEBUG` | تفعيل رسائل Debug | `false` |

---

## 6.3 Basic Execution

يمكن تمرير Environment Variables باستخدام الخيار `-e`:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TEST_USERNAME=performance-user \
  -e TEST_PASSWORD=performance-password \
  docs/testing/k6/smoke.js
```

---

## 6.4 Using an Existing Token

إذا كان JWT Token متوفرًا مسبقًا، يمكن تمريره مباشرة:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e AUTH_TOKEN="your-jwt-token" \
  docs/testing/k6/load.js
```

عند وجود `AUTH_TOKEN`، يمكن للسكربت تجاوز عملية تسجيل الدخول حسب تنفيذ `helpers.js`.

---

## 6.5 Bash Environment Variables

يمكن تصدير القيم قبل تشغيل الاختبار:

```bash
export BASE_URL=http://localhost:8080
export TEST_USERNAME=performance-user
export TEST_PASSWORD=performance-password

k6 run docs/testing/k6/load.js
```

---

## 6.6 PowerShell Environment Variables

على Windows PowerShell:

```powershell
$env:BASE_URL = "http://localhost:8080"
$env:TEST_USERNAME = "performance-user"
$env:TEST_PASSWORD = "performance-password"

k6 run docs/testing/k6/load.js
```

---

## 6.7 Environment Safety

يجب الالتزام بالقواعد التالية:

- عدم كتابة Passwords داخل Git.
- عدم تخزين JWT Tokens داخل الملفات.
- عدم استخدام بيانات Production.
- عدم طباعة Tokens في Logs.
- استخدام حسابات مخصصة للاختبارات.
- تغيير بيانات الاختبار بصورة دورية.
- استخدام Secrets داخل CI/CD.
- عدم رفع ملفات تحتوي على Credentials.

---

## 6.8 Example Local Configuration

مثال لإعداد بيئة تطوير محلية:

```bash
export BASE_URL=http://localhost:8080
export API_PREFIX=/api/v1
export LOGIN_ENDPOINT=/auth/login
export REQUEST_ENDPOINT=/requests
export TEST_USERNAME=performance-user
export TEST_PASSWORD=performance-password
export REQUEST_TIMEOUT=30s
export DEBUG=false
```

ثم تشغيل Smoke Test:

```bash
k6 run docs/testing/k6/smoke.js
```

---

# 7. Authentication

## 7.1 Overview

تحتاج معظم REST APIs في مشروع HLRMS إلى مصادقة باستخدام JWT.

تدعم سكربتات k6 طريقتين للحصول على Access Token:

1. تنفيذ Login Request داخل الاختبار.
2. تمرير Token جاهز باستخدام `AUTH_TOKEN`.

---

## 7.2 Login-Based Authentication

عند عدم تمرير `AUTH_TOKEN`، يقوم السكربت بإرسال طلب تسجيل دخول باستخدام:

```text
TEST_USERNAME
TEST_PASSWORD
```

إلى المسار:

```text
POST /api/v1/auth/login
```

مثال Request Body:

```json
{
  "username": "performance-user",
  "password": "performance-password"
}
```

مثال Response متوقع:

```json
{
  "accessToken": "jwt-access-token",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

---

## 7.3 Pre-Generated Token

يمكن تجاوز Login API وتمرير Token جاهز:

```bash
k6 run \
  -e AUTH_TOKEN="your-jwt-token" \
  docs/testing/k6/load.js
```

يفيد هذا الأسلوب في الحالات التالية:

- عزل اختبار Request API عن Authentication API.
- منع Login Endpoint من التأثير على نتائج الأداء.
- تشغيل اختبارات طويلة باستخدام Token صالح.
- تقليل عدد طلبات المصادقة غير الضرورية.

---

## 7.4 Authorization Header

يتم إرسال Token في HTTP Header بالشكل التالي:

```text
Authorization: Bearer <token>
```

مثال:

```javascript
const headers = {
  Authorization: `Bearer ${token}`,
  'Content-Type': 'application/json',
};
```

---

## 7.5 Token Validation

يجب أن يتحقق السكربت من:

- وجود Token داخل الاستجابة.
- أن Token ليس فارغًا.
- نجاح Login Request.
- عدم استخدام Token منتهي الصلاحية.
- عدم طباعة Token داخل Logs.

---

## 7.6 Authentication Failure

إذا فشلت عملية المصادقة، يجب أن يتوقف الاختبار أو يفشل مبكرًا بدل متابعة إرسال طلبات غير مصرح بها.

تشمل حالات الفشل:

- HTTP `400`
- HTTP `401`
- HTTP `403`
- بيانات مستخدم غير صحيحة.
- Response Body غير متوقع.
- Token مفقود.

---

## 7.7 Token Reuse

يفضل الحصول على Token مرة واحدة داخل `setup()` وإعادة استخدامه داخل Virtual Users عندما يكون الهدف قياس أداء Request APIs فقط.

مثال تدفق الاختبار:

```text
setup()
   ↓
Login
   ↓
Return Access Token
   ↓
Virtual Users
   ↓
Authenticated Requests
```

---

## 7.8 Authentication Load Testing

عند اختبار Authentication API نفسه، يجب عدم إعادة استخدام Token.

بدلًا من ذلك، يتم إرسال Login Requests تحت الحمل وقياس:

- Login Response Time.
- Authentication Error Rate.
- Token Generation Throughput.
- Database Load.
- Redis Session Performance، إذا كان مستخدمًا.
- CPU Usage أثناء إنشاء Tokens.

---

# 8. Test Data

## 8.1 Overview

تعتمد اختبارات الأداء على Test Data مخصصة وآمنة وقابلة للتكرار.

يجب أن تكون بيانات الاختبار:

- Synthetic.
- مستقلة عن Production.
- قابلة للحذف.
- قابلة لإعادة الإنشاء.
- غير حساسة.
- مناسبة للحمل المتزامن.

---

## 8.2 Test User Data

يجب توفير مستخدم واحد أو أكثر لتشغيل الاختبارات.

مثال:

```text
performance-user-001
performance-user-002
performance-user-003
```

عند الحاجة إلى حمل كبير، يفضل استخدام مجموعة مستخدمين بدل استخدام حساب واحد لجميع Virtual Users.

---

## 8.3 Request Payload

مثال Payload لإنشاء Request:

```json
{
  "type": "STANDARD",
  "priority": "NORMAL",
  "payload": {
    "source": "k6",
    "operation": "performance-test"
  }
}
```

يجب تعديل الحقول وفق API Specification الفعلية الخاصة بالمشروع.

---

## 8.4 Unique Data

يجب تجنب إرسال نفس البيانات تمامًا في جميع الطلبات إذا كان النظام يطبق:

- Idempotency.
- Unique Constraints.
- Duplicate Detection.
- Request Deduplication.

يمكن إنشاء قيم فريدة باستخدام:

```javascript
const uniqueId = `${__VU}-${__ITER}-${Date.now()}`;
```

مثال:

```javascript
const payload = {
  externalReference: `k6-${__VU}-${__ITER}-${Date.now()}`,
};
```

---

## 8.5 Correlation ID

يفضل إرسال Correlation ID مع كل طلب:

```text
X-Correlation-ID
```

مثال:

```javascript
const correlationId = `k6-${__VU}-${__ITER}-${Date.now()}`;
```

يساعد ذلك على:

- تتبع الطلب داخل Logs.
- تتبع الرسالة داخل RabbitMQ.
- ربط API Request مع Worker Processing.
- تحليل الأخطاء.
- تحليل Distributed Traces.

---

## 8.6 Test Run ID

يجب أن يمتلك كل تشغيل للاختبار معرفًا خاصًا:

```text
TEST_RUN_ID
```

مثال:

```text
load-2026-07-23-001
```

يمكن استخدامه في:

- Request Payload.
- HTTP Headers.
- Logs.
- Database Records.
- Prometheus Labels.
- Test Reports.

---

## 8.7 Data Volume

يجب تحديد حجم البيانات المتوقع قبل بدء الاختبار.

مثال:

| Scenario | Expected Created Requests |
|----------|---------------------------|
| Smoke | أقل من 20 |
| Load | من 1,000 إلى 100,000 |
| Stress | حسب نقطة الفشل |
| Spike | عدد كبير خلال فترة قصيرة |
| Soak | قد يصل إلى ملايين السجلات |

---

## 8.8 Data Cleanup

بعد انتهاء الاختبار، يجب تنظيف البيانات الناتجة عند الحاجة.

يمكن تنفيذ التنظيف باستخدام:

- Database Cleanup Script.
- Test Data API.
- SQL Script.
- Scheduled Cleanup Job.
- Test Run ID.

مثال SQL منطقي:

```sql
DELETE FROM requests
WHERE test_run_id = 'load-2026-07-23-001';
```

يجب عدم تنفيذ Cleanup مباشرة على Production.

---

## 8.9 Data Isolation

يجب عزل بيانات كل اختبار عن الاختبارات الأخرى باستخدام:

- Unique Test Run ID.
- Unique User Accounts.
- Unique Request References.
- Dedicated Test Environment.
- Separate Database Schema عند الحاجة.

---

## 8.10 Test Data Reference

يتم توثيق استراتيجية إدارة البيانات بصورة تفصيلية في:

```text
docs/testing/TEST-DATA.md
```

---

# 9. Thresholds

## 9.1 Overview

تمثل Thresholds شروط النجاح والفشل التي يقيّم k6 الاختبار بناءً عليها.

إذا لم تتحقق Thresholds، ينتهي الاختبار بحالة فشل حتى لو اكتمل التنفيذ.

---

## 9.2 Common Thresholds

تشمل الحدود الأساسية:

- HTTP Error Rate.
- Response Time.
- Request Failure Rate.
- Check Success Rate.
- Custom Business Metrics.
- Scenario-Specific Metrics.

---

## 9.3 Default Threshold Example

```javascript
export const defaultThresholds = {
  http_req_failed: ['rate<0.01'],
  http_req_duration: ['p(95)<500', 'p(99)<1000'],
  checks: ['rate>0.99'],
};
```

تعني هذه القيم:

- أقل من 1% من HTTP Requests تفشل.
- 95% من الطلبات تنتهي خلال أقل من 500 ms.
- 99% من الطلبات تنتهي خلال أقل من 1000 ms.
- أكثر من 99% من Checks تنجح.

---

## 9.4 Scenario-Specific Thresholds

يجب أن تختلف Thresholds حسب نوع الاختبار.

مثال:

| Scenario | Error Rate | p95 Response Time |
|----------|------------|-------------------|
| Smoke | أقل من 1% | أقل من 1000 ms |
| Load | أقل من 1% | أقل من 500 ms |
| Stress | أقل من 5% قبل نقطة الفشل | حسب المرحلة |
| Spike | أقل من 10% أثناء الذروة | حسب السعة |
| Soak | أقل من 1% | مستقر طوال المدة |
| Recovery | العودة للحد الطبيعي | بعد انتهاء الذروة |

---

## 9.5 Tagged Thresholds

يمكن تطبيق Thresholds على Endpoint محدد باستخدام Tags.

مثال:

```javascript
export const thresholds = {
  'http_req_duration{endpoint:create-request}': [
    'p(95)<500',
  ],
  'http_req_failed{endpoint:create-request}': [
    'rate<0.01',
  ],
};
```

---

## 9.6 Business Thresholds

بالإضافة إلى HTTP Metrics، يجب تعريف Metrics مرتبطة بسلوك النظام.

أمثلة:

- نسبة نجاح إنشاء Request.
- نسبة نجاح استرجاع Request.
- زمن قبول Request.
- زمن اكتمال المعالجة.
- نسبة الرسائل التي انتقلت إلى DLQ.
- نسبة الطلبات التي احتاجت إلى Retry.

---

## 9.7 Threshold Ownership

يجب الاتفاق على Thresholds بين:

- Backend Team.
- QA Team.
- DevOps Team.
- System Architect.
- Product أو Academic Supervisor عند الحاجة.

---

## 9.8 Threshold Stability

لا يجب تعديل Thresholds فقط لجعل الاختبار ينجح.

عند فشل Threshold يجب أولًا التحقق من:

- وجود Bottleneck.
- صحة بيئة الاختبار.
- صحة Workload.
- صحة البيانات.
- وجود أعطال في الخدمات.
- صحة إعدادات Infrastructure.

---

# 10. Metrics

## 10.1 Built-in Metrics

يوفر k6 مجموعة Metrics افتراضية، منها:

| Metric | Description |
|--------|-------------|
| `http_reqs` | العدد الإجمالي لطلبات HTTP |
| `http_req_duration` | زمن طلب HTTP الكامل |
| `http_req_waiting` | Time to First Byte |
| `http_req_connecting` | زمن إنشاء الاتصال |
| `http_req_tls_handshaking` | زمن TLS Handshake |
| `http_req_sending` | زمن إرسال الطلب |
| `http_req_receiving` | زمن استلام الاستجابة |
| `http_req_failed` | نسبة طلبات HTTP الفاشلة |
| `iterations` | عدد Iterations المكتملة |
| `iteration_duration` | زمن كل Iteration |
| `vus` | عدد Virtual Users الحالي |
| `vus_max` | الحد الأقصى لـ Virtual Users |
| `checks` | نسبة نجاح Checks |
| `data_received` | حجم البيانات المستلمة |
| `data_sent` | حجم البيانات المرسلة |

---

## 10.2 Response Time Percentiles

يجب الاعتماد على Percentiles بدل Average فقط.

أهم القيم:

- `p(50)` يمثل Median.
- `p(90)` يمثل 90% من الطلبات.
- `p(95)` يمثل 95% من الطلبات.
- `p(99)` يمثل 99% من الطلبات.
- `max` يمثل أبطأ طلب.

مثال:

```text
p(50) = 120 ms
p(95) = 480 ms
p(99) = 900 ms
```

---

## 10.3 Custom Metrics

يمكن تعريف Metrics مخصصة:

```javascript
import { Counter, Rate, Trend } from 'k6/metrics';

export const createdRequests = new Counter(
  'created_requests',
);

export const businessErrors = new Rate(
  'business_errors',
);

export const requestProcessingTime = new Trend(
  'request_processing_time',
  true,
);
```

---

## 10.4 Counter

يستخدم `Counter` لعد الأحداث.

أمثلة:

- عدد Requests المنشأة.
- عدد عمليات Login.
- عدد Retries.
- عدد الرسائل الفاشلة.
- عدد Responses غير المتوقعة.

---

## 10.5 Rate

يستخدم `Rate` لحساب نسبة النجاح أو الفشل.

أمثلة:

- Authentication Failure Rate.
- Business Error Rate.
- Request Creation Failure Rate.
- Polling Timeout Rate.

---

## 10.6 Trend

يستخدم `Trend` لتسجيل قيم زمنية أو رقمية قابلة للتحليل.

أمثلة:

- Processing Time.
- Queue Waiting Time.
- End-to-End Request Time.
- Database Operation Time.
- Recovery Time.

---

## 10.7 Business Metrics

ينصح بجمع Metrics مثل:

| Metric | Purpose |
|--------|---------|
| `request_creation_success` | قياس نجاح إنشاء الطلبات |
| `request_processing_time` | قياس زمن المعالجة الكامل |
| `request_polling_attempts` | قياس عدد محاولات الاستعلام |
| `authentication_failures` | قياس فشل تسجيل الدخول |
| `unexpected_status_codes` | قياس Responses غير المتوقعة |
| `recovery_duration` | قياس زمن التعافي |

---

## 10.8 Infrastructure Metrics

لا يستطيع k6 وحده قياس جميع مكونات النظام الداخلية، لذلك يجب ربط نتائجه مع Metrics البنية التحتية، ومنها:

- CPU Usage.
- Memory Usage.
- JVM Heap.
- Garbage Collection.
- Thread Count.
- Database Connections.
- RabbitMQ Queue Depth.
- RabbitMQ Publish Rate.
- RabbitMQ Consumer Rate.
- Redis Hit Ratio.
- Redis Memory Usage.
- Worker Processing Rate.

---

# 11. Request Validation

## 11.1 Overview

يجب ألا يكتفي الاختبار بقياس Response Time، بل يجب التحقق أيضًا من صحة الاستجابة.

---

## 11.2 Status Code Check

مثال:

```javascript
check(response, {
  'status is 201': (res) => res.status === 201,
});
```

---

## 11.3 Response Body Check

مثال:

```javascript
check(response, {
  'response contains id': (res) => {
    const body = res.json();
    return body.id !== undefined && body.id !== null;
  },
});
```

---

## 11.4 Content-Type Check

```javascript
check(response, {
  'content type is JSON': (res) =>
    res.headers['Content-Type']?.includes(
      'application/json',
    ),
});
```

---

## 11.5 Business Status Check

إذا كان النظام يعيد حالة الطلب داخل Response Body:

```javascript
check(response, {
  'request status is accepted': (res) => {
    const body = res.json();
    return body.status === 'ACCEPTED';
  },
});
```

---

## 11.6 Safe JSON Parsing

قد يفشل `response.json()` إذا كانت الاستجابة فارغة أو غير صالحة.

لذلك يفضل استخدام دالة آمنة داخل `helpers.js`:

```javascript
export function parseJson(response) {
  try {
    return response.json();
  } catch (error) {
    return null;
  }
}
```

---

## 11.7 Expected Status Codes

يجب تحديد Status Codes المقبولة لكل Endpoint.

مثال:

| Operation | Expected Status |
|-----------|-----------------|
| Login | `200 OK` |
| Create Request | `201 Created` أو `202 Accepted` |
| Get Request | `200 OK` |
| List Requests | `200 OK` |
| Invalid Input | `400 Bad Request` |
| Unauthorized | `401 Unauthorized` |
| Forbidden | `403 Forbidden` |
| Missing Request | `404 Not Found` |
| Duplicate Request | `409 Conflict` |
| Rate Limited | `429 Too Many Requests` |

يجب اعتماد القيم النهائية من API Specification الفعلية.

---

## 11.8 Validation Cost

يجب تجنب Checks ثقيلة جدًا داخل اختبارات الحمل الكبيرة.

لا ينصح بـ:

- معالجة JSON كبيرة بصورة متكررة.
- Logging لكل Request.
- Regex معقد لكل Response.
- حفظ جميع Response Bodies.
- عمليات حسابية غير ضرورية.

الهدف هو التحقق الكافي دون جعل سكربت k6 نفسه Bottleneck.

---

# 12. Running Tests

## 12.1 Smoke Test

تشغيل الاختبار محليًا:

```bash
k6 run docs/testing/k6/smoke.js
```

مع Environment Variables:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TEST_USERNAME=performance-user \
  -e TEST_PASSWORD=performance-password \
  docs/testing/k6/smoke.js
```

---

## 12.2 Load Test

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TEST_USERNAME=performance-user \
  -e TEST_PASSWORD=performance-password \
  docs/testing/k6/load.js
```

---

## 12.3 Stress Test

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TEST_USERNAME=performance-user \
  -e TEST_PASSWORD=performance-password \
  docs/testing/k6/stress.js
```

---

## 12.4 Spike Test

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TEST_USERNAME=performance-user \
  -e TEST_PASSWORD=performance-password \
  docs/testing/k6/spike.js
```

---

## 12.5 Soak Test

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TEST_USERNAME=performance-user \
  -e TEST_PASSWORD=performance-password \
  docs/testing/k6/soak.js
```

---

## 12.6 Recovery Test

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e TEST_USERNAME=performance-user \
  -e TEST_PASSWORD=performance-password \
  docs/testing/k6/recovery.js
```

---

## 12.7 Summary Export

يمكن حفظ ملخص الاختبار في ملف JSON:

```bash
k6 run \
  --summary-export=build/k6/load-summary.json \
  docs/testing/k6/load.js
```

يجب إنشاء المجلد قبل التشغيل عند الحاجة:

```bash
mkdir -p build/k6
```

---

## 12.8 CSV Output

يمكن تصدير Metrics إلى CSV:

```bash
k6 run \
  --out csv=build/k6/load-results.csv \
  docs/testing/k6/load.js
```

---

## 12.9 JSON Output

يمكن تصدير جميع Metrics إلى JSON:

```bash
k6 run \
  --out json=build/k6/load-results.json \
  docs/testing/k6/load.js
```

قد يكون حجم الملف كبيرًا في الاختبارات الطويلة.

---

## 12.10 Custom Test Run ID

```bash
k6 run \
  -e TEST_RUN_ID=load-local-001 \
  docs/testing/k6/load.js
```

---

## 12.11 Debug Mode

```bash
k6 run \
  -e DEBUG=true \
  docs/testing/k6/smoke.js
```

يجب عدم استخدام Debug Logging في اختبارات الحمل الكبيرة إلا عند التحقيق في مشكلة محددة.

---

## 12.12 Abort on Threshold Failure

يمكن استخدام:

```bash
k6 run \
  --exit-on-running \
  docs/testing/k6/load.js
```

لكن سلوك الإيقاف الفعلي يعتمد على Threshold Configuration وطريقة تشغيل الاختبار.

يفضل أن تكون Thresholds معرفة داخل ملفات الاختبار لضمان نتائج متسقة.

---

## 12.13 Execution Order

ينصح بتنفيذ الاختبارات بالترتيب التالي:

```text
Smoke Test
    ↓
Load Test
    ↓
Stress Test
    ↓
Spike Test
    ↓
Recovery Test
    ↓
Soak Test
```

يجب عدم بدء Stress أو Soak Test قبل نجاح Smoke Test والتحقق من صحة بيئة الاختبار.

---

## 12.14 Pre-Test Checklist

قبل بدء أي اختبار، يجب التأكد من:

- جميع الخدمات الأساسية تعمل.
- Health Checks ناجحة.
- بيانات الاختبار جاهزة.
- Credentials صحيحة.
- Prometheus يعمل.
- Grafana Dashboards جاهزة.
- لا توجد اختبارات أخرى على البيئة نفسها.
- لا توجد عمليات Deployment أثناء الاختبار.
- تم تسجيل وقت بداية الاختبار.
- تم تحديد `TEST_RUN_ID`.
- توجد مساحة تخزين كافية للـ Logs والنتائج.

---

# 13. Prometheus Integration

## 13.1 Overview

يمكن دمج k6 مع Prometheus لتصدير Metrics الخاصة باختبارات الأداء، مما يسمح بمراقبة نتائج الاختبارات إلى جانب مؤشرات النظام في الوقت الحقيقي.

يتيح هذا الدمج تحليل أداء النظام ومقارنة مؤشرات k6 مع مؤشرات البنية التحتية مثل استهلاك المعالج والذاكرة وحالة RabbitMQ وPostgreSQL.

---

## 13.2 Architecture

```text
             +----------------------+
             |      k6 Runner       |
             +----------+-----------+
                        |
                        | Metrics
                        v
             +----------------------+
             |     Prometheus       |
             +----------+-----------+
                        |
                        | Query
                        v
             +----------------------+
             |      Grafana         |
             +----------+-----------+
                        |
                        v
               Performance Dashboards
```

---

## 13.3 Exporting Metrics

يمكن تشغيل الاختبارات مع تصدير Metrics إلى Prometheus باستخدام الإضافة المناسبة، أو باستخدام بوابة متوافقة مع Prometheus Remote Write.

مثال عام:

```bash
k6 run \
  --out experimental-prometheus-rw \
  docs/testing/k6/load.js
```

تعتمد طريقة التصدير على البيئة والإصدار المستخدم من k6.

---

## 13.4 Metrics Correlation

ينصح بمقارنة مؤشرات k6 مع مؤشرات النظام التالية:

- JVM Heap Usage
- CPU Usage
- Memory Usage
- Thread Count
- Active Connections
- PostgreSQL Connections
- RabbitMQ Queue Depth
- RabbitMQ Publish Rate
- RabbitMQ Consumer Rate
- Redis Memory Usage
- Redis Cache Hit Ratio

---

## 13.5 Labeling

يفضل استخدام Labels لتمييز كل تشغيل اختبار.

أمثلة:

```text
environment=local
scenario=load
test_run=load-001
service=request-service
```

يسهل ذلك مقارنة النتائج بين البيئات المختلفة.

---

# 14. Grafana Dashboards

## 14.1 Overview

توفر Grafana واجهة رسومية لتحليل نتائج اختبارات الأداء بصورة لحظية أو بعد انتهاء التشغيل.

---

## 14.2 Recommended Dashboards

ينصح بإنشاء Dashboards تعرض:

- Requests per Second
- Response Time
- Response Time Percentiles
- Error Rate
- Active Virtual Users
- Throughput
- CPU Usage
- Memory Usage
- JVM Metrics
- RabbitMQ Metrics
- PostgreSQL Metrics
- Redis Metrics

---

## 14.3 Dashboard Layout

يمكن تقسيم Dashboard إلى الأقسام التالية:

```text
Overview
Performance
HTTP Metrics
RabbitMQ
Database
Redis
JVM
Infrastructure
```

---

## 14.4 Correlation Analysis

يجب تحليل مؤشرات k6 بالتزامن مع مؤشرات البنية التحتية.

مثال:

- ارتفاع Response Time.
- انخفاض Throughput.
- زيادة Queue Depth.
- زيادة CPU Usage.
- انخفاض Cache Hit Ratio.

قد يشير هذا النمط إلى وجود اختناق في أحد مكونات النظام.

---

## 14.5 Test Comparison

يفضل الاحتفاظ بنتائج الاختبارات السابقة لمقارنتها مع الإصدارات الجديدة، مما يساعد على اكتشاف أي تراجع في الأداء (Performance Regression).

---

# 15. Result Interpretation

## 15.1 Overview

لا تعتمد جودة النظام على نجاح الاختبار فقط، بل على تحليل النتائج بصورة صحيحة وربطها بسلوك النظام أثناء التنفيذ.

---

## 15.2 Key Metrics

أهم المؤشرات التي يجب مراجعتها:

| Metric | Description |
|--------|-------------|
| Response Time | زمن الاستجابة |
| p95 Response Time | زمن استجابة 95% من الطلبات |
| Throughput | عدد الطلبات المعالجة |
| Error Rate | نسبة الطلبات الفاشلة |
| Checks | نسبة نجاح التحقق |
| Iterations | عدد الدورات المكتملة |
| Virtual Users | عدد المستخدمين الافتراضيين |

---

## 15.3 Healthy System Indicators

تشمل مؤشرات الأداء الجيد:

- استقرار Response Time.
- انخفاض Error Rate.
- ثبات Throughput.
- عدم تراكم الرسائل في RabbitMQ.
- استقرار استهلاك الذاكرة.
- عدم وجود زيادات غير طبيعية في CPU Usage.

---

## 15.4 Warning Indicators

قد تشير المؤشرات التالية إلى وجود مشكلة:

- ارتفاع p95 بصورة مستمرة.
- انخفاض Throughput.
- ارتفاع Error Rate.
- تراكم الرسائل داخل Queue.
- زيادة Garbage Collection.
- ارتفاع زمن الاستجابة تدريجيًا.

---

## 15.5 Bottleneck Identification

عند اكتشاف مشكلة، يجب تحديد مصدرها، مثل:

- REST API.
- Database.
- RabbitMQ.
- Redis.
- Worker.
- JVM.
- Network.
- Infrastructure.

---

## 15.6 Result Documentation

بعد انتهاء كل اختبار، يفضل توثيق:

- تاريخ التنفيذ.
- نوع الاختبار.
- إصدار النظام.
- البيئة المستخدمة.
- عدد Virtual Users.
- مدة الاختبار.
- أهم النتائج.
- الاستنتاجات.
- الإجراءات المقترحة.

---

# 16. Troubleshooting

## 16.1 Authentication Failure

الأسباب المحتملة:

- بيانات دخول غير صحيحة.
- Token منتهي الصلاحية.
- Login Endpoint غير متاح.
- مشكلة في Authorization Header.

---

## 16.2 Connection Errors

تشمل الأسباب المحتملة:

- Backend غير مشغل.
- BASE_URL غير صحيح.
- مشكلة في Docker Network.
- منفذ الخدمة غير متاح.

---

## 16.3 High Error Rate

ينصح بالتحقق من:

- Logs الخاصة بالخدمات.
- RabbitMQ.
- PostgreSQL.
- Redis.
- JVM Logs.
- Network Latency.

---

## 16.4 Unexpected Response Time

قد يكون السبب:

- Database Locking.
- Queue Congestion.
- Slow Queries.
- CPU Saturation.
- Memory Pressure.
- Garbage Collection.

---

## 16.5 Threshold Failures

عند فشل Threshold، يجب:

1. مراجعة نتائج k6.
2. مراجعة Grafana.
3. مراجعة Prometheus.
4. مراجعة Logs.
5. إعادة تشغيل الاختبار للتأكد من قابلية إعادة إنتاج المشكلة.
6. تحليل السبب الجذري قبل تعديل Thresholds.

---

## 16.6 Script Errors

إذا فشل السكربت نفسه، تحقق من:

- صحة استيراد الملفات.
- Environment Variables.
- أسماء Endpoints.
- تنسيق JSON.
- وجود جميع الملفات المشتركة.

---

# 17. Best Practices

ينصح باتباع الإرشادات التالية عند تنفيذ اختبارات k6:

- تشغيل Smoke Test أولًا.
- استخدام بيئة اختبار معزولة.
- استخدام بيانات اختبار مستقلة.
- تجنب تشغيل اختبارات متعددة على البيئة نفسها في الوقت نفسه.
- إعادة ضبط البيانات عند الحاجة.
- مراقبة مؤشرات النظام أثناء الاختبار.
- حفظ نتائج كل تشغيل.
- توثيق أي تغيير في سيناريوهات الاختبار.
- استخدام Thresholds ثابتة للمقارنة بين الإصدارات.
- مراجعة النتائج قبل اعتمادها.

---

## 17.1 Code Organization

يفضل:

- إعادة استخدام الدوال المشتركة.
- فصل الإعدادات عن السكربتات.
- استخدام أسماء واضحة للسيناريوهات.
- تجنب تكرار الكود.
- كتابة تعليقات عند الحاجة فقط.

---

## 17.2 Performance Considerations

يجب أن يكون سكربت k6 نفسه خفيفًا.

لذلك يفضل:

- تقليل Logging.
- تجنب معالجة JSON غير الضرورية.
- إعادة استخدام Headers.
- إعادة استخدام Tokens.
- تقليل العمليات الحسابية داخل كل Iteration.

---

# 18. CI/CD Integration

## 18.1 Overview

يمكن تشغيل اختبارات k6 تلقائيًا ضمن خطوط CI/CD للتحقق من الأداء قبل نشر الإصدارات الجديدة.

---

## 18.2 Suggested Pipeline

```text
Checkout Source
        ↓
Build Backend
        ↓
Run Unit Tests
        ↓
Run Integration Tests
        ↓
Deploy Test Environment
        ↓
Run Smoke Test
        ↓
Run Load Test
        ↓
Collect Metrics
        ↓
Generate Reports
        ↓
Performance Validation
        ↓
Deployment Decision
```

---

## 18.3 Pipeline Rules

ينصح بتطبيق القواعد التالية:

- تنفيذ Smoke Test في كل Pipeline.
- تنفيذ Load Test قبل الإصدارات الرئيسية.
- تنفيذ Stress وSoak Tests بشكل دوري أو قبل الإصدارات المهمة.
- إيقاف النشر عند فشل Thresholds الحرجة.
- حفظ تقارير الأداء كجزء من مخرجات الـ Pipeline.

---

## 18.4 Performance Regression

يجب مقارنة نتائج كل إصدار بالإصدار السابق لاكتشاف أي تراجع في الأداء.

تشمل المقارنة:

- Response Time.
- Throughput.
- Error Rate.
- Resource Consumption.
- Queue Performance.

---

## 18.5 Report Archiving

ينصح بحفظ الملفات التالية بعد كل تشغيل:

- Summary Report.
- JSON Metrics.
- CSV Metrics (إن وجدت).
- Grafana Screenshots (عند الحاجة).
- Prometheus Snapshots (اختياري).

يساعد ذلك على تتبع تطور أداء النظام عبر الإصدارات المختلفة.

---

# 19. Security Considerations

## 19.1 Credential Protection

يجب عدم تخزين أي من البيانات التالية داخل ملفات k6 أو داخل Git:

- Passwords.
- JWT Tokens.
- API Keys.
- Database Credentials.
- RabbitMQ Credentials.
- Redis Passwords.
- Prometheus Credentials.

يجب تمرير هذه القيم باستخدام:

- Environment Variables.
- CI/CD Secrets.
- Secret Management Systems.
- ملفات إعداد محلية مستثناة من Git.

مثال:

```bash
k6 run \
  -e TEST_USERNAME="$TEST_USERNAME" \
  -e TEST_PASSWORD="$TEST_PASSWORD" \
  docs/testing/k6/load.js
```

---

## 19.2 Sensitive Test Data

يجب ألا تحتوي Test Payloads على:

- بيانات مستخدمين حقيقية.
- معلومات شخصية.
- بيانات مالية حقيقية.
- Tokens مأخوذة من Production.
- معلومات سرية داخل Correlation IDs.
- بيانات يمكن استخدامها لتحديد هوية مستخدم حقيقي.

يجب استخدام بيانات Synthetic مخصصة للاختبارات فقط.

---

## 19.3 Production Testing

لا يجوز تشغيل اختبارات:

- Stress Test
- Spike Test
- Soak Test
- Recovery Test

على بيئة Production دون:

- موافقة رسمية.
- تحديد نافذة تنفيذ.
- تحديد حد أقصى للحمل.
- وجود فريق مراقبة أثناء الاختبار.
- إعداد Abort Plan.
- إعداد Rollback Plan.
- التأكد من عدم التأثير على المستخدمين الحقيقيين.

---

## 19.4 Least Privilege

يجب أن يمتلك مستخدم الاختبار أقل مجموعة صلاحيات لازمة لتنفيذ السيناريو.

مثال:

- مستخدم اختبار عادي لاختبار إنشاء Requests.
- مستخدم Administrator فقط عند اختبار Administrative APIs.
- عدم استخدام Superuser بصورة افتراضية.

---

## 19.5 Logging Safety

يجب عدم طباعة البيانات التالية داخل Logs:

- Access Tokens.
- Passwords.
- Authorization Headers.
- Cookies الحساسة.
- Response Bodies التي تحتوي على بيانات سرية.
- معلومات شخصية.

مثال غير آمن:

```javascript
console.log(`Token: ${token}`);
```

يجب تجنب هذا الأسلوب بصورة كاملة.

---

## 19.6 TLS

في البيئات المشتركة أو البعيدة يجب استخدام:

```text
HTTPS
```

مع التحقق من صحة TLS Certificates.

لا يجوز تعطيل TLS Verification إلا في بيئة محلية معزولة، ولسبب مؤقت وموثق.

---

## 19.7 Rate Limiting Safety

عند اختبار Rate Limiting يجب:

- استخدام حسابات مخصصة.
- عدم استهداف Production.
- تحديد مدة قصيرة للاختبار.
- مراقبة Blocking Rules.
- تنظيف أي حظر مؤقت بعد الاختبار.
- عدم التأثير على مستخدمين آخرين.

---

# 20. Limitations

## 20.1 Client-Side Scope

يقيس k6 أداء النظام من منظور Client، لكنه لا يحدد بمفرده السبب الداخلي لأي Bottleneck.

لذلك يجب دمج نتائجه مع:

- Prometheus Metrics.
- Grafana Dashboards.
- Application Logs.
- PostgreSQL Metrics.
- RabbitMQ Metrics.
- Redis Metrics.
- JVM Metrics.
- Distributed Tracing عند توفره.

---

## 20.2 Environment Differences

لا تمثل نتائج البيئة المحلية أو بيئة Development أداء Production بصورة دقيقة بسبب اختلاف:

- Hardware.
- Network Latency.
- Database Size.
- Number of Service Instances.
- Container Resource Limits.
- Load Balancer Configuration.
- Security Layers.
- External Dependencies.

يجب توثيق البيئة المستخدمة مع كل نتيجة اختبار.

---

## 20.3 Test Data Effects

قد تتأثر نتائج الاختبار بسبب:

- Cache Warm-up.
- Duplicate Data.
- Database صغيرة بصورة غير واقعية.
- Shared Test Users.
- Idempotency Rules.
- Cleanup Jobs.
- Unique Constraints.
- Request Deduplication.

---

## 20.4 External Services

عندما يعتمد HLRMS على خدمات خارجية، قد تقيس نتائج k6 أداء تلك الخدمات أيضًا.

عند الحاجة إلى عزل أداء HLRMS، يمكن استخدام:

- Mocks.
- Stubs.
- Service Virtualization.
- Local Test Implementations.

يجب توثيق ما إذا كانت الخدمات الخارجية حقيقية أو محاكاة.

---

## 20.5 Asynchronous Processing

نجاح:

```text
POST /requests
```

لا يعني بالضرورة اكتمال المعالجة داخل Worker.

يجب التمييز بين:

| Metric | Description |
|--------|-------------|
| API Acceptance Time | زمن قبول Request من REST API |
| Queue Waiting Time | مدة انتظار الرسالة داخل Queue |
| Worker Processing Time | مدة تنفيذ Worker للرسالة |
| End-to-End Time | الزمن من إنشاء Request حتى اكتماله |

---

## 20.6 Polling Impact

عند استخدام Polling للتحقق من اكتمال Request، فإن طلبات Polling نفسها تضيف حملًا على النظام.

لذلك يجب:

- تحديد Polling Interval مناسب.
- تحديد Maximum Attempts.
- عدم تنفيذ Polling سريع جدًا.
- فصل Metrics الخاصة بالإنشاء عن Metrics الخاصة بالاستعلام.
- منع Infinite Polling.

---

## 20.7 Load Generator Capacity

قد يصبح جهاز تشغيل k6 نفسه Bottleneck عند استخدام عدد كبير جدًا من Virtual Users.

يجب مراقبة:

- CPU Usage على k6 Runner.
- Memory Usage.
- Network Bandwidth.
- Open Connections.
- File Descriptor Limits.

عند تجاوز قدرة جهاز واحد، يمكن استخدام Distributed Execution.

---

## 20.8 Result Variability

قد تختلف النتائج بين تشغيل وآخر بسبب:

- JVM Warm-up.
- Garbage Collection.
- Cache State.
- Network Conditions.
- Background Jobs.
- Database Checkpoints.
- Container Scheduling.
- Shared Infrastructure.

لذلك يفضل تكرار الاختبار أكثر من مرة قبل اعتماد النتائج النهائية.

---

# 21. Future Improvements

تشمل التحسينات المستقبلية المقترحة:

- إضافة Authentication Load Scenario.
- إضافة Idempotency Test.
- إضافة Rate Limiting Test.
- إضافة Retry Testing تحت الحمل.
- إضافة Dead Letter Queue Scenario.
- إضافة اختبار Queue Backlog.
- إضافة End-to-End Processing Metrics.
- إضافة Polling Helpers.
- إضافة Multiple Test Users.
- إضافة Data Setup Scripts.
- إضافة Data Cleanup Scripts.
- دعم Distributed k6 Execution.
- إنشاء Performance Baseline لكل إصدار.
- إنشاء Automated Regression Comparison.
- إنشاء HTML Reports.
- إضافة GitHub Actions Pipeline.
- إضافة Jenkins Pipeline.
- ربط النتائج تلقائيًا مع Grafana.
- إضافة Failover Testing.
- إضافة Chaos Engineering Scenarios.
- إضافة Database Saturation Scenario.
- إضافة Worker Scaling Scenario.
- إضافة RabbitMQ Consumer Scaling Scenario.
- إضافة Redis Failure Scenario.

---

## 21.1 Authentication Scenario

يمكن إضافة ملف مستقل مستقبلًا:

```text
authentication.js
```

لقياس:

- Login Throughput.
- JWT Generation Time.
- Authentication Error Rate.
- Database Load.
- Redis Session Performance.

---

## 21.2 Messaging Scenario

يمكن إضافة ملف:

```text
messaging.js
```

لقياس:

- Publish Rate.
- Consumer Rate.
- Queue Waiting Time.
- Retry Rate.
- DLQ Rate.
- Worker Processing Time.

---

## 21.3 Distributed Execution

عند الحاجة إلى حمل كبير، يمكن توزيع الاختبار على أكثر من k6 Runner لتحقيق:

- عدد أكبر من Virtual Users.
- Throughput أعلى.
- تقليل تأثير قيود جهاز واحد.
- محاكاة مصادر Traffic متعددة.

---

## 21.4 Automated Performance Baseline

يمكن حفظ Baseline لكل إصدار ومقارنة النتائج تلقائيًا.

مثال:

| Metric | Baseline | Current | Result |
|--------|----------|---------|--------|
| p95 Response Time | 450 ms | 480 ms | Accepted |
| Error Rate | 0.5% | 0.7% | Accepted |
| Throughput | 1200 RPS | 1100 RPS | Review |
| Queue Recovery | 60 s | 95 s | Failed |

---

# 22. References

## 22.1 Internal Documents

- `docs/testing/README.md`
- `docs/testing/TESTING-STRATEGY.md`
- `docs/testing/UNIT-TESTING.md`
- `docs/testing/INTEGRATION-TESTING.md`
- `docs/testing/LOAD-TESTING.md`
- `docs/testing/TEST-DATA.md`
- `docs/testing/TRACEABILITY-MATRIX.md`
- API Specification
- Monitoring Architecture
- RabbitMQ Architecture
- Security Design
- Deployment Diagram

---

## 22.2 Technical References

- k6 Documentation
- Grafana Documentation
- Prometheus Documentation
- Spring Boot Actuator Documentation
- Micrometer Documentation
- RabbitMQ Monitoring Documentation
- PostgreSQL Monitoring Documentation
- Redis Monitoring Documentation
- Docker Documentation

يجب اعتماد روابط وإصدارات الأدوات المستخدمة فعليًا عند تثبيت البيئة النهائية للمشروع.

---

## 22.3 Related Standards

يمكن الرجوع إلى المبادئ العامة الواردة في:

- ISO/IEC 25010 — Software Product Quality.
- ISO/IEC/IEEE 29119 — Software Testing.
- ISTQB Performance Testing Concepts.

---

# 23. Summary

يوفر مجلد:

```text
docs/testing/k6/
```

البنية الأساسية لاختبارات الأداء والحمل الخاصة بمشروع **High-Load Request Management System (HLRMS)** باستخدام k6.

تغطي الاختبارات السيناريوهات التالية:

- Smoke Testing.
- Load Testing.
- Stress Testing.
- Spike Testing.
- Soak Testing.
- Recovery Testing.

تعتمد السكربتات على إعدادات ودوال وThresholds مشتركة، مما يساعد على:

- منع تكرار الكود.
- توحيد طريقة تنفيذ الاختبارات.
- تسهيل تعديل Endpoints.
- توحيد Metrics.
- توحيد Pass/Fail Criteria.
- تحسين قابلية الصيانة.

كما توضح الوثيقة كيفية:

- إدارة Authentication.
- إنشاء Test Data.
- تمرير Environment Variables.
- تشغيل الاختبارات محليًا أو داخل Docker.
- تصدير النتائج.
- ربط k6 مع Prometheus وGrafana.
- تحليل مؤشرات HTTP والبنية التحتية.
- اكتشاف Bottlenecks.
- دمج الاختبارات ضمن CI/CD.

لا تعد نتائج k6 وحدها كافية للحكم على أداء النظام، بل يجب تحليلها مع Metrics الخاصة بـ:

- PostgreSQL.
- RabbitMQ.
- Redis.
- Workers.
- JVM.
- Containers.
- Infrastructure.

تمثل هذه الوثيقة المرجع الأساسي لتشغيل وصيانة سكربتات k6 في مشروع HLRMS.

---

# 24. Document Information

| Property | Value |
|----------|-------|
| Document | `docs/testing/k6/README.md` |
| Project | High-Load Request Management System |
| Version | 1.0 |
| Status | Approved |
| Owner | HLRMS Development Team |
| Category | Performance Testing Documentation |
| Tool | k6 |
| Language | Arabic with English Technical Terms |
| Last Updated | k6 Testing Phase |