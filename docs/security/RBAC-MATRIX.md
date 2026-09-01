# مصفوفة الصلاحيات

| الواجهة | Public | USER | ADMIN |
|---|:---:|:---:|:---:|
| `POST /api/v1/auth/register` | ✓ | ✓ | ✓ |
| `POST /api/v1/auth/login` | ✓ | ✓ | ✓ |
| `POST /api/v1/auth/refresh` | ✓ | ✓ | ✓ |
| `POST /api/v1/requests` |  | ✓ | ✓ |
| `GET /api/v1/requests` |  | الطلبات المملوكة فقط | الطلبات المملوكة عبر User endpoint |
| `GET /api/v1/requests/{id}` |  | الطلب المملوك فقط | الطلب المملوك عبر User endpoint |
| `GET /api/v1/admin/requests` |  |  | ✓ |
| `GET /api/v1/admin/requests/{id}` |  |  | ✓ |

المسارات `/api/v1/perf/**` وActuator لها سياسة تطوير خاصة ولا تعد جزءًا من عقد المستخدم النهائي.
