<div align="center">

# DocuSpeak AI

<img src="https://github.com/user-attachments/assets/13cf5124-7e88-496e-95d3-5979b88d29a4" width="300"/>

### The Pure Free AI PDF Reader.
Featuring an iPhone-inspired UI and a zero-config AI architecture.

</div>

---

## 📱 Screenshots

<div align="center">

<img src="https://github.com/user-attachments/assets/73ca34ca-a4d1-4cf7-bb9e-18ad3df6c8e3" width="180"/>
<img src="https://github.com/user-attachments/assets/0d8265d9-8616-46dc-8d6e-7929828592be" width="180"/>
<img src="https://github.com/user-attachments/assets/ca462415-3afe-4c8b-89df-0bced386f3ed" width="180"/>
<img src="https://github.com/user-attachments/assets/a418df71-6fc6-474c-89f5-17c793cd0aec" width="180"/>

</div>
## Core Features ✨
*   **Indestructible Bookmark System**: Save and remove bookmarks with a single tap. The TTS engine intelligently resumes reading from your saved point.
*   **Long-Strip Vertical View**: No more rigid page flipping. Read PDFs like a modern webpage with a continuous, smooth vertical scroll.
*   **Geometric Page Numbering**: Real-time "Page X" indicators baked directly into the document view as you scroll.
*   **Glassmorphic Aesthetic**: Deep translucent backgrounds, subtle borders, and iOS-style micro-animations.

---

## Technical Stack 🛠️
*   **Language**: Kotlin (Android SDK 34)
*   **Physics Engine**: `androidx.dynamicanimation` for elastic, spring-based UI transitions.
*   **AI Engine**: Pollinations AI (Keyless primary) / G4F fallback support.
*   **PDF Engine**: `PDFBox-Android` optimized for vertical long-strip rendering.
*   **UI Framework**: Material Design 3 with custom Glassmorphing.

---

## How to Build Locally 🚀
This repository includes a convenient, one-click PowerShell build script. You must have Java & the Android SDK installed.

1. Clone the repository.
2. Open PowerShell in the project directory.
3. Run the automated build script:
```powershell
.\build_app.ps1
```
4. Your premium APK will be ready at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Author
Maintained by [Hirojonn](https://github.com/Hirojonn).

---

<div align="center">

## 📄 License  

MIT License  
Feel free to fork and build upon this reading engine!

</div>
