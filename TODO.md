# 📝 ChistanLand Project Roadmap

## ✅ Done (Development)
- [x] **Smart Typing Engine**: Implementation of character-by-character validation.
- [x] **Kid-Friendly UI**: Large buttons, RTL support, and high-contrast visuals.
- [x] **Feedback Loops**:
    - [x] Haptic feedback on error.
    - [x] Shake animation on wrong input.
    - [x] Target Glow (Hint system) after 4 seconds of inactivity.
- [x] **Game Mechanics**:
    - [x] Streak counter.
    - [x] Knowledge Plant growth visualizer.
    - [x] Happy Chick status (Emoji-based).
- [x] **Success Overlay**: Lottie-powered festival screen with congratulatory text.

## 🛠️ To-Do: Manual Steps (Assets)
- [ ] **Audio Files**:
    - [ ] Record/Source Phonics sounds for each letter (e.g., `a.mp3`, `b.mp3`).
    - [ ] Place them in `app/src/main/res/raw/`.
    - [ ] Record instructional voice-overs (e.g., "Tap the letter A").
- [ ] **Lottie Animations**:
    - [ ] Add `success_fest.json` to `app/src/main/res/raw/`.
    - [ ] Add animations for the "Knowledge Plant" and "Happy Chick".
- [ ] **Graphics**:
    - [ ] Finalize SVG icons for word cards (replacing star emojis).

## 🚀 To-Do: Development (Next Phases)
- [ ] **Saga Map (The Journey)**:
    - [ ] Create the main navigation screen (Island Metaphor).
    - [ ] Implement parallax scrolling for the map.
- [ ] **Adaptive Learning Logic**:
    - [ ] Connect `LearningViewModel` to a Room Database.
    - [ ] Implement Spaced Repetition (Leitner) timing logic.
- [ ] **Parental Dashboard**:
    - [ ] Create a "Generative UI" to translate raw data into human stories.
    - [ ] Add the "Show me why" audit button for transparency.
- [ ] **Offline-First Resilience**:
    - [ ] Ensure all assets are bundled; zero external dependencies.
