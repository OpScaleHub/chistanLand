#!/bin/bash

# Loop through all files ending in .mp3
for file in *.mp3; do
    # Check if files exist to avoid errors in empty directories
    [ -e "$file" ] || continue
    
    # Extract the filename without the extension
    filename="${file%.*}"
    
    # Run ffmpeg to convert to wav
    ffmpeg -i "$file" "${filename}.wav"
done

echo "Conversion complete!"
