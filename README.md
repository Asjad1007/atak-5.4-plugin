# ATAK Plugin Development - Setup Guide

**Developer**: Asjad Nirban  
**Email**: asjad.nirban@gmail.com  
**GitHub**: [https://github.com/Asjad1007/atak-5.4-plugin](https://github.com/Asjad1007/atak-5.4-plugin)

---

## ⚠️ IMPORTANT

**This repository contains ONLY the plugin folders:**

- `feature1-gps-simulator/`
- `feature4-boundary-alert/`

**You must copy these folders into your ATAK SDK 5.4.0.27 `samples/` directory.**

---

## 📋 Requirements

### **Software**

| Software           | Version                                                                               |
| ------------------ | ------------------------------------------------------------------------------------- |
| **Android Studio** | Latest version                                                                        |
| **JDK**            | Oracle OpenJDK 17.0.2<br>Eclipse Temurin 11.0.29<br>JetBrains Runtime 21.0.8 (jbr-21) |
| **ATAK SDK**       | 5.4.0.27                                                                              |
| **ATAK App**       | 5.4.0.27 (from GitHub, NOT app stores)                                                |

### **Android Device/Emulator**

- Android 5.0 (API 21) or higher
- USB debugging enabled (for physical device)

---

## 🚀 Setup Instructions

### **Step 1: Copy Plugin Folders**

Copy these folders into your ATAK SDK:

```
ATAK-CIV-5.4.0.27-SDK/
└── samples/
    ├── feature1-gps-simulator/     ← Copy here
    └── feature4-boundary-alert/    ← Copy here
```

---

### **Step 2: Install ATAK App 5.4.0.27**

⚠️ **Do NOT use ATAK from Google Play Store or Apple App Store** - it's an old version!

**Download**: Get `ATAK-CIV-5.4.0.27.apk` from https://github.com/Asjad1007/atak-5.4-plugin

#### **Install on Physical Device**

```bash
adb install ATAK-CIV-5.4.0.27.apk
```

#### **Install on Emulator**

1. Start your Android Emulator
2. Drag and drop `ATAK-CIV-5.4.0.27.apk` onto the emulator window
3. Wait for installation to complete

---

### **Step 3: Build and Run Plugins**

#### **For Physical Device (USB)**

1. Open Android Studio
2. Open project: `ATAK-CIV-5.4.0.27-SDK/samples/feature1-gps-simulator`
3. Select **Build Variant**: `civDebug` (bottom left corner of Android Studio)
4. Connect your device via USB
5. Click **Run** button (green play icon)
6. Select your device
7. Repeat for `feature4-boundary-alert`

#### **For Emulator**

1. Open Android Studio
2. Open project: `ATAK-CIV-5.4.0.27-SDK/samples/feature1-gps-simulator`
3. Select **Build Variant**: `civDebug` (bottom left corner of Android Studio)
4. Start your Android Emulator
5. Click **Run** button (green play icon)
6. Select your emulator
7. Repeat for `feature4-boundary-alert`
