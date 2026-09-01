# المتطلبات الوظيفية

تعكس المتطلبات التالية السلوك الموجود في الفرع الحالي.

| المعرّف | المتطلب | موضع التنفيذ الرئيس |
|---|---|---|
| `FR-001` | يدعم النظام Register وLogin وRefresh Token | `AuthController` و`AuthenticationService` |
| `FR-002` | يتحقق Gateway من JWT ويولد ترويسات الهوية الموثوقة | `JwtAuthenticationFilter` |
| `FR-003` | يقبل طلبًا عامًا يحوي requestType وpayload مع Idempotency-Key | `RequestController` و`CreateRequestDto` |
| `FR-004` | ينشئ Request ID فريدًا ويعيده إلى العميل | `RequestEntity` و`RequestResponseDto` |
| `FR-005` | يطبق Idempotency ضمن نطاق المستخدم، ويعيد الطلب السابق عند التطابق | `RequestServiceImpl` وRedis services |
| `FR-006` | يرفض إعادة المفتاح نفسه مع محتوى مختلف بـ409 | `IdempotencyConflictException` |
| `FR-007` | يحفظ request وoutbox event داخل Transaction واحدة | `RequestCreationTransactionService` |
| `FR-008` | يعيد 201 للطلب الجديد و200 للReplay مع Header يوضح النتيجة | `RequestController` |
| `FR-009` | يسمح للمستخدم بقراءة طلب يملكه فقط | `findByIdAndUserId` و`CurrentUserProvider` |
| `FR-010` | يسمح للمستخدم بعرض طلباته مع status وpagination | `getAllRequests` |
| `FR-011` | يسمح للمسؤول بقراءة كل الطلبات بعد requireAdmin | `AdminRequestController` و`RoleAuthorizationService` |
| `FR-012` | ينشر Outbox على دفعات مع Publisher Confirms | `OutboxEventProcessor` و`RequestEventPublisher` |
| `FR-013` | يعيد الأحداث المتوقفة بعد Restart ويقيد عدد محاولات النشر | `OutboxEventTransactionService` |
| `FR-014` | يستهلك Worker رسائل REQUEST_CREATED من Queue متينة | `RequestEventConsumer` |
| `FR-015` | يمنع معالجة eventId نفسه مرتين | `IdempotentRequestProcessingService` و`processed_events` |
| `FR-016` | يحدث الطلب وفق PENDING → PROCESSING → COMPLETED/FAILED | `RequestStatusTransactionService` |
| `FR-017` | يعيد Listener المحاولة ثم يوجه الفشل النهائي إلى DLQ | Spring AMQP config و`RequestFailureMessageRecoverer` |
| `FR-018` | يعمل Redis كمسار سريع مع حماية PostgreSQL عند تعذره | Redis services وUnique Constraints |
| `FR-019` | يعرض Health وMetrics بتنسيق Prometheus | Actuator وMicrometer |
| `FR-020` | يدعم تشغيل عدة Worker Replicas | `docker-compose.scaling-base.yml` |
| `FR-021` | يربط Demo Client عبر واجهات HLRMS العامة دون دمجه في قلب النظام | `android-app/` وGateway routes |

## قواعد القبول الأساسية

- لا يقبل POST دون Idempotency-Key أو requestType أو payload.
- لا يتجاوز Idempotency-Key مئة محرف، ولا يتجاوز payload عشرة آلاف محرف.
- لا يستطيع المستخدم العادي قراءة طلب مستخدم آخر.
- لا تعد الاستجابة Accepted كاملة قبل نجاح Transaction التي تضم request وoutbox event.
- لا يعد الحدث معالجًا قبل تسجيله وتحديث الطلب داخل المسار المتين.
- يجب أن يترك الفشل النهائي أثرًا قابلًا للتتبع في الطلب وDLQ.
