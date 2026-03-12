#!/bin/bash
# Usage: bash encode_keystore.sh release.keystore
# Then copy the output and paste it into your GitHub secret KEYSTORE_BASE64
if [ -z "$1" ] || [ ! -f "$1" ]; then
  echo "Error: keystore file not found: '$1'" >&2
  echo "Usage: bash encode_keystore.sh <path-to-keystore>" >&2
  exit 1
fi
base64 -w 0 "$1"
echo ""
echo "Copy the line above into your GitHub secret KEYSTORE_BASE64"
