# LocalSync — Secure AI-Powered Cross-Platform Storage Bridge

A secure personal storage bridge allowing laptops and mobile devices to access and stream each other's storage seamlessly over local Wi-Fi and remote networks (Anywhere Access).

---

## 📱 Android App Download

Get the ready-to-install Android client for your mobile device:
- 📲 **[Download BoltShare APK (Google Drive)](https://drive.google.com/file/d/1IW9jBKmvUcv0AczU7Vs7JTRqjc2z4sk5/view?usp=sharing)**

---

## 🌟 Key Features

1. **Persistent Device Pairing**:
   - Pair once using an HMAC-signed QR code.
   - Long-lived 1-Year `deviceToken` allows automatic zero-friction reconnect without re-scanning QR codes.
2. **Virtual Storage / Live Streaming (No Full Export Required)**:
   - Browse drives (`C:\`, `D:\`) and folders (`Documents`, `Downloads`, `Videos`, `Pictures`).
   - HTTP Range (206 Partial Content) streaming allows playing videos and opening media on-the-fly without downloading entire multi-gigabyte files first.
3. **Hybrid Smart Routing (Anywhere Access)**:
   - **LAN Direct Mode**: Connects at full Wi-Fi network speed with zero internet data usage.
   - **Remote Gateway Mode**: When outside on 4G/5G mobile data, falls back securely to remote encrypted tunnel.
4. **Chunked Resumable Transfers**:
   - 10MB chunked uploads with byte-accurate resume if Wi-Fi disconnects.
5. **AI Storage Insights**:
   - SHA-256 duplicate file detection and categorization.
6. **Security & RBAC**:
   - JWT authentication, per-folder read/write permissions, and emergency one-click Kill Switch.

---

## 📁 Project Structure

- `localsync-android/`: Native Android app (Kotlin + Jetpack Compose + Material3 + CameraX).
- `localsync-server/`: Java 21 + Spring Boot 3 + SQLite backend.
- `localsync-ui/`: React + Vite frontend with minimal White & Slate design system.
- `start-localsync.bat`: One-click startup script for Windows.

---

## 🚀 How to Run

### Quick Start (Windows)
Double-click `start-localsync.bat` in the project root.

### Manual Start

**1. Backend (Spring Boot):**
```powershell
cd localsync-server
$env:PATH = "C:\tools\apache-maven-3.9.6\bin;$env:PATH"
mvn spring-boot:run
```
*Backend runs at http://localhost:8080*

**2. Frontend (React + Vite):**
```powershell
cd localsync-ui
npm install
npm run dev
```
*Frontend runs at http://localhost:5173*
