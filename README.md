# Descanso 🌙

App Android nativa para **bloquear apps por un tiempo configurable**. Elegís cuánto
querés desconectar y qué apps bloquear (las lee directo de tu teléfono); mientras dura
el temporizador, al abrir una app bloqueada aparece la pantalla de *Descanso*.

## ¿Por qué es nativa y no una web?

Leer las apps instaladas y bloquear otras apps **solo lo puede hacer una app Android
nativa** con permiso de *Accessibility Service*. Un sitio web (GitHub Pages) corre en el
navegador y por seguridad no puede ver ni bloquear otras apps. Por eso Descanso es una
app real (APK) y GitHub Pages se usa solo como página de descarga.

## Descargar / instalar

1. Entrá a la página de descarga (GitHub Pages) o a
   [Releases](https://github.com/fdoti/Descanso/releases/latest) y bajá `app-debug.apk`.
2. Abrí el archivo en el teléfono y permití *instalar apps de orígenes desconocidos*.
3. Abrí **Descanso** y activá el servicio de **Accesibilidad** cuando te lo pida.
4. Elegí el tiempo y las apps a bloquear → **Empezar descanso**.

## Cómo funciona

- **Temporizador**: elegís de 5 a 240 minutos (con presets rápidos).
- **Selector de apps**: lista las apps del teléfono con su ícono; marcás las que querés bloquear.
- **Bloqueo**: un `AccessibilityService` detecta cuándo abrís una app bloqueada y muestra
  la pantalla de descanso hasta que termina el tiempo.
- **Notificación**: un servicio en primer plano muestra la cuenta regresiva.

## Permisos que usa

| Permiso | Para qué |
|---|---|
| Accesibilidad | Detectar la app en primer plano y bloquearla |
| `QUERY_ALL_PACKAGES` | Listar las apps instaladas |
| `POST_NOTIFICATIONS` | Mostrar la cuenta regresiva |
| `FOREGROUND_SERVICE` | Mantener el temporizador activo |

Descanso **no lee ni guarda** el contenido de tus apps.

## Compilar

El APK se compila solo en **GitHub Actions** (JDK 17 + Gradle 8.7) en cada push a `main`,
y se publica en Releases. Para compilar en tu máquina necesitás Android Studio o el SDK
de Android y JDK 17:

```bash
./gradlew assembleDebug
# APK en app/build/outputs/apk/debug/app-debug.apk
```

## Stack

Kotlin · Jetpack Compose (Material 3) · minSdk 26 · targetSdk 34
