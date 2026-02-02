#!/bin/bash

# Navigate to the raw resources directory
TARGET_DIR="app/src/main/res/raw"
BACKUP_DIR="app/src/main/res/raw_backups"

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

echo "Starting compression..."

for file in "$TARGET_DIR"/*.wav; do
    # Check if files exist to avoid errors in empty dirs
    [ -e "$file" ] || continue

    # Get filename without extension
    filename=$(basename "$file" .wav)

    echo "Processing: $filename"

    # Convert to OGG/Opus
    # -c:a libopus: Use Opus codec
    # -b:a 24k: Set bitrate to 24kbps (great for alphabet sounds)
    # -ar 24000: Keep your original sample rate
    ffmpeg -i "$file" -c:a libopus -b:a 24k -ar 24000 "$TARGET_DIR/$filename.ogg" -loglevel quiet

    # Move the original WAV to backup
    mv "$file" "$BACKUP_DIR/"
done

echo "Done! New .ogg files are in $TARGET_DIR"
echo "Original .wav files moved to $BACKUP_DIR"
