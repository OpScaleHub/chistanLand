#!/bin/bash

# Define paths
TARGET_DIR="app/src/main/assets/vits-persian"
TEMP_DIR="temp_vits"
MODEL_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-fa_IR-amir-medium.tar.bz2"

echo "🚀 Starting Persian VITS setup..."

# 1. Create target directory if it doesn't exist
mkdir -p "$TARGET_DIR"

# 2. Create a temporary workspace
mkdir -p "$TEMP_DIR"
cd "$TEMP_DIR" || exit

# 3. Download the model bundle
echo "📥 Downloading model (Amir Medium)..."
curl -L "$MODEL_URL" -o model_bundle.tar.bz2

# 4. Extract the files
echo "📦 Extracting files..."
tar -xjf model_bundle.tar.bz2 --strip-components=1

# 5. Move specific required files to the assets folder
echo "🚚 Moving files to $TARGET_DIR..."
mv *.onnx "../$TARGET_DIR/model.onnx"
mv *.json "../$TARGET_DIR/config.json"
mv tokens.txt "../$TARGET_DIR/tokens.txt"

# 6. Cleanup
cd ..
rm -rf "$TEMP_DIR"

echo "✅ Success! Files are located in $TARGET_DIR"
ls -lh "$TARGET_DIR"
