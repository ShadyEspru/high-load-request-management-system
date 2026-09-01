# مصفوفة تتبع مختصرة

| المتطلب | الاختبار/الدليل |
|---|---|
| FR-001 Auth | `AuthenticationServiceTest`, `AuthControllerIntegrationTest` |
| FR-002 JWT trusted headers | `JwtAuthenticationFilterTest`, `CurrentUserProviderTest` |
| FR-003/004 Create request | `RequestControllerIntegrationTest` |
| FR-005/006 Idempotency | `RequestServiceImplIdempotencyTest`, Redis integration tests، P19-R10 |
| FR-007 Atomic Outbox | `RequestCreationTransactionServiceTest`, `OutboxEventRepositoryTest` |
| FR-009 User ownership | Controller/Repository tests |
| FR-011 Admin RBAC | `AdminRequestControllerIntegrationTest`, `RoleAuthorizationServiceTest` |
| FR-012/013 Publishing | Outbox repository/service tests وRecovery runs |
| FR-014 Worker consume | `RequestEventConsumerTest`, `RequestWorkerIntegrationTest` |
| FR-015 Consumer idempotency | `IdempotentRequestProcessingServiceTest`, `ProcessedEventRepositoryTest` |
| FR-016 Lifecycle | `RequestStatusTransactionServiceTest` |
| FR-017 Retry/DLQ | `RequestFailureMessageRecovererTest` وفشل Worker التجريبي |
| NFR-001 No accepted loss | Database + Outbox + Processed reconciliation |
| NFR-006 Performance metrics | k6 summary + Grafana + Docker stats |
| NFR-010 Worker scaling | تشغيل 1→2→4→8 مع 30001 processed |

المصفوفة تربط المتطلبات بالدليل الموجود؛ لا تستبدل نتيجة تنفيذ الاختبار أو المخرجات الخام.
