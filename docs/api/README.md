# واجهات HLRMS

العقد العام منشور عبر API Gateway على `http://localhost:8088`.

| المجموعة | المسارات |
|---|---|
| Authentication | `POST /api/v1/auth/register`, `/login`, `/refresh` |
| User Requests | `POST /api/v1/requests`, `GET /api/v1/requests`, `GET /api/v1/requests/{id}` |
| Administration | `GET /api/v1/admin/requests`, `GET /api/v1/admin/requests/{id}` |
| Operations | `/actuator/health`, `/actuator/prometheus` بحسب الخدمة |

لا توجد في التنفيذ الحالي واجهات مستقلة لـ`/status` أو `/history` أو `/admin/workers` أو `/admin/queues` أو تعديل الإعدادات.

- [المواصفات المقروءة](API-SPECIFICATION.md)
- [عقد OpenAPI](openapi.yaml)
- [دليل الأخطاء](ERROR-CATALOG.md)
