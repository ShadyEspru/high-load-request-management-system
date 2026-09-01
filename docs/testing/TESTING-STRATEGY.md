# استراتيجية الاختبار

## الأهداف

1. صحة القبول والتحقق من الهوية والملكية.
2. Atomicity بين Request وOutbox.
3. Idempotency عند Client retry وMessage redelivery.
4. Recovery بعد توقف Redis أو RabbitMQ أو Worker أو PostgreSQL.
5. قياس Throughput وLatency وBacklog دون خلط تشبع k6 بتشبع النظام.

## مستويات الاختبار

| المستوى | ما يثبته | أداة الدليل |
|---|---|---|
| Unit | منطق فرعي مع Dependencies مقلدة | JUnit، Mockito، AssertJ |
| Component | Controller/Repository ضمن Spring Context | SpringBootTest، MockMvc |
| Integration | قاعدة/Redis/RabbitMQ حقيقية | Profiles محلية وAwaitility |
| E2E | السلسلة الكاملة والحالة النهائية | Docker Compose وAPI/DB checks |
| Performance | السعة والكمون والاستعادة | k6 وGrafana وReconciliation |

## معايير النجاح

- لا تُعد استجابة HTTP ناجحة وحدها دليلًا على اكتمال العمل غير المتزامن.
- كل Accepted Request يجب أن يظهر في Requests وOutbox وProcessed Events بعد Recovery.
- Duplicate Request يعيد Request ID نفسه، وDuplicate Event لا ينفذ مرتين.
- Dropped Iterations تسجل كقيد مولد حمل ما لم يثبت العكس.
- يعلن الاختبار Failed أو Degraded إذا خالف Threshold حتى لو بقيت سلامة البيانات صحيحة.

## بيئات الاختبار

- Developer Unit: لا يعتمد على Docker إلا عند الاختبارات المعلّمة Integration.
- Local Integration: PostgreSQL وRedis وRabbitMQ من Docker Compose.
- Single-Host Performance: مفيد لاكتشاف السقف المحلي، وليس لفصل عنق زجاجة k6.
- Two-Host Final Validation: k6 على Host A وHLRMS على Host B ضمن LAN.
