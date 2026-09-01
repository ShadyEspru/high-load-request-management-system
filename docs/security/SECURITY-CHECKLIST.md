# قائمة التحقق الأمني

## قبل العرض المحلي

- [ ] تغيير JWT_SECRET وDemo internal secret.
- [ ] عدم إظهار Tokens أو Passwords في Terminal أو Screenshots.
- [ ] تقييد Port 18080 إلى الجهاز المحلي.
- [ ] تشغيل ngrok Tunnel واحد فقط وإيقافه بعد العرض.
- [ ] التأكد أن Demo Data غير حقيقية.

## قبل أي نشر خارجي

- [ ] إزالة كلمات المرور الافتراضية من compose.
- [ ] تعطيل `/api/v1/perf/**` أو حمايته.
- [ ] حماية Actuator وRabbitMQ Management وPrometheus وGrafana.
- [ ] إضافة Rate Limit لمسارات Auth وWrite وفق Capacity.
- [ ] استخدام TLS بين Hosts أو Private Network.
- [ ] ضبط CORS على Origins محددة.
- [ ] فصل DB users وصلاحيات الخدمات.
- [ ] تدوير الأسرار وتخزينها في Secret Manager.
- [ ] تفعيل Backup واختبار Restore.
- [ ] إجراء Dependency scan وContainer image scan.
