# Deck Remote (Android)

لوحة أزرار على الجوال تتصل بتطبيق الويندوز عبر USB.

## كيف يشتغل الربط
التطبيق يتصل بـ `127.0.0.1:8765`. تطبيق الويندوز يشغّل تلقائياً `adb reverse tcp:8765 tcp:8765` فيصير localhost على الجوال = localhost على الكمبيوتر.

## البناء
- من GitHub: ارفع المشروع، Actions → Build Android → حمّل `app-debug.apk`.
- محلياً: افتحه في Android Studio (يولّد الـ Gradle wrapper تلقائياً).

## التثبيت
فعّل "Install unknown apps" ثم ثبّت الـ APK، أو `adb install app-debug.apk`.
