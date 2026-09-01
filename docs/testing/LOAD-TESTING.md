# Load Testing

توجد سكربتات k6 في `tests/performance/k6/` لسيناريوهات Smoke وBaseline وLoad وStress وSpike وSoak وRecovery.

المرجع المعتمد للنتائج هو:

- [نتائج Benchmark](../performance/BENCHMARK-RESULTS.md)
- [بروتوكول الإثبات](../performance/EVIDENCE-PROTOCOL.md)
- [دليل تشغيل k6](../../tests/performance/k6/README.md)

## قواعد القياس

- يستخدم Base URL عبر Gateway LB، لا Request Service مباشرة، إلا في تشخيص معزول يذكر صراحة.
- يستخدم عنوان LAN عند فصل Host الحمل.
- لا يستخدم ngrok في Performance Test.
- يسجل TEST_RUN_ID وCommit Hash والأمر وHardware وReplicas.
- ينتظر Recovery ثم يجري Database/RabbitMQ reconciliation.
- يفصل بين Target RPS وEffective RPS وHTTP RPS وIterations/s وAccepted Requests.
