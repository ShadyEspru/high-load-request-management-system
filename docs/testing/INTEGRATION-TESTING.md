# INTEGRATION-TESTING.md

# Integration Testing Guidelines

**Project:** High-Load Request Management System (HLRMS)

**Version:** 1.0

**Status:** Accepted

---

# 1. Purpose

## 1.1 Overview

يحدد هذا المستند المعايير الرسمية لتصميم وتنفيذ اختبارات **Integration Testing** في مشروع **High-Load Request Management System (HLRMS)**.

تهدف اختبارات التكامل إلى التحقق من أن مكونات النظام المختلفة تعمل معًا بصورة صحيحة بعد دمجها، وأن عمليات الاتصال بين الطبقات والخدمات والبنية التحتية تتم كما هو متوقع.

بعكس Unit Tests التي تختبر كل مكون بصورة معزولة، فإن Integration Tests تتحقق من التعاون الحقيقي بين عدة مكونات داخل بيئة قريبة جدًا من بيئة الإنتاج (Production-like Environment).

---

## 1.2 Objectives

تهدف اختبارات التكامل إلى تحقيق ما يلي:

- التحقق من صحة الاتصال بين طبقات النظام.
- اختبار التكامل مع PostgreSQL.
- اختبار التكامل مع RabbitMQ.
- اختبار التكامل مع Redis.
- التحقق من Transaction Management.
- اختبار Transactional Outbox Pattern.
- اختبار Event Publishing.
- اختبار Worker Processing.
- التحقق من سلامة عمليات Persistence.
- اكتشاف مشاكل Configuration مبكرًا.
- تقليل أخطاء التكامل قبل الوصول إلى مرحلة النظام الكامل.

---

## 1.3 Why Integration Testing Matters

في الأنظمة الموزعة (Distributed Systems)، معظم الأخطاء لا تظهر داخل منطق الأعمال نفسه، وإنما تظهر أثناء التواصل بين المكونات المختلفة.

على سبيل المثال:

- Repository يعمل بصورة صحيحة، ولكن Transaction لا يتم حفظها.
- يتم حفظ البيانات في قاعدة البيانات، ولكن لا يتم نشر الحدث إلى RabbitMQ.
- يتم نشر الرسالة، لكن Worker لا يستطيع معالجتها.
- يتم تحديث Redis ولكن لا يتم تحديث قاعدة البيانات.
- يتم تنفيذ Commit بينما يفشل Outbox Publisher.

لذلك تعتبر Integration Tests من أهم وسائل ضمان موثوقية النظام.

---

# 2. Scope

## 2.1 Included Components

تشمل اختبارات التكامل جميع المكونات التي تتفاعل مع بعضها داخل النظام.

ومنها:

- Spring Boot Context
- REST Controllers
- Service Layer
- Repository Layer
- PostgreSQL
- RabbitMQ
- Redis
- Transaction Manager
- Transactional Outbox
- Message Publisher
- Message Consumer
- Worker Services
- Security Configuration
- Bean Validation
- Database Migrations
- Cache Layer

---

## 2.2 Excluded Components

لا تعتبر العناصر التالية جزءًا من Integration Testing:

- Browser UI
- Android Application
- Frontend Components
- Performance Testing
- Load Testing
- Stress Testing
- Chaos Testing
- End-to-End Business Workflow

يتم اختبار هذه العناصر ضمن وثائق مستقلة.

---

## 2.3 Integration Boundaries

تختبر Integration Tests الحدود التالية:

```text
Controller
      │
      ▼
Service Layer
      │
      ▼
Repository
      │
      ▼
PostgreSQL
```

وكذلك:

```text
Service

↓

Transactional Outbox

↓

RabbitMQ

↓

Worker

↓

Database
```

كما يتم اختبار:

```text
Service

↓

Redis Cache

↓

Database
```

---

# 3. Position in Test Pyramid

يعتمد المشروع نموذج Test Pyramid.

```text
                 End-to-End Tests
                       ▲
               Integration Tests
                       ▲
                  Unit Tests
```

---

## 3.1 Unit Tests

تتحقق من منطق الأعمال فقط باستخدام Mock Objects.

لا تعتمد على قاعدة بيانات أو Broker.

---

## 3.2 Integration Tests

تستخدم المكونات الحقيقية للنظام مثل:

- PostgreSQL
- RabbitMQ
- Redis

ولكن داخل بيئة اختبار معزولة.

---

## 3.3 End-to-End Tests

تختبر النظام بالكامل من خلال واجهات الاستخدام الخارجية.

---

## 3.4 Testing Strategy

يعتمد المشروع على المبدأ التالي:

- عدد Unit Tests هو الأكبر.
- عدد Integration Tests متوسط.
- عدد End-to-End Tests هو الأقل.

وذلك لضمان سرعة التنفيذ مع الحفاظ على جودة التحقق.

---

# 4. Test Environment

## 4.1 Overview

يتم تشغيل جميع اختبارات التكامل داخل بيئة مستقلة تمامًا عن بيئة التطوير والإنتاج.

يجب أن تكون البيئة:

- قابلة لإعادة الإنشاء.
- معزولة.
- قابلة للتكرار.
- آلية بالكامل.

---

## 4.2 Required Components

تتكون بيئة الاختبار من:

- Java 21
- Spring Boot
- JUnit 5
- Testcontainers
- PostgreSQL Container
- RabbitMQ Container
- Redis Container
- Docker Engine

---

## 4.3 Spring Profile

تعتمد جميع اختبارات التكامل على Profile مستقل.

```text
application-test.yml
```

ولا يسمح باستخدام:

```text
application.yml
```

أثناء تنفيذ Integration Tests.

---

## 4.4 Test Configuration

تستخدم الاختبارات إعدادات مستقلة مثل:

- Database URL
- RabbitMQ Port
- Redis Port
- Logging Level
- Flyway Configuration

ويتم تحميلها تلقائيًا بواسطة Spring Boot.

---

# 5. Infrastructure

## 5.1 Infrastructure Components

تعمل اختبارات التكامل مع البنية التحتية الحقيقية داخل حاويات Docker.

تشمل:

- PostgreSQL
- RabbitMQ
- Redis

ويتم تشغيلها تلقائيًا قبل بدء الاختبارات.

---

## 5.2 Isolation

لكل عملية تنفيذ (Test Run):

- قاعدة بيانات مستقلة.
- Queue مستقلة.
- Cache مستقلة.

ولا يتم مشاركة البيانات بين الاختبارات.

---

## 5.3 Repeatability

يجب أن تكون نتائج الاختبارات متطابقة عند تشغيلها:

- محليًا.
- داخل CI.
- على أي جهاز.

---

## 5.4 Cleanup Strategy

بعد انتهاء الاختبارات:

- حذف البيانات المؤقتة.
- إزالة الحاويات.
- تنظيف الاتصالات المفتوحة.
- إعادة الحالة الابتدائية.

---

# 6. Testcontainers

## 6.1 Purpose

يعتمد المشروع على مكتبة **Testcontainers** لتوفير بنية تحتية حقيقية داخل Docker أثناء تنفيذ اختبارات التكامل.

يوفر ذلك بيئة قريبة جدًا من الإنتاج مع الحفاظ على استقلالية الاختبارات.

---

## 6.2 Supported Containers

يستخدم المشروع الحاويات التالية:

- PostgreSQLContainer
- RabbitMQContainer
- GenericContainer (Redis)
- Network

---

## 6.3 Lifecycle

يتم إنشاء الحاويات قبل بدء الاختبارات، ثم إيقافها بعد الانتهاء.

ويجب ألا تعتمد أي اختبارات على حاويات تم إنشاؤها في تشغيل سابق.

---

## 6.4 Dynamic Configuration

تستخدم خاصية Dynamic Property Registration لتمرير بيانات الاتصال بالحاويات إلى Spring Boot أثناء التشغيل.

وبذلك لا يتم تثبيت Ports أو عناوين الاتصال داخل ملفات الإعدادات.

---

# 7. Spring Boot Integration Testing

## 7.1 Spring Context

يجب تشغيل التطبيق داخل Spring Context الحقيقي.

لذلك تستخدم الاختبارات:

```java
@SpringBootTest
```

بدلاً من تحميل Beans بصورة يدوية.

---

## 7.2 Active Profile

يجب تفعيل:

```java
@ActiveProfiles("test")
```

لضمان استخدام إعدادات الاختبار فقط.

---

## 7.3 Test Configuration

يمكن استخدام:

```java
@TestConfiguration
```

لتوفير Beans خاصة بالاختبارات دون التأثير على إعدادات الإنتاج.

---

## 7.4 Test Properties

عند الحاجة يمكن استخدام:

```java
@TestPropertySource
```

لتعديل خصائص محددة داخل الاختبار.

---

## 7.5 Dependency Injection

تعتمد الاختبارات على Dependency Injection الحقيقي الذي يوفره Spring Boot.

ولا يتم إنشاء الكائنات يدويًا إلا عند وجود سبب واضح.

---

# 8. Database Integration Testing

## 8.1 Purpose

تهدف اختبارات قاعدة البيانات إلى التأكد من أن جميع عمليات التخزين والاسترجاع تعمل بصورة صحيحة باستخدام PostgreSQL الحقيقي.

---

## 8.2 CRUD Operations

يجب اختبار:

- Create
- Read
- Update
- Delete

لكل Repository داخل النظام.

---

## 8.3 Transactions

يجب التحقق من:

- Commit
- Rollback
- Atomicity
- Consistency

وأن جميع العمليات تتم داخل Transaction صحيحة.

---

## 8.4 Constraints

يجب اختبار:

- Primary Keys
- Foreign Keys
- Unique Constraints
- Check Constraints
- Not Null Constraints

---

## 8.5 Database Migrations

يجب التحقق من نجاح تنفيذ Flyway Migrations قبل بدء أي اختبار.

ويعتبر فشل Migration سببًا مباشرًا لفشل جميع اختبارات التكامل.

---

## 8.6 Persistence Verification

بعد تنفيذ أي عملية يجب التحقق من أن البيانات مخزنة فعليًا داخل قاعدة البيانات، وليس فقط داخل ذاكرة التطبيق.

---

# 9. Repository Testing

## 9.1 Purpose

تعد طبقة Repository المسؤولة عن التفاعل المباشر مع قاعدة البيانات، لذلك يجب التأكد من أن جميع عمليات القراءة والكتابة تعمل بصورة صحيحة باستخدام PostgreSQL الحقيقي.

لا تستخدم Mock Objects في Repository Integration Tests.

---

## 9.2 Repository Scope

تشمل اختبارات Repository جميع العمليات التالية:

- Insert
- Update
- Delete
- Select
- Pagination
- Sorting
- Filtering
- Custom Queries
- Specifications
- Native Queries
- JPQL Queries

---

## 9.3 CRUD Verification

لكل Repository يجب اختبار:

### Create

التأكد من:

- نجاح الحفظ.
- إنشاء Primary Key.
- حفظ جميع الحقول.

---

### Read

التحقق من:

- البحث بواسطة ID.
- البحث بواسطة Business Key.
- البحث بواسطة UUID.
- البحث بواسطة الحالة.
- البحث بواسطة التاريخ.

---

### Update

التحقق من:

- تعديل البيانات.
- عدم إنشاء سجل جديد.
- تحديث Version عند استخدام Optimistic Locking.

---

### Delete

اختبار:

- Soft Delete.
- Hard Delete.
- Cascade Delete.
- عدم حذف البيانات غير المرتبطة.

---

## 9.4 Query Verification

يجب اختبار جميع الاستعلامات المخصصة.

مثل:

```text
findByStatus()

findPendingRequests()

findExpiredRequests()

findByClientId()

findByIdempotencyKey()
```

ويجب التحقق من:

- عدد النتائج.
- ترتيب النتائج.
- صحة البيانات.

---

## 9.5 Database Constraints

يجب اختبار القيود الفعلية لقاعدة البيانات.

مثل:

- Unique Constraint
- Foreign Key
- Check Constraint
- Not Null Constraint

ويجب التأكد من أن النظام يعالج الأخطاء الناتجة بصورة صحيحة.

---

# 10. Transaction Testing

## 10.1 Purpose

تضمن اختبارات Transaction أن جميع العمليات التي يجب أن تنفذ كوحدة واحدة (Atomic Unit) تعمل بصورة صحيحة.

---

## 10.2 Commit Testing

يجب التحقق من أن جميع التغييرات تحفظ عند نجاح العملية.

مثال:

```text
Insert Request

↓

Insert Outbox Event

↓

Commit
```

ويجب التأكد من وجود السجلين داخل قاعدة البيانات.

---

## 10.3 Rollback Testing

إذا فشل جزء من العملية:

```text
Insert Request

↓

Insert Outbox

↓

Failure

↓

Rollback
```

فيجب التأكد من:

- عدم حفظ Request.
- عدم حفظ Outbox Event.
- عدم وجود بيانات جزئية.

---

## 10.4 Transaction Boundaries

يجب اختبار أن جميع العمليات تتم داخل Transaction صحيحة.

ولا يسمح بخروج جزء من العملية خارج نطاق Transaction.

---

## 10.5 Nested Transactions

إذا استخدم المشروع Nested Transactions فيجب اختبار:

- Success.
- Rollback.
- Propagation Rules.

---

# 11. RabbitMQ Integration Testing

## 11.1 Purpose

تهدف اختبارات RabbitMQ إلى التأكد من أن النظام يتفاعل بصورة صحيحة مع Message Broker.

---

## 11.2 Publisher Testing

يجب اختبار:

- إرسال الرسائل.
- Exchange الصحيح.
- Routing Key الصحيح.
- Message Headers.
- Correlation ID.
- Payload Serialization.

---

## 11.3 Consumer Testing

يجب اختبار:

- استقبال الرسائل.
- فك Serialization.
- تنفيذ Business Logic.
- Acknowledgement.
- Negative Acknowledgement.

---

## 11.4 Queue Verification

بعد إرسال الرسالة يجب التحقق من:

- وجود الرسالة داخل Queue.
- إزالة الرسالة بعد المعالجة.
- انتقال الرسالة إلى DLQ عند الفشل.

---

## 11.5 Error Scenarios

اختبار:

- Broker Unavailable.
- Invalid Message.
- Invalid Routing Key.
- Serialization Failure.
- Consumer Failure.

---

# 12. Redis Integration Testing

## 12.1 Purpose

تهدف اختبارات Redis إلى التأكد من أن نظام التخزين المؤقت يعمل بصورة صحيحة.

---

## 12.2 Cache Operations

اختبار:

- Put
- Get
- Delete
- Exists
- TTL
- Expiration

---

## 12.3 Cache Consistency

بعد تعديل البيانات يجب التأكد من:

- تحديث Cache.
- حذف Cache عند الحاجة.
- عدم قراءة بيانات قديمة.

---

## 12.4 Cache Miss

اختبار السيناريو التالي:

```text
Cache Miss

↓

Database Read

↓

Cache Update

↓

Return Result
```

---

## 12.5 Cache Eviction

اختبار:

- انتهاء TTL.
- إزالة البيانات.
- إعادة تحميل البيانات من PostgreSQL.

---

# 13. Transactional Outbox Testing

## 13.1 Purpose

يعتمد المشروع على نمط Transactional Outbox لضمان عدم فقدان الرسائل.

تعتبر هذه الاختبارات من أكثر اختبارات المشروع أهمية.

---

## 13.2 Insert Verification

بعد إنشاء Request يجب التأكد من:

- إنشاء سجل Request.
- إنشاء Outbox Event.
- وجودهما داخل نفس Transaction.

---

## 13.3 Publisher Verification

بعد تشغيل Publisher يجب التحقق من:

- إرسال الرسالة.
- تحديث حالة Outbox.
- عدم إرسال الرسالة مرتين.

---

## 13.4 Retry

إذا فشل Publisher:

يجب اختبار:

- Retry.
- Retry Counter.
- Retry Delay.
- Maximum Attempts.

---

## 13.5 Recovery

بعد عودة RabbitMQ للعمل يجب التأكد من:

- نشر الرسائل المؤجلة.
- تحديث الحالة إلى Published.

---

# 14. Worker Integration Testing

## 14.1 Purpose

التحقق من أن Workers يستطيعون معالجة الرسائل بصورة صحيحة.

---

## 14.2 Processing

اختبار:

- قراءة الرسالة.
- تنفيذ Business Logic.
- تحديث قاعدة البيانات.
- Ack.

---

## 14.3 Failure

اختبار:

- Exception.
- Retry.
- DLQ.
- Logging.

---

## 14.4 Concurrency

اختبار:

- أكثر من Worker.
- أكثر من رسالة.
- عدم حدوث Race Conditions.

---

## 14.5 Idempotency

إذا استلم Worker نفس الرسالة مرتين:

يجب التأكد من:

- عدم تكرار التنفيذ.
- عدم تكرار البيانات.

---

# 15. Event Publishing Testing

## 15.1 Purpose

اختبار جميع الأحداث المنشورة داخل النظام.

---

## 15.2 Event Structure

يجب التحقق من:

- Event Type.
- Correlation ID.
- Timestamp.
- Payload.
- Metadata.

---

## 15.3 Event Delivery

اختبار:

- نجاح الإرسال.
- إعادة الإرسال.
- Duplicate Prevention.
- Ordering (عند الحاجة).

---

## 15.4 Serialization

التأكد من أن الرسالة قابلة للتحويل إلى JSON ثم استعادتها دون فقدان البيانات.

---

# 16. REST API Integration Testing

## 16.1 Purpose

تهدف هذه الاختبارات إلى التحقق من التكامل بين:

Controller

↓

Service

↓

Repository

↓

Database

---

## 16.2 Request Validation

اختبار:

- Valid Request.
- Missing Fields.
- Invalid UUID.
- Invalid Enum.
- Large Payload.

---

## 16.3 Response Verification

التحقق من:

- HTTP Status.
- Response Body.
- Headers.
- Content Type.

---

## 16.4 Security

اختبار:

- Authentication.
- Authorization.
- Invalid JWT.
- Expired JWT.
- Missing Token.

---

## 16.5 End-to-End API Flow

لكل Endpoint يجب التأكد من:

- وصول الطلب.
- تنفيذ Service.
- حفظ البيانات.
- إنشاء Outbox عند الحاجة.
- إعادة Response صحيحة.

ولا يقتصر الاختبار على التحقق من HTTP Status فقط، بل يجب التأكد من تأثير العملية على قاعدة البيانات، والـ Queue، والـ Cache عند الحاجة.

---

# 17. Authentication & Authorization Testing

## 17.1 Purpose

تهدف اختبارات المصادقة (Authentication) والتفويض (Authorization) إلى التأكد من أن جميع واجهات النظام (REST APIs) لا يمكن الوصول إليها إلا من قبل المستخدمين أو الخدمات المصرح لها.

تعتمد HLRMS على JWT Authentication، لذلك يجب اختبار جميع السيناريوهات الأمنية المتعلقة بالرموز (Tokens).

---

## 17.2 Authentication Scenarios

يجب اختبار الحالات التالية:

| Scenario | Expected Result |
|----------|-----------------|
| Valid JWT | 200 OK |
| Missing JWT | 401 Unauthorized |
| Invalid JWT | 401 Unauthorized |
| Expired JWT | 401 Unauthorized |
| Tampered JWT | 401 Unauthorized |
| Unsupported Algorithm | 401 Unauthorized |

---

## 17.3 Authorization Scenarios

بعد نجاح Authentication يجب اختبار الصلاحيات.

مثل:

- User يستطيع إنشاء Request.
- User لا يستطيع حذف بيانات إدارية.
- Admin يستطيع إدارة النظام.
- Worker يستطيع استهلاك الرسائل فقط.

---

## 17.4 Security Headers

يجب التحقق من وجود جميع Security Headers المطلوبة في الاستجابة عند الحاجة.

---

## 17.5 Failure Verification

عند فشل Authentication يجب التأكد من:

- عدم تنفيذ Business Logic.
- عدم حفظ أي بيانات.
- عدم إنشاء Outbox Event.
- عدم إرسال أي رسالة إلى RabbitMQ.

---

# 18. Persistence Verification

## 18.1 Purpose

بعد انتهاء أي عملية ناجحة يجب التأكد من أن البيانات قد تم حفظها فعليًا داخل PostgreSQL.

لا يكفي التحقق من Response API فقط.

---

## 18.2 Verification Rules

بعد كل عملية يجب التحقق من:

- عدد السجلات.
- صحة القيم.
- العلاقات.
- Version.
- Audit Fields.

---

## 18.3 Audit Fields

يجب التأكد من:

- created_at
- updated_at
- created_by
- updated_by

إذا كانت مستخدمة داخل المشروع.

---

## 18.4 Optimistic Locking

إذا استخدم المشروع Versioning فيجب اختبار:

- Successful Update
- Version Conflict
- Concurrent Update

---

# 19. Cache Verification

## 19.1 Purpose

التحقق من أن Redis يعمل بصورة صحيحة مع قاعدة البيانات.

---

## 19.2 Write Through

إذا كان النظام يستخدم Write Through Cache:

```text
Request

↓

Database

↓

Redis
```

فيجب التأكد من تحديث الطرفين.

---

## 19.3 Cache Aside

إذا استخدم Cache Aside:

```text
Cache Miss

↓

Database

↓

Redis

↓

Client
```

فيجب اختبار جميع المراحل.

---

## 19.4 Cache Expiration

اختبار:

- TTL
- Expiration
- Automatic Eviction

---

## 19.5 Cache Consistency

يجب التأكد من أن البيانات الموجودة في Redis مطابقة للبيانات الموجودة في PostgreSQL.

---

# 20. Queue Verification

## 20.1 Purpose

التحقق من أن الرسائل تنتقل داخل RabbitMQ بصورة صحيحة.

---

## 20.2 Message Lifecycle

```text
Producer

↓

Exchange

↓

Queue

↓

Consumer

↓

Ack
```

---

## 20.3 Verification Points

يجب اختبار:

- Exchange.
- Queue.
- Routing Key.
- Headers.
- Payload.
- Correlation ID.

---

## 20.4 Message Ordering

إذا كان ترتيب الرسائل مهمًا فيجب التحقق من عدم كسره.

---

## 20.5 Queue Cleanup

بعد انتهاء الاختبارات يجب تنظيف جميع الرسائل.

---

# 21. Error Handling Testing

## 21.1 Purpose

اختبار كيفية تعامل النظام مع الأخطاء الحقيقية.

---

## 21.2 Failure Types

تشمل:

- Database Failure
- RabbitMQ Failure
- Redis Failure
- Serialization Failure
- Validation Failure
- Transaction Failure

---

## 21.3 Expected Behaviour

عند حدوث أي فشل يجب التأكد من:

- Rollback عند الحاجة.
- عدم فقدان البيانات.
- تسجيل الخطأ.
- عدم تعطل التطبيق.

---

## 21.4 Recovery

بعد زوال سبب الخطأ يجب التأكد من أن النظام يعود للعمل بصورة طبيعية دون تدخل يدوي.

---

# 22. Retry & Recovery Testing

## 22.1 Purpose

التحقق من أن Retry يعمل وفق السياسة المحددة في النظام.

---

## 22.2 Retry Flow

```text
Failure

↓

Retry

↓

Retry

↓

Retry

↓

Success
```

---

## 22.3 Retry Exhausted

```text
Failure

↓

Retry

↓

Retry

↓

Retry

↓

DLQ
```

---

## 22.4 Verification

يجب التحقق من:

- Retry Counter.
- Retry Delay.
- Exponential Backoff (إن وجد).
- الحالة النهائية.

---

## 22.5 Recovery Testing

بعد عودة الخدمة الخارجية يجب اختبار:

- إعادة معالجة الرسائل.
- تحديث حالة البيانات.
- حذف الرسائل من Queue.

---

# 23. Dead Letter Queue Testing

## 23.1 Purpose

تستخدم Dead Letter Queue لمنع فقدان الرسائل التي تعذر معالجتها.

---

## 23.2 Test Scenarios

اختبار:

- رسالة غير صالحة.
- Serialization Error.
- Business Exception.
- Retry Limit Exceeded.

---

## 23.3 Verification

التأكد من:

- انتقال الرسالة إلى DLQ.
- عدم حذف الرسالة.
- تسجيل سبب الفشل.
- إمكانية إعادة معالجتها لاحقًا.

---

## 23.4 DLQ Recovery

إذا أعيدت الرسالة من DLQ فيجب التأكد من:

- نجاح المعالجة.
- إزالة الرسالة من DLQ.
- تحديث قاعدة البيانات.

---

# 24. Test Data Management

## 24.1 Purpose

تعتمد جودة اختبارات التكامل بصورة كبيرة على جودة بيانات الاختبار.

---

## 24.2 Principles

يجب أن تكون بيانات الاختبار:

- مستقلة.
- قابلة للتكرار.
- واضحة.
- صغيرة قدر الإمكان.
- قريبة من البيانات الحقيقية.

---

## 24.3 Test Data Builders

يفضل استخدام Test Builders لإنشاء البيانات بدلاً من كتابة SQL يدويًا.

---

## 24.4 Database Initialization

يمكن استخدام:

- Flyway
- SQL Scripts
- Java Builders

لتهيئة البيانات قبل الاختبارات.

---

## 24.5 Cleanup Strategy

بعد كل اختبار يجب:

- حذف البيانات المؤقتة.
- إعادة قاعدة البيانات إلى حالتها الابتدائية.
- تنظيف Redis.
- تنظيف RabbitMQ.

ويجب ألا يعتمد أي اختبار على بيانات أنشأها اختبار سابق.

---

# 25. Test Isolation

## 25.1 Purpose

تعد استقلالية الاختبارات (Test Isolation) من أهم مبادئ Integration Testing.

يجب أن يكون كل اختبار قادرًا على التنفيذ بصورة مستقلة دون الاعتماد على نتائج أو بيانات اختبارات أخرى.

أي اعتماد بين الاختبارات يؤدي إلى ظهور اختبارات غير مستقرة (Flaky Tests) يصعب تشخيصها وصيانتها.

---

## 25.2 Isolation Principles

يجب أن تحقق جميع اختبارات التكامل المبادئ التالية:

- كل اختبار يبدأ بحالة نظيفة.
- لا تتم مشاركة البيانات بين الاختبارات.
- لا تعتمد الاختبارات على ترتيب التنفيذ.
- يمكن تشغيل أي اختبار منفردًا.
- يمكن تشغيل جميع الاختبارات بالتوازي عند الحاجة.

---

## 25.3 Database Isolation

يجب تنظيف قاعدة البيانات بعد انتهاء كل اختبار باستخدام إحدى الطرق التالية:

- Transaction Rollback.
- Database Cleanup Scripts.
- إعادة إنشاء Container.
- حذف الجداول المؤقتة.

ويجب اختيار الطريقة التي تحقق أفضل توازن بين السرعة والاستقلالية.

---

## 25.4 Queue Isolation

قبل كل اختبار يجب التأكد من:

- عدم وجود رسائل داخل Queue.
- تنظيف Dead Letter Queue.
- حذف الرسائل المؤقتة.

---

## 25.5 Cache Isolation

يجب تنظيف Redis بالكامل بعد انتهاء كل اختبار.

ولا يسمح بإعادة استخدام بيانات Cache بين الاختبارات.

---

# 26. Performance Considerations

## 26.1 Purpose

على الرغم من أن اختبارات التكامل أبطأ من Unit Tests، إلا أنها يجب أن تظل سريعة بما يكفي لتشغيلها مع كل Pull Request.

---

## 26.2 Performance Goals

يستهدف المشروع المؤشرات التالية:

| Metric | Target |
|--------|-------:|
| Single Integration Test | أقل من 5 ثوانٍ |
| Repository Test | أقل من 2 ثانية |
| API Integration Test | أقل من 5 ثوانٍ |
| Complete Integration Suite | أقل من 10 دقائق |

---

## 26.3 Optimization Techniques

لتقليل زمن التنفيذ:

- إعادة استخدام Testcontainers عند الإمكان.
- تشغيل الاختبارات بالتوازي إذا كانت مستقلة.
- تقليل حجم بيانات الاختبار.
- استخدام Builders بدلاً من SQL الضخم.
- تقليل إنشاء Spring Context أكثر من اللازم.

---

## 26.4 Resource Usage

يجب مراقبة:

- استهلاك الذاكرة.
- عدد الاتصالات المفتوحة.
- عدد الحاويات.
- استهلاك CPU أثناء الاختبارات.

---

# 27. Continuous Integration

## 27.1 Purpose

تشغل جميع Integration Tests تلقائيًا داخل Pipeline بعد نجاح Unit Tests.

ولا يسمح بالانتقال إلى المراحل التالية إذا فشلت أي Integration Test.

---

## 27.2 Pipeline Order

```text
Compile

↓

Static Analysis

↓

Unit Tests

↓

Integration Tests

↓

Coverage Report

↓

Package

↓

Docker Image

↓

Deployment
```

---

## 27.3 Build Failure Conditions

يفشل الـ Pipeline إذا تحقق أحد الشروط التالية:

- فشل أي Integration Test.
- فشل Testcontainers.
- فشل Flyway Migration.
- فشل الاتصال بـ PostgreSQL.
- فشل الاتصال بـ RabbitMQ.
- فشل الاتصال بـ Redis.

---

## 27.4 Pull Request Policy

لا يتم دمج أي Pull Request قبل:

- نجاح Unit Tests.
- نجاح Integration Tests.
- مراجعة الكود.
- مراجعة الوثائق عند الحاجة.

---

# 28. Best Practices

## Test Real Components

يجب اختبار المكونات الحقيقية للنظام، وليس نسخًا وهمية (Mocks)، إلا إذا كان ذلك ضروريًا جدًا.

---

## Keep Tests Independent

كل اختبار مستقل تمامًا.

---

## Test Business Scenarios

ركز على اختبار السيناريوهات الحقيقية وليس تفاصيل التنفيذ الداخلية.

---

## Verify Side Effects

لا يكفي التحقق من HTTP Status.

يجب أيضًا التحقق من:

- البيانات داخل PostgreSQL.
- الرسائل داخل RabbitMQ.
- البيانات داخل Redis.
- حالة Outbox.
- سجلات Worker.

---

## Prefer Builders

استخدم Test Data Builders لإنشاء البيانات بدلاً من كتابة SQL طويل داخل الاختبارات.

---

## Use Production-like Environment

يجب أن تكون بيئة الاختبار قريبة قدر الإمكان من بيئة الإنتاج باستخدام Testcontainers.

---

# 29. Common Anti-Patterns

يجب تجنب الممارسات التالية:

---

## Shared Test Data

إعادة استخدام بيانات أنشأها اختبار سابق.

---

## Hardcoded IDs

استخدام معرفات ثابتة قد تتعارض مع اختبارات أخرى.

---

## Thread.sleep()

يمنع استخدام Thread.sleep داخل اختبارات التكامل.

يفضل استخدام Awaitility عند انتظار أحداث غير متزامنة.

---

## Partial Verification

التحقق من HTTP Response فقط دون التحقق من تأثير العملية على قاعدة البيانات أو الرسائل.

---

## Large Test Methods

إذا أصبح الاختبار طويلًا جدًا أو يحتوي على عدة سيناريوهات مختلفة، فيجب تقسيمه.

---

## Ignoring Cleanup

عدم تنظيف قاعدة البيانات أو Redis أو RabbitMQ بعد انتهاء الاختبارات.

---

# 30. Integration Testing Checklist

قبل دمج أي Pull Request يجب التأكد من:

---

## Infrastructure

- [ ] PostgreSQL يعمل بصورة صحيحة.
- [ ] RabbitMQ يعمل بصورة صحيحة.
- [ ] Redis يعمل بصورة صحيحة.
- [ ] Testcontainers تعمل دون أخطاء.

---

## Database

- [ ] تم اختبار جميع Repositories.
- [ ] تم اختبار جميع Transactions.
- [ ] تم اختبار Constraints.
- [ ] تم اختبار Flyway.

---

## Messaging

- [ ] تم اختبار Publisher.
- [ ] تم اختبار Consumer.
- [ ] تم اختبار Retry.
- [ ] تم اختبار DLQ.
- [ ] تم اختبار Outbox.

---

## API

- [ ] تم اختبار جميع Endpoints.
- [ ] تم اختبار Validation.
- [ ] تم اختبار Authentication.
- [ ] تم اختبار Authorization.
- [ ] تم اختبار Error Responses.

---

## Quality

- [ ] جميع الاختبارات ناجحة.
- [ ] لا توجد Flaky Tests.
- [ ] لا توجد بيانات مشتركة.
- [ ] جميع الاختبارات قابلة للتكرار.

---

# 31. Definition of Done

لا تعتبر أي ميزة (Feature) مكتملة حتى تحقق جميع الشروط التالية:

- تم تنفيذ المتطلبات الوظيفية.
- تمت كتابة Unit Tests.
- تمت كتابة Integration Tests.
- نجحت جميع الاختبارات.
- تم اختبار السيناريوهات الأساسية والاستثنائية.
- تم التحقق من قاعدة البيانات.
- تم التحقق من RabbitMQ.
- تم التحقق من Redis.
- تم التحقق من Transactional Outbox.
- تم التحقق من Retry وDLQ.
- نجحت مراجعة الكود.
- نجحت جميع مراحل CI Pipeline.

---

# 32. References

تعتمد هذه الوثيقة على المراجع التالية:

## Official Documentation

- Spring Boot Testing Documentation
- Testcontainers Documentation
- PostgreSQL Documentation
- RabbitMQ Documentation
- Redis Documentation
- Flyway Documentation
- JUnit 5 Documentation

---

## Books

- Clean Architecture — Robert C. Martin
- Designing Data-Intensive Applications — Martin Kleppmann
- Release It! — Michael T. Nygard
- Building Microservices — Sam Newman

---

## Internal Documents

- Functional Requirements
- Non-Functional Requirements
- Security Design
- Architecture Decision Records (ADR)
- Testing Strategy
- UNIT-TESTING.md
- API Specification
- Deployment Architecture

---

# 33. Summary

تمثل Integration Tests الطبقة الثانية في استراتيجية الاختبارات الخاصة بمشروع **High-Load Request Management System (HLRMS)**.

وتهدف إلى التحقق من أن جميع مكونات النظام تعمل معًا بصورة صحيحة داخل بيئة تشغيل قريبة من الإنتاج، بما يشمل:

- Spring Boot
- PostgreSQL
- RabbitMQ
- Redis
- Transactional Outbox
- Workers
- REST APIs

الالتزام بالإرشادات الواردة في هذه الوثيقة يضمن اكتشاف أخطاء التكامل مبكرًا، ويزيد من موثوقية النظام وقابليته للتوسع والصيانة.

تعد هذه الوثيقة المرجع الرسمي لجميع اختبارات Integration Testing في المشروع.

---

# Document Information

| Property | Value |
|----------|-------|
| Document | INTEGRATION-TESTING.md |
| Version | 1.0 |
| Status | Approved |
| Owner | HLRMS Development Team |
| Category | Testing Documentation |
| Last Updated | Integration Testing Phase |