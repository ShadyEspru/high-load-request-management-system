# Architecture Decision Records — HLRMS

يوثق هذا المجلد القرارات المعمارية الأساسية لنظام **High-Load Request Management System (HLRMS)**.

## الهيكل

```text
docs/adr/
├── README.md
├── ADR-TEMPLATE.md
├── ADR-001-use-rabbitmq-as-message-broker.md
├── ADR-002-use-postgresql-as-primary-database.md
├── ADR-003-use-redis-for-cache-and-ephemeral-state.md
├── ADR-004-use-transactional-outbox-pattern.md
└── ADR-005-use-rabbitmq-quorum-queues.md
```

## الحالات

| الحالة | المعنى |
|---|---|
| Proposed | قيد النقاش |
| Accepted | معتمد |
| Deprecated | لم يعد موصى به |
| Superseded | استبدل بقرار أحدث |
| Rejected | تمت دراسته ورفضه |

## الفهرس

| الرقم | القرار | الحالة |
|---|---|---|
| ADR-001 | استخدام RabbitMQ كوسيط رسائل | Accepted |
| ADR-002 | استخدام PostgreSQL كقاعدة البيانات الأساسية | Accepted |
| ADR-003 | استخدام Redis للتخزين المؤقت والحالة المؤقتة | Accepted |
| ADR-004 | استخدام Transactional Outbox Pattern | Accepted |
| ADR-005 | استخدام RabbitMQ Quorum Queues | Accepted |

## قواعد السجل

1. لا يحذف ADR معتمد.
2. أي تغيير جوهري ينشئ ADR جديدًا يشير إلى السابق.
3. اسم الملف يتبع `ADR-NNN-short-kebab-case-title.md`.
4. يجب ربط كل قرار بالتنفيذ والاختبارات والوثائق ذات الصلة.
