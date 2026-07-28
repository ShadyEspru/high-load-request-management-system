# UNIT-TESTING.md

# Unit Testing Guidelines

**Project:** High-Load Request Management System (HLRMS)

**Version:** 1.0

**Status:** Accepted

---

# 1. Purpose

يحدد هذا المستند المعايير الرسمية لتصميم وتنفيذ اختبارات **Unit Testing** في مشروع HLRMS.

الغرض من Unit Tests ليس فقط اكتشاف الأخطاء البرمجية، بل ضمان أن جميع قواعد الأعمال (Business Rules) تعمل بصورة صحيحة ومستقرة قبل دمج أي تغيير داخل النظام.

تعد Unit Tests أول طبقة في **Test Pyramid**، ولذلك يجب أن تكون:

- سريعة (Fast)
- مستقلة (Independent)
- قابلة للتكرار (Repeatable)
- قابلة للقراءة (Readable)
- قابلة للصيانة (Maintainable)

تعمل جميع Unit Tests مع كل عملية Build داخل بيئة التكامل المستمر (Continuous Integration)، وتشكل خط الدفاع الأول ضد Regression.

---

# 2. Scope

تشمل Unit Tests جميع الوحدات البرمجية التي تحتوي على منطق أعمال (Business Logic) ولا تعتمد مباشرة على بنية تحتية خارجية.

## Included Components

يشمل الاختبار الوحدات التالية:

- Service Classes
- Business Rules
- Domain Services
- Validators
- DTO Mappers
- Utility Classes
- Helper Classes
- State Machine Logic
- Retry Decision Logic
- Priority Resolution Logic
- Idempotency Logic
- Authorization Helper Classes
- Exception Mapping
- Command Handlers
- Query Handlers
- Value Objects

---

## Excluded Components

لا تعتبر العناصر التالية جزءًا من Unit Testing:

- PostgreSQL
- RabbitMQ
- Redis
- Docker
- Docker Compose
- HTTP Communication
- REST Controllers
- Spring Context
- Testcontainers
- Message Broker
- File System
- External APIs

اختبار هذه المكونات يتم ضمن:

- Integration Testing
- Component Testing
- End-to-End Testing

---

# 3. Objectives

تهدف Unit Tests إلى تحقيق الأهداف التالية:

## Functional Correctness

التحقق من أن كل وحدة برمجية تنفذ السلوك المتوقع.

---

## Business Rule Validation

التأكد من أن جميع قواعد الأعمال مطبقة بصورة صحيحة.

---

## Regression Prevention

منع التعديلات المستقبلية من كسر السلوك الحالي.

---

## Documentation

تمثل Unit Tests وثائق تنفيذية (Executable Documentation) توضح كيفية استخدام الكود.

---

## Confidence

منح المطورين الثقة عند إعادة هيكلة الكود (Refactoring).

---

## Fast Feedback

تقديم نتائج فورية بعد كل تعديل.

---

# 4. Testing Principles

يعتمد المشروع المبادئ التالية.

---

## 4.1 Fast

يجب أن تنفذ Unit Tests خلال أجزاء من الثانية.

لا يجوز وجود اختبارات تستغرق عدة ثوانٍ دون مبرر.

---

## 4.2 Independent

كل اختبار مستقل تمامًا.

لا يعتمد على:

- اختبار آخر
- ترتيب التنفيذ
- بيانات مشتركة

---

## 4.3 Deterministic

يعطي الاختبار نفس النتيجة في كل مرة.

يمنع الاعتماد على:

- الوقت الحالي
- البيانات العشوائية
- الشبكة
- حالة النظام

---

## 4.4 Repeatable

يمكن تشغيل الاختبار مئات المرات بنفس النتيجة.

---

## 4.5 Readable

يجب أن يكون الاختبار أسهل قراءة من الكود الذي يختبره.

---

## 4.6 Single Responsibility

كل اختبار يختبر سلوكًا واحدًا فقط.

---

## 4.7 Small

يجب أن يكون حجم الاختبار صغيرًا.

الاختبارات الطويلة عادة تشير إلى تصميم غير جيد.

---

## 4.8 Maintainable

يجب أن تكون صيانة الاختبارات سهلة مثل صيانة الكود.

---

# 5. Test Structure

يعتمد المشروع رسميًا نمط:

```text
Arrange

↓

Act

↓

Assert
```

أو النمط المكافئ:

```text
Given

↓

When

↓

Then
```

---

## Arrange

تهيئة جميع البيانات اللازمة.

مثال:

```java
CreateRequestCommand command =
    RequestTestBuilder.validCommand().build();
```

---

## Act

تنفيذ العملية المراد اختبارها.

```java
Request result = service.create(command);
```

---

## Assert

التحقق من النتيجة.

```java
assertThat(result.getStatus())
    .isEqualTo(RequestStatus.NEW);
```

---

## قواعد Arrange–Act–Assert

- Arrange واحدة.
- Act واحدة.
- Assert واضح.
- لا يفضل وجود أكثر من عملية رئيسية داخل الاختبار.

---

# 6. Naming Convention

يجب أن تصف أسماء الاختبارات:

- العملية
- الشرط
- النتيجة

النمط الرسمي:

```text
method_condition_expectedResult
```

---

## أمثلة صحيحة

```text
createRequest_validCommand_returnsRequestId

createRequest_duplicateKey_returnsExistingRequest

publishEvent_brokerUnavailable_keepsPending

markCompleted_processingRequest_changesStatus

validatePriority_invalidValue_throwsValidationException

retryExceeded_movesMessageToDeadLetterQueue

generateCorrelationId_returnsUniqueIdentifier
```

---

## أمثلة غير مقبولة

```text
test()

test1()

serviceTest()

myTest()

works()

shouldWork()
```

هذه الأسماء لا توضح السلوك المختبر.

---

# 7. Test Class Organization

يفضل أن تطابق أسماء ملفات الاختبار أسماء الكلاسات الأصلية.

مثال:

```text
RequestService

↓

RequestServiceTest
```

---

داخل الملف:

```text
Fields

Setup

Nested Classes

Test Methods

Helper Methods
```

---

يفضل استخدام:

```java
@Nested
```

لتجميع الاختبارات حسب الوظيفة.

مثال:

```text
Create Request

Delete Request

Retry Request

Validation
```

---

# 8. Mocking Strategy

يعتمد المشروع Mockito لمحاكاة الحدود الخارجية فقط.

---

## يسمح بعمل Mock لـ

- Repository
- Publisher
- Clock
- UUID Provider
- External Client
- Notification Service
- Cache Interface

---

## يمنع عمل Mock لـ

- Entity
- DTO
- Value Object
- Business Rule
- الكلاس قيد الاختبار نفسه

---

## مثال صحيح

```java
@Mock

RequestRepository repository;
```

---

## مثال غير صحيح

```java
@Mock

RequestService service;
```

لأن RequestService هو الكلاس الذي نريد اختباره.

---

## قواعد Mocking

- Mock أقل ما يمكن.
- لا تستخدم Deep Stubs.
- لا تعمل Mock لمنطق الأعمال.
- استخدم Fake عندما يكون أكثر وضوحًا.
- لا تتحقق من تفاصيل التنفيذ الداخلية إلا عند الحاجة.

---

## Dependency Injection

يفضل استخدام:

```java
@InjectMocks
```

بدل إنشاء الكائن يدويًا عندما يكون ذلك مناسبًا.

---

## Verification

يستخدم:

```java
verify(repository)
    .save(any());
```

للتحقق من التفاعل مع الحدود الخارجية.

ولا يجب الإفراط في استخدام `verify()` عندما تكون النتيجة النهائية (State) كافية لإثبات صحة السلوك.

---

# 9. Validation Testing

## 9.1 Purpose

يمثل التحقق من صحة البيانات (Validation) خط الدفاع الأول للنظام، حيث يمنع وصول البيانات غير الصحيحة إلى طبقة منطق الأعمال (Business Logic).

يجب أن تركز Unit Tests الخاصة بالتحقق (Validation Tests) على التأكد من أن جميع قواعد التحقق يتم تطبيقها قبل تنفيذ أي عملية داخل النظام.

لا يسمح بانتقال أي طلب يحتوي على بيانات غير صالحة إلى مراحل المعالجة اللاحقة.

---

## 9.2 Validation Levels

تقسم عمليات التحقق داخل المشروع إلى عدة مستويات:

### Field Validation

تشمل التحقق من:

- Required Fields
- Null Values
- Blank Strings
- Empty Collections
- Maximum Length
- Minimum Length
- UUID Format
- Enum Values
- Numeric Ranges

---

### Business Validation

تركز على قواعد العمل مثل:

- عدم قبول أولوية غير معرفة.
- منع إنشاء طلب بدون Client.
- رفض الطلبات المنتهية صلاحيتها.
- منع إنشاء طلب مكرر إذا كان Idempotency-Key مستخدمًا.

---

### Cross Field Validation

يتم فيها التحقق من العلاقة بين أكثر من حقل.

مثال:

إذا كان Request Type = INTERNAL

فيجب أن يكون Destination داخل نفس النظام.

---

## 9.3 Validation Test Cases

يجب أن تغطي الاختبارات جميع السيناريوهات التالية:

| Scenario | Expected Result |
|----------|-----------------|
| Null Field | ValidationException |
| Blank String | ValidationException |
| Invalid UUID | ValidationException |
| Unsupported Enum | ValidationException |
| Payload Too Large | ValidationException |
| Valid Request | Success |

---

## 9.4 Boundary Value Analysis

يجب اختبار الحدود الدنيا والعليا.

مثال:

```text
Maximum Description Length = 255

254 ✔

255 ✔

256 ✖
```

---

## 9.5 Bean Validation

إذا استخدم المشروع Jakarta Bean Validation فيجب اختبار:

- @NotNull
- @NotBlank
- @Positive
- @Size
- @Pattern
- @Email (إن وجدت)

ويجب ألا تقتصر الاختبارات على التأكد من وجود Annotation فقط، بل يجب التحقق من السلوك الناتج عنها.

---

# 10. Exception Testing

## 10.1 Purpose

لا تقتصر جودة النظام على نجاح السيناريوهات الصحيحة (Happy Path)، بل يجب اختبار جميع مسارات الفشل (Failure Paths).

كل Exception يمثل جزءًا من Contract النظام ويجب اختباره بصورة مستقلة.

---

## 10.2 Required Exceptions

تشمل الاستثناءات الأساسية في المشروع:

- ValidationException
- DuplicateRequestException
- UnauthorizedException
- ForbiddenException
- RequestNotFoundException
- IllegalStateException
- RetryLimitExceededException
- InvalidStateTransitionException

---

## 10.3 Exception Assertions

كل اختبار Exception يجب أن يتحقق من:

- نوع الاستثناء.
- الرسالة (عند الحاجة).
- عدم تعديل حالة النظام.
- عدم استدعاء Dependencies غير المتوقعة.

---

## 10.4 Side Effects

إذا رمي Exception فيجب التأكد من:

- عدم حفظ بيانات.
- عدم نشر رسالة RabbitMQ.
- عدم إنشاء Outbox Event.
- عدم تحديث Cache.

---

## 10.5 Failure Path Coverage

يجب أن تكون جميع مسارات الفشل مغطاة بنفس جودة المسارات الناجحة.

عدم اختبار Failure Paths يعتبر نقصًا في التغطية حتى لو كانت نسبة Coverage مرتفعة.

---

# 11. State Transition Testing

## 11.1 Purpose

يعتمد المشروع على دورة حياة (Lifecycle) واضحة لكل Request.

أي انتقال غير قانوني قد يؤدي إلى فساد في حالة النظام أو تكرار تنفيذ الطلب.

---

## 11.2 Valid State Diagram

```text
NEW
 ↓
QUEUED
 ↓
PROCESSING
 ↓
COMPLETED
```

---

## 11.3 Failure Flow

```text
PROCESSING
 ↓
FAILED
 ↓
RETRY
 ↓
PROCESSING
```

---

## 11.4 Dead Letter Flow

```text
FAILED
 ↓
Retry Counter++

 ↓

Retry?

 ↓

Yes → Retry Queue

No → DLQ
```

---

## 11.5 Invalid Transitions

يجب رفض انتقالات مثل:

```text
COMPLETED
↓

PROCESSING
```

أو

```text
FAILED
↓

NEW
```

أو

```text
QUEUED
↓

NEW
```

---

## 11.6 State Testing Checklist

لكل انتقال يجب اختبار:

- الانتقال القانوني.
- الانتقال غير القانوني.
- الحالة النهائية.
- الرسالة الناتجة.
- الاستثناء المناسب.
- عدم تعديل الحالة عند الفشل.

---

# 12. Idempotency Testing

## 12.1 Purpose

تعد Idempotency من أهم خصائص النظام لأنها تمنع تنفيذ نفس العملية أكثر من مرة.

---

## 12.2 Test Scenarios

يجب اختبار:

- أول طلب.
- إعادة نفس الطلب.
- تغيير Client.
- تغيير Payload.
- تغيير Idempotency-Key.
- إعادة الطلب بعد نجاح العملية.
- إعادة الطلب بعد فشل مؤقت.

---

## 12.3 Expected Behaviour

إذا وصل نفس الطلب مرتين:

- لا ينشأ Request جديد.
- لا ينشأ Outbox Event جديد.
- يعاد نفس Request ID.
- يعاد نفس Status.

---

## 12.4 Edge Cases

يجب اختبار:

- اختلاف الحروف.
- اختلاف المسافات.
- UUID غير صالح.
- مفتاح فارغ.
- مفتاح Null.

---

# 13. Retry Decision Testing

## 13.1 Purpose

تحدد Retry Policy ما إذا كانت العملية ستعاد أو ستعتبر فشلًا نهائيًا.

أي خطأ في هذه السياسة قد يؤدي إلى:

- فقدان الطلبات.
- حلقات Retry لا نهائية.
- نقل رسائل سليمة إلى DLQ.

---

## 13.2 Retry Decision Tree

```text
Processing Failed

↓

Retryable?

↓

Yes

↓

Attempts < Max?

↓

Yes

↓

Retry

↓

No

↓

Dead Letter Queue
```

---

## 13.3 Test Cases

يجب اختبار:

- Temporary Failure.
- Permanent Failure.
- Retry Counter.
- Maximum Attempts.
- Retry Delay.
- Exponential Backoff.
- Retry Disabled.

---

## 13.4 Assertions

يجب التحقق من:

- القرار.
- عدد المحاولات.
- الحالة النهائية.
- Queue المستخدمة.

---

# 14. Mapper Testing

## 14.1 Purpose

تضمن Mapper Tests أن جميع عمليات التحويل بين طبقات النظام صحيحة.

---

## 14.2 Mapping Paths

```text
RequestDTO

↓

RequestEntity

↓

Domain Model

↓

ResponseDTO
```

---

## 14.3 Test Coverage

يجب اختبار:

- جميع الحقول.
- القيم Null.
- Optional Fields.
- UUID Mapping.
- Enum Mapping.
- Date Mapping.
- Boolean Fields.
- Collections.

---

## 14.4 Equality

إذا كان التحويل:

DTO → Entity → DTO

فيجب أن تكون البيانات الناتجة مساوية للبيانات الأصلية متى كان ذلك منطقيًا.

---

# 15. Time Testing

## 15.1 Purpose

الاعتماد المباشر على الوقت الحالي يجعل الاختبارات غير مستقرة.

---

## 15.2 Clock Abstraction

يستخدم المشروع:

```java
Clock
```

بدلاً من:

```java
LocalDateTime.now()
```

---

## 15.3 Test Scenarios

- انتهاء المهلة.
- انتهاء TTL.
- Retry Delay.
- Expiration Date.
- CreatedAt.
- UpdatedAt.

---

## 15.4 Fixed Clock

يفضل استخدام Fixed Clock أثناء الاختبارات للحصول على نتائج ثابتة وقابلة للتكرار.

---

# 16. UUID Generation

## 16.1 Purpose

توليد UUID بصورة عشوائية يجعل الاختبارات غير قابلة للتنبؤ.

---

## 16.2 UUID Provider

يعتمد المشروع طبقة مجردة:

```text
UUIDProvider
```

بدلاً من استدعاء:

```java
UUID.randomUUID()
```

---

## 16.3 Test Benefits

يسمح ذلك بـ:

- UUID ثابت.
- نتائج قابلة للتكرار.
- Assertions أسهل.
- Mock بسيط.

---

# 16.4 Test Data Builders

لإنشاء بيانات الاختبار يستخدم المشروع Builders بدلاً من إنشاء الكائنات يدويًا.

مثال:

```java
RequestBuilder
    .aRequest()
    .withPriority(HIGH)
    .withClientId(clientId)
    .build();
```

يساعد ذلك على:

- إزالة التكرار.
- تحسين القراءة.
- تسهيل صيانة الاختبارات.
- تقليل تأثير تغييرات نموذج البيانات على الاختبارات.

---

# 17. Coverage Goals

## 17.1 Purpose

تستخدم مؤشرات تغطية الاختبارات (Test Coverage Metrics) لقياس مدى تغطية الاختبارات للكود المصدري، لكنها لا تعتبر مؤشرًا كافيًا على جودة الاختبارات.

قد يصل المشروع إلى نسبة تغطية مرتفعة مع وجود اختبارات ضعيفة أو عديمة القيمة، لذلك تعتمد HLRMS على مبدأ:

> **High-Quality Tests are more important than High Coverage Numbers.**

---

## 17.2 Coverage Targets

يحدد المشروع الحد الأدنى المقبول لكل طبقة من طبقات النظام.

| Component | Minimum Coverage |
|-----------|-----------------:|
| Business Services | ≥ 90% |
| Domain Services | ≥ 90% |
| Validators | ≥ 95% |
| Retry Logic | ≥ 90% |
| State Machine | ≥ 90% |
| Utility Classes | ≥ 90% |
| Mapper Classes | ≥ 85% |
| Exception Handlers | ≥ 80% |
| Overall Project | ≥ 80% |

هذه القيم ليست هدفًا بحد ذاتها، وإنما تمثل الحد الأدنى المقبول.

---

## 17.3 Branch Coverage

لا يعتمد المشروع على Line Coverage فقط.

يجب أيضًا قياس:

- Branch Coverage
- Decision Coverage
- Condition Coverage

مثال:

```java
if (retryable && attempts < maxRetries)
```

يجب اختبار جميع الحالات:

| retryable | attempts < max | Expected |
|------------|----------------|----------|
| true | true | Retry |
| true | false | DLQ |
| false | true | No Retry |
| false | false | No Retry |

---

## 17.4 Critical Business Rules

بعض أجزاء النظام تعتبر Critical Components ويجب أن تكون تغطيتها شبه كاملة.

تشمل:

- Request Lifecycle
- Idempotency
- Retry Decision
- Authorization Rules
- State Machine
- Outbox Decision Logic

---

## 17.5 Mutation Testing

يوصى باستخدام Mutation Testing مستقبلًا لقياس جودة الاختبارات.

الهدف من Mutation Testing ليس رفع نسبة التغطية وإنما التأكد من قدرة الاختبارات على اكتشاف الأخطاء الحقيقية.

---

# 18. Test Data Builders

## 18.1 Purpose

كتابة كائنات طويلة داخل كل اختبار يؤدي إلى:

- تكرار الكود.
- صعوبة القراءة.
- صعوبة الصيانة.

لذلك يعتمد المشروع نمط:

> Test Data Builder Pattern

---

## 18.2 Example

بدلاً من:

```java
CreateRequestCommand command =
    new CreateRequestCommand(
        UUID.randomUUID(),
        "client-1",
        RequestPriority.HIGH,
        ...
    );
```

يستخدم:

```java
CreateRequestCommand command =
    RequestBuilder
        .aValidRequest()
        .build();
```

---

## 18.3 Builder Customization

يمكن تعديل القيم المطلوبة فقط.

```java
RequestBuilder
    .aValidRequest()
    .withPriority(HIGH)
    .withDescription("Transfer Salary")
    .build();
```

---

## 18.4 Builder Rules

يجب أن:

- ينشئ بيانات صحيحة افتراضيًا.
- يسمح بتعديل أي خاصية.
- لا يعتمد على قاعدة بيانات.
- لا يعتمد على Spring.

---

## 18.5 Reusability

يعاد استخدام Builders في:

- Unit Tests
- Integration Tests
- Load Tests
- Testcontainers

---

# 19. Anti-Patterns

## 19.1 Purpose

هناك ممارسات تجعل الاختبارات غير مستقرة أو صعبة الصيانة.

يجب تجنبها تمامًا.

---

## 19.2 Thread.sleep()

يمنع استخدام:

```java
Thread.sleep(...)
```

لأنه:

- يبطئ الاختبارات.
- يسبب Flaky Tests.
- يعتمد على سرعة الجهاز.

يفضل استخدام:

```java
Awaitility
```

عند الحاجة في اختبارات غير وحدوية.

---

## 19.3 Random Data

يمنع استخدام:

```java
new Random()
```

داخل Unit Tests.

لأن النتائج تختلف بين تشغيل وآخر.

يفضل استخدام بيانات ثابتة.

---

## 19.4 Shared State

كل اختبار يجب أن يبدأ من حالة نظيفة.

لا يسمح بمشاركة:

- Collections
- Static Variables
- Mutable Objects

بين الاختبارات.

---

## 19.5 Order Dependency

لا يجوز أن يعتمد اختبار على تنفيذ اختبار آخر.

يجب أن ينجح كل اختبار سواء تم تشغيله منفردًا أو مع بقية الاختبارات.

---

## 19.6 Real Database

لا يسمح باستخدام PostgreSQL الحقيقي داخل Unit Tests.

---

## 19.7 Real RabbitMQ

لا يسمح بالاتصال الحقيقي بـ RabbitMQ.

---

## 19.8 Real Redis

لا يسمح باستخدام Redis الحقيقي.

---

## 19.9 Multiple Assertions

يجب ألا يتحول الاختبار إلى قائمة طويلة من Assertions غير المرتبطة.

يفضل أن يتحقق كل اختبار من سلوك واحد واضح.

---

## 19.10 Logic Inside Tests

يمنع كتابة منطق معقد داخل الاختبار نفسه.

إذا احتاج الاختبار إلى منطق طويل، فيجب استخراج هذا المنطق إلى Test Builder أو Helper.

---

## 19.11 Copy-Paste Tests

تكرار الاختبارات مع تعديلات بسيطة يعد مؤشرًا على تصميم سيئ.

يفضل استخدام:

- Parameterized Tests
- Builders
- Helper Methods

---

# 20. Continuous Integration Rules

## 20.1 Purpose

تشغل جميع Unit Tests تلقائيًا داخل CI مع كل تغيير على المشروع.

لا يسمح بدمج أي Pull Request إذا لم تجتز جميع الاختبارات.

---

## 20.2 Build Pipeline

يكون ترتيب التنفيذ:

```text
Compile

↓

Static Analysis

↓

Unit Tests

↓

Coverage Report

↓

Quality Gate

↓

Package

↓

Integration Tests
```

---

## 20.3 Build Failure Conditions

يفشل الـ Pipeline إذا تحقق أحد الشروط التالية:

- فشل أي Unit Test.
- فشل Compilation.
- انخفاض التغطية عن الحد الأدنى.
- فشل Quality Gate.
- وجود Critical Bug في Static Analysis.

---

## 20.4 Pull Request Policy

لا يتم دمج أي Pull Request قبل:

- نجاح جميع Unit Tests.
- مراجعة الكود.
- مراجعة التغطية.
- الموافقة على التغييرات.

---

## 20.5 Code Review Expectations

يجب أن يراجع المراجع (Reviewer):

- جودة الاختبارات.
- وضوح الأسماء.
- صحة Assertions.
- تغطية السيناريوهات.
- حالات الفشل.
- Edge Cases.

---

# 21. Practical Examples

## 21.1 Successful Service Test

```java
@Test
void createRequest_validCommand_returnsCreatedRequest() {

    // Arrange

    CreateRequestCommand command =
        RequestBuilder.aValidRequest().build();

    when(repository.save(any()))
        .thenReturn(savedRequest);

    // Act

    Request result = service.create(command);

    // Assert

    assertThat(result).isNotNull();

    assertThat(result.getStatus())
        .isEqualTo(RequestStatus.NEW);

    verify(repository).save(any());
}
```

---

## 21.2 Validation Example

```java
@Test
void createRequest_nullClientId_throwsValidationException() {

    CreateRequestCommand command =
        RequestBuilder.aValidRequest()
            .withClientId(null)
            .build();

    assertThrows(
        ValidationException.class,
        () -> service.create(command)
    );
}
```

---

## 21.3 Idempotency Example

```java
@Test
void duplicateRequest_returnsExistingRequest() {

    when(repository.findByIdempotencyKey(KEY))
        .thenReturn(existingRequest);

    Request result = service.create(command);

    assertThat(result.getId())
        .isEqualTo(existingRequest.getId());

    verify(repository, never())
        .save(any());
}
```

---

## 21.4 Retry Decision Example

```java
@Test
void retryExceeded_movesToDeadLetterQueue() {

    RetryDecision decision =
        retryPolicy.evaluate(MAX_RETRIES);

    assertThat(decision)
        .isEqualTo(RetryDecision.DEAD_LETTER);
}
```

---

## 21.5 State Machine Example

```java
@Test
void processingRequest_canBeCompleted() {

    request.markCompleted();

    assertThat(request.getStatus())
        .isEqualTo(RequestStatus.COMPLETED);
}
```

---

## 21.6 Verify Example

```java
verify(repository, times(1))
    .save(any());

verifyNoMoreInteractions(repository);
```

---

## 21.7 AssertJ Example

```java
assertThat(result)
    .isNotNull()
    .extracting(Request::getPriority)
    .isEqualTo(RequestPriority.HIGH);
```

---

## 21.8 Parameterized Test Example

```java
@ParameterizedTest
@EnumSource(RequestPriority.class)
void acceptsAllPriorities(RequestPriority priority) {

    CreateRequestCommand command =
        RequestBuilder.aValidRequest()
            .withPriority(priority)
            .build();

    Request result = service.create(command);

    assertThat(result).isNotNull();
}
```

---

# 22. Unit Testing Checklist

يجب مراجعة القائمة التالية قبل دمج أي Pull Request.

---

## 22.1 Test Coverage

- [ ] تمت كتابة Unit Tests لجميع Business Rules.
- [ ] تمت تغطية جميع السيناريوهات الأساسية (Happy Path).
- [ ] تمت تغطية جميع السيناريوهات الاستثنائية (Failure Paths).
- [ ] تمت تغطية Edge Cases.
- [ ] تمت تغطية Boundary Values.
- [ ] لا توجد أجزاء منطقية غير مختبرة.

---

## 22.2 Test Quality

- [ ] أسماء الاختبارات واضحة وتعبر عن السلوك المختبر.
- [ ] كل اختبار يختبر مسؤولية واحدة فقط.
- [ ] جميع الاختبارات مستقلة.
- [ ] لا يعتمد أي اختبار على ترتيب التنفيذ.
- [ ] جميع الاختبارات قابلة للتكرار.

---

## 22.3 Dependencies

- [ ] لا يوجد اتصال بقاعدة بيانات حقيقية.
- [ ] لا يوجد اتصال بـ RabbitMQ.
- [ ] لا يوجد اتصال بـ Redis.
- [ ] لا يوجد اتصال بالشبكة.
- [ ] جميع الحدود الخارجية Mocked أو Fake.

---

## 22.4 Assertions

- [ ] جميع Assertions واضحة.
- [ ] لا توجد Assertions غير ضرورية.
- [ ] تم التحقق من النتائج النهائية.
- [ ] تم التحقق من الاستثناءات عند الحاجة.

---

## 22.5 Readability

- [ ] الكود سهل القراءة.
- [ ] لا يوجد تكرار غير ضروري.
- [ ] تم استخدام Test Builders.
- [ ] تم استخدام Arrange–Act–Assert.

---

## 22.6 Maintainability

- [ ] لا توجد Magic Numbers.
- [ ] لا توجد Magic Strings.
- [ ] لا توجد بيانات عشوائية.
- [ ] لا توجد Thread.sleep().
- [ ] جميع الاختبارات قابلة للصيانة.

---

# 23. Definition of Done

لا تعتبر أي وحدة برمجية مكتملة حتى تحقق جميع الشروط التالية.

---

## Functional Requirements

- تم تنفيذ جميع المتطلبات الوظيفية.
- تمت مراجعة Business Logic.
- تمت مراجعة Validation Rules.

---

## Testing

- تمت كتابة Unit Tests.
- نجحت جميع Unit Tests.
- تم اختبار Happy Path.
- تم اختبار Failure Path.
- تم اختبار Boundary Values.
- تم اختبار Edge Cases.

---

## Code Quality

- لا توجد تحذيرات حرجة.
- لا توجد أخطاء Compilation.
- يلتزم الكود بمعايير المشروع.

---

## Coverage

- تحقيق الحد الأدنى من Coverage.
- تغطية جميع Business Rules.
- تغطية State Machine.
- تغطية Retry Logic.
- تغطية Idempotency Logic.

---

## Documentation

- تحديث JavaDoc عند الحاجة.
- تحديث الوثائق إذا تغير السلوك.
- توضيح أي قرار معماري جديد.

---

## Review

- نجاح Code Review.
- نجاح Pull Request Review.
- نجاح Pipeline بالكامل.

---

# 24. Best Practices

## Keep Tests Small

كل اختبار يجب أن يكون قصيرًا وواضحًا.

---

## Test Behavior, Not Implementation

يجب اختبار السلوك الخارجي (Behavior) وليس تفاصيل التنفيذ الداخلية (Implementation Details).

تغيير طريقة التنفيذ دون تغيير السلوك يجب ألا يؤدي إلى فشل الاختبارات.

---

## Prefer State Verification

يفضل التحقق من الحالة النهائية للكائن (State Verification) متى كان ذلك ممكنًا.

ولا يستخدم Interaction Verification إلا عندما يكون جزءًا من متطلبات السلوك.

---

## Avoid Over-Mocking

الإفراط في استخدام Mock يجعل الاختبارات هشة وصعبة الصيانة.

يجب عمل Mock فقط للحدود الخارجية (External Boundaries).

---

## Use Descriptive Names

يجب أن يوضح اسم الاختبار:

- العملية.
- الشرط.
- النتيجة.

مثال:

```text
createRequest_duplicateKey_returnsExistingRequest()
```

---

## One Assertion Theme

يمكن أن يحتوي الاختبار على أكثر من Assertion إذا كانت جميعها تتحقق من نفس السلوك.

أما إذا كانت تتحقق من أكثر من سلوك مختلف، فيجب تقسيم الاختبار.

---

## Prefer Builders

يجب استخدام Test Data Builders بدلاً من إنشاء الكائنات يدويًا داخل كل اختبار.

---

## Keep Tests Independent

كل اختبار يجب أن يعمل بصورة صحيحة حتى لو تم تشغيله منفردًا.

---

# 25. Testing Smells

هناك مؤشرات تدل على أن الاختبار يحتاج إلى إعادة تصميم.

---

## Fragile Tests

اختبارات تفشل بسبب تغييرات غير مرتبطة بالسلوك المختبر.

---

## Slow Tests

اختبارات تستغرق وقتًا طويلًا بسبب اعتمادها على موارد خارجية.

---

## Duplicate Tests

وجود نفس الاختبار مكرر عدة مرات مع اختلافات بسيطة.

يفضل استخدام:

- Parameterized Tests
- Helper Methods
- Test Builders

---

## Large Tests

إذا تجاوز الاختبار عشرات الأسطر وأصبح من الصعب فهمه، فيجب إعادة تنظيمه.

---

## Conditional Logic

وجود تعليمات مثل:

```java
if (...)
```

داخل الاختبار غالبًا يشير إلى تصميم غير مناسب.

---

## Hidden Assertions

يجب أن تكون Assertions واضحة وصريحة.

---

## Multiple Responsibilities

إذا كان الاختبار يتحقق من أكثر من سلوك مستقل، فيجب تقسيمه إلى عدة اختبارات.

---

# 26. Recommended Project Structure

يوصى بتنظيم ملفات الاختبارات بالشكل التالي:

```text
src
└── test
    └── java
        └── com
            └── hlrms
                ├── service
                ├── validator
                ├── mapper
                ├── domain
                ├── retry
                ├── state
                ├── util
                └── support
```

---

داخل مجلد support توضع:

- Test Builders
- Fake Implementations
- Common Assertions
- Test Utilities

---

# 27. Naming Standards

يفضل اعتماد النمط التالي:

```text
<ClassName>Test
```

مثل:

```text
RequestServiceTest
RetryPolicyTest
RequestMapperTest
StateMachineTest
```

أما أسماء الدوال:

```text
method_condition_expectedResult
```

مثل:

```text
createRequest_validCommand_returnsRequest

markCompleted_processingRequest_changesStatus

retryExceeded_movesMessageToDeadLetterQueue
```

---

# 28. References

تعتمد هذه الوثيقة على أفضل الممارسات والمراجع التالية:

### Official Documentation

- JUnit 5 User Guide
- Mockito Documentation
- AssertJ Documentation
- Spring Boot Testing Documentation
- Jakarta Bean Validation Specification

---

### Books

- Effective Java — Joshua Bloch
- Clean Code — Robert C. Martin
- Clean Architecture — Robert C. Martin
- Working Effectively with Legacy Code — Michael Feathers
- Growing Object-Oriented Software Guided by Tests

---

### Articles

- Martin Fowler — Test Pyramid
- Google Testing Blog
- Microsoft Engineering Testing Guidelines

---

### Internal Project Documents

- Functional Requirements
- Non-Functional Requirements
- Security Design
- Architecture Decision Records (ADR)
- Testing Strategy
- Coding Standards
- API Specification

---

# 29. Summary

تمثل Unit Tests الطبقة الأساسية في استراتيجية الاختبارات الخاصة بمشروع **High-Load Request Management System (HLRMS)**.

ويعد الالتزام بالإرشادات الواردة في هذه الوثيقة شرطًا أساسيًا لضمان:

- صحة منطق الأعمال.
- استقرار النظام.
- سهولة إعادة الهيكلة (Refactoring).
- اكتشاف الأخطاء مبكرًا.
- تقليل Regression.
- الحفاظ على جودة الكود مع نمو المشروع.

تعتبر هذه الوثيقة المرجع الرسمي لجميع اختبارات Unit Testing الخاصة بالمشروع، ويجب الالتزام بها من قبل جميع أعضاء فريق التطوير طوال دورة حياة المشروع.

---

# Document Information

| Property | Value |
|----------|-------|
| Document | UNIT-TESTING.md |
| Version | 1.0 |
| Status | Approved |
| Owner | HLRMS Development Team |
| Category | Testing Documentation |
| Last Updated | Testing Phase |