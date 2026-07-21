# إنشاء المستودع

## الاسم المعتمد
`high-load-request-management-system`

## الوصف
Distributed high-load request management platform using RabbitMQ and parallel workers.

## أوامر الإنشاء المحلية
```bash
git init
git branch -M main
git add .
git commit -m "chore: initialize HLRMS project structure"
git checkout -b develop
```

## إنشاء المستودع على GitHub
1. إنشاء Repository جديد بالاسم المعتمد.
2. جعله Private خلال مرحلة التطوير.
3. عدم إضافة README تلقائيًا لأن الهيكل يحتوي على README.
4. ربطه محليًا:

```bash
git remote add origin <REPOSITORY_URL>
git push -u origin main
git push -u origin develop
```

## حماية الفروع
- منع الدفع المباشر إلى `main`.
- طلب مراجعة واحدة على الأقل.
- منع الدمج عند فشل الاختبارات.
- حذف فرع الميزة بعد الدمج.

## التسميات المقترحة للـIssues
- `architecture`
- `backend`
- `rabbitmq`
- `worker`
- `database`
- `monitoring`
- `android-demo`
- `load-testing`
- `documentation`
- `diagram`
- `bug`
- `priority-high`
