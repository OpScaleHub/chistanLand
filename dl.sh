#!/bin/bash

# Define the target directory
TARGET_DIR="app/src/main/assets/persian_tts"

echo "🚀 Starting TTS Asset Setup..."

# 1. Create the directory structure
if [ ! -d "$TARGET_DIR" ]; then
    echo "📁 Creating directory: $TARGET_DIR"
    mkdir -p "$TARGET_DIR"
else
    echo "✅ Directory already exists."
fi

# 2. Instructions for the user
echo "------------------------------------------------------------"
echo "⚠️  ACTION REQUIRED: Place your downloaded files into:"
echo "    $TARGET_DIR"
echo "------------------------------------------------------------"
echo "Expected files:"
echo "  - model.onnx"
echo "  - tokens.txt"
echo "  - espeak-ng-data/ (The whole folder)"
echo "------------------------------------------------------------"

# 3. Final check (Optional: uncomment if you want to verify after moving files)
# ls -R $TARGET_DIR

echo "💡 Tip: Once files are moved, run: git add . && git commit -m 'Add Persian TTS models'"
