<center>

<img width="703" height="301" alt="image" src="https://github.com/user-attachments/assets/ae72e22c-5be9-4e28-b102-d23910011dbe" /># DocuSpeak 
A premium, distraction-free PDF reading engine for Android featuring High-Fidelity Word Tracking and a Cinematic "Dynamic Island" UI.

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
