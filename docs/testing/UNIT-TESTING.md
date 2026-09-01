# Unit وComponent Tests

## API Gateway

تغطي الاختبارات `JwtAuthenticationFilter` و`CorrelationIdFilter` و`GatewayLoggingFilter` وJWT validation وGateway routes وFallback responses.

## Auth Service

تغطي التسجيل، البريد المكرر، Login الصحيح والخاطئ، Refresh Token، انتهاء الرموز، Hashing، وController validation.

## Request Service

تغطي ملكية الطلب، RBAC، Idempotency replay/conflict، Atomic request/outbox creation، Repository queries، Admin access، Redis lock/cache/idempotency، وأخطاء HTTP.

## Request Worker

تغطي Event validation، Duplicate event، انتقالات الحالة، معالجة النجاح والفشل، Recoverer بعد Retry، وProcessed Events repository.

## أوامر التشغيل

```bash
./backend/api-gateway/mvnw -f backend/api-gateway/pom.xml test
./backend/auth-service/mvnw -f backend/auth-service/pom.xml test
./backend/request-service/mvnw -f backend/request-service/pom.xml test
./backend/request-worker/mvnw -f backend/request-worker/pom.xml test
```

قد تحتاج آخر خدمتين إلى Dependencies محلية بحسب Test Class؛ راجع `INTEGRATION-TESTING.md`.
