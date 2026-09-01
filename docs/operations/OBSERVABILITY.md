# المراقبة التشغيلية

## المكونات

- Prometheus يجمع المقاييس كل 15 ثانية.
- Grafana تقرأ Prometheus عبر Data Source منشأ تلقائيًا.
- Dashboard يحمل اسم `HLRMS System Overview` وينشأ من JSON داخل المستودع.

المسارات:

```text
monitoring/prometheus/prometheus.yml
monitoring/grafana/dashboards/hlrms-system-overview.json
monitoring/grafana/provisioning/
```

## Scrape Jobs

| Job | المصدر |
|---|---|
| request-service | `request-service-lb:8080/actuator/prometheus` |
| api-gateway | `host.docker.internal:8088/actuator/prometheus` |
| auth-service | `auth-service:8081/actuator/prometheus` |
| request-worker | `request-worker:8082/actuator/prometheus` |
| rabbitmq | `rabbitmq:15692/metrics` |
| postgres | `postgres-exporter:9187` |
| redis | `redis-exporter:9121` |

## مجموعات Dashboard

- System Health وServices UP وRequests Created.
- Gateway RPS وP95 و5xx و429 وStatus Codes.
- JVM Heap/Non-Heap وThreads وOpen Files وGC وCPU.
- Worker Completed/Failed وProcessing Time وCompletion Rate.
- Authentication Success/Failure.
- RabbitMQ Ready/Unacked/Consumers/DLQ/Delivery/Redelivery/Ack.
- PostgreSQL connections وtransactions وsize وdeadlocks وcache hit.
- Redis memory وclients وcache hit وevictions وcommands.
- Resilience4j Circuit Breaker وBulkhead وTimeLimiter.

## حدود القياس الحالية

1. Request Service يجمع عبر HAProxy، لذلك لا يضمن سلسلة زمنية مستقلة لكل Replica.
2. اسم `request-worker` مناسب للنسخة الأساسية؛ عند Scaling يجب تعريف Target لكل Replica أو Service Discovery.
3. لوحة `Services UP` تعد Scrape Targets/Jobs، وليست عدد Replicas.
4. Postgres Exporter متصل حاليًا بقاعدة `hlrms_auth`؛ المقاييس الخاصة بقاعدة requests تحتاج Exporter أو Queries مخصصة إذا أريد الفصل.
5. Scrape interval قدره 15 ثانية قد يفوت تفاصيل Burst قصير مدته ثوانٍ. k6 هو المصدر الأساسي لـClient latency وEffective rate في هذه الحالة.
6. صور Dashboard أثناء Idle لا تثبت قدرة الأداء.

## لقطات التقرير

بدل إدراج عشرات الصور المتشابهة، تحفظ 4–6 لقطات مختارة لكل تشغيل مهم:

1. Overview في بداية الحمل مع TEST_RUN_ID ظاهر خارج Dashboard.
2. RPS وP95 وErrors عند الذروة.
3. RabbitMQ Queue/Consumers وWorker rate.
4. CPU/Memory عند الذروة.
5. نهاية Recovery مع Outbox/Queue صفر.
6. لقطة Failure/Recovery عند اختبار الاعتمادية.

يجب أن تكون Time Range والTimezone واضحتين، وأن تتطابق النافذة مع k6 terminal summary.

## التحسين المقترح قبل اختبار Hostين

- إضافة Prometheus targets مستقلة لكل Request Service وWorker Replica.
- تقليل Scrape interval مؤقتًا إلى 5 ثوانٍ أثناء Benchmark إذا تحملت البيئة الكلفة.
- إضافة Dashboard variable لـTEST_RUN_ID إذا أضيف Label مناسب إلى المقاييس.
- حفظ Dashboard JSON وScreenshots وk6 summary ضمن حزمة الدليل نفسها.
