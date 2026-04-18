<center>

# DocuSpeak 
<img width="1564" height="1534" alt="docuspeak" src="https://github.com/user-attachments/assets/13cf5124-7e88-496e-95d3-5979b88d29a4" />
A premium, distraction-free PDF reading engine for Android featuring High-Fidelity Word Tracking and a Cinematic "Dynamic Island" UI.

## Screenshots
<img width="720" height="1640" alt="darkmode" src="https://github.com/user-attachments/assets/73ca34ca-a4d1-4cf7-bb9e-18ad3df6c8e3" />
<img width="720" height="1640" alt="settings" src="https://github.com/user-attachments/assets/0d8265d9-8616-46dc-8d6e-7929828592be" />
<img width="720" height="1640" alt="readingnow" src="https://github.com/user-attachments/assets/8b32795e-4b56-4e82-9393-6925c4ad3da7" />
<img width="720" height="1640" alt="pdfviewer" src="https://github.com/user-attachments/assets/ca462415-3afe-4c8b-89df-0bced386f3ed" />
<img width="720" height="1640" alt="lighmode" src="https://github.com/user-attachments/assets/a418df71-6fc6-474c-89f5-17c793cd0aec" />


## Features ✨
*   **Geometric Word Tracking**: An indestructible blue highlight that maps the Text-to-Speech audio exactly to the physical PDF coordinates, regardless of the page's scale or dimensions.
*   **Cinematic Immersive Mode**: Seamless edge-to-edge rendering where your PDF flows beautifully behind transparent system/navigation bars.
*   **Glassmorphic "Island" UI**: Premium floating controls that feature high-depth drop shadows and subtle frosted glass aesthetics.
*   **Intelligent Auto-Scroll**: The document automatically scrolls to keep the currently spoken sentence prioritized in your view.
*   **Humanized Voice Selection**: Swap seamlessly between natural-sounding male and female narrators.

## UI Showcase 🎨
DocuSpeak utilizes a completely custom, bottom-heavy control scheme featuring high-fidelity `icons8` icons. The 4-button media cluster (Open, Play, Pause, Stop) prioritizes symmetry and quick access.

## Technical Architecture 🛠️
*   **Language**: Kotlin (Android SDK 34)
*   **PDF Engine**: Custom integration of `PDFBox-Android` allowing single-pass unified extraction of both text content and precise character bounding boxes.
*   **Audio Engine**: `TextToSpeech` with real-time `UtteranceProgressListener` linked directly to UI Coordinate Mapping.
*   **UI Framework**: Material Design 3.

## Author
Maintained by [Hirojonn](https://github.com/Hirojonn).

## License
MIT License. Feel free to fork and build upon this reading engine!

</center>
