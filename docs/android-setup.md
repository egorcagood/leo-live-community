# Сборка Android

Для использования готового APK Android Studio не нужна. Эта инструкция для сборки из исходников до первого релиза и для разработчиков.

## Инструменты

- JDK 17 или совместимая с Gradle 9.1.0 установленная JDK.
- Android SDK Platform 36 и Android SDK Build-Tools 36.0.0.
- Gradle 9.1.0 через поставляемый wrapper; Android Gradle plugin 9.0.1.
- Python 3 для проверки комплекта исходников; JDK для host-тестов.

Открой каталог **android/** в актуальной Android Studio, укажи SDK и дождись синхронизации. SDK и зависимости при первой сборке скачиваются из репозиториев Google/Maven Central/Gradle. Само собранное приложение не имеет разрешения на интернет.

Если IDE просит путь к SDK, настрой его в IDE или создай локальный `android/local.properties`:

```properties
sdk.dir=/absolute/path/to/your/Android/sdk
```

На Windows используй путь, записанный Android Studio; не копируй чужой `local.properties`. Этот файл не коммитится. В нём не нужны никакие ключи или токены.

## Проверки и сборка

Из корня проекта:

```sh
sh scripts/test-core.sh
python3 -m unittest discover -s tests -p 'test_*.py'
python3 scripts/release-audit.py
cd android
./gradlew :app:assembleDebug :app:lintDebug
```

На Windows Gradle запускается как `gradlew.bat`; host shell-скрипт можно выполнить в Git Bash. APK: `android/app/build/outputs/apk/debug/app-debug.apk`.

Это **debug APK для проверки**, а не официальный подписанный релиз. Для официального APK нужна отдельная release-подпись Community, управляемая сопровождающим. Ключ подписи не включается в repository или ZIP. Ключ частного LEO Live использовать не нужно.

## Установка для разработчика

Разблокируй принадлежащий тебе тестовый телефон, включи USB-отладку, подтверди доверие этому компьютеру. Проверь `adb devices`, затем укажи именно нужное устройство:

```sh
adb -s DEVICE_SERIAL install -r android/app/build/outputs/apk/debug/app-debug.apk
```

Команда выполняется из корня repository; `DEVICE_SERIAL` замени идентификатором своего тестового устройства. Не запускай установку во время движения. USB нужен для установки/отладки; OBD-соединение идёт по BLE.

Пакет: `community.leo.obd`. Приложение не заменяет частный LEO Live. API-ключи и логин не нужны.

## Что доказывают проверки

Host-тесты проверяют кодеки, ограничение команд, очередь, тайм-ауты, журнал и demo. Lint проверяет Android-код. Успешная сборка не доказывает корректность конкретного адаптера и ЭБУ. Устройства, версии Android и реальные сценарии должны отдельно отмечаться в [матрице совместимости](compatibility.md).
