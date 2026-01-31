# 📝 ChistanLand Project Roadmap

## ✅ Done (Development)
- [x] **Core Engine**: Smart typing validation with character-by-character feedback.
- [x] **Saga Map (The Journey)**: Main navigation screen based on the "Island Metaphor" with locked/unlocked states.
- [x] **Parental Dashboard**: Generative UI that translates technical data into human narratives for parents.
- [x] **Full Curriculum Data**:
    - [x] Added all 33 Persian alphabet letters (آ تا ی).
    - [x] Added all 10 Persian numbers (۰ تا ۹).
    - [x] Mapped unique words to every character (e.g., آ -> آب، ب -> بابا).
- [x] **Visual Feedback**:
    - [x] Haptic feedback and shake animation on errors.
    - [x] Streak counter and progressive plant growth indicators.
    - [x] Adaptive In-app Keyboard (MonkeyType Junior concept).
- [x] **Leitner Logic**: Implemented Spaced Repetition (10m, 24h, 4d, 7d) in `LearningRepository`.
- [x] **UI Polish**: Fixed hint flickering and added smooth interaction timing for kids.

## 🛠️ To-Do: Manual Asset Integration (CRITICAL)
*These must be added manually to the project folders to make the app functional.*

- [ ] **Audio Files (Phonics)**:
    - [ ] Place 43 MP3 files in `app/src/main/res/raw/`.
    - [ ] Naming convention must match `LearningViewModel`: `audio_a1.mp3` to `audio_a33.mp3` and `audio_n0.mp3` to `audio_n9.mp3`.
    - [ ] Add `pop_sound.mp3` and `error_sound.mp3`.
- [x] **Illustrations**:
    - [x] Source/Create 43 SVG/PNG images for word cards (e.g., picture of a "Rabbit" for letter 'Kh').
    - [x] Place in `app/src/main/res/drawable/` with names matching `img_a1`, etc.
- [x] **Lottie Animations**:
    - [x] `success_fest.json`: For the milestone celebration.
    - [x] `plant_growth.json`: For the progressive growth levels.

## 🚀 To-Do: Advanced Development (Next Phases)
- [ ] **Voice-Over Instructions**:
    - [ ] Add an "Avatar" that speaks instructions (e.g., "بزن روی حرف ب").
- [ ] **Offline-First Polish**:
    - [ ] Verify database migration strategies for future content updates.
