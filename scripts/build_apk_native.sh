#!/bin/bash
# Shomara (شمارا) native (pure Kotlin, no WebView) — build without Gradle
set -euo pipefail

: "${SHOMARA_KEYSTORE_PATH:?Set SHOMARA_KEYSTORE_PATH to the signing keystore file}"
: "${SHOMARA_KEY_ALIAS:?Set SHOMARA_KEY_ALIAS}"
: "${SHOMARA_KEYSTORE_PASSWORD:?Set SHOMARA_KEYSTORE_PASSWORD}"
: "${SHOMARA_KEY_PASSWORD:?Set SHOMARA_KEY_PASSWORD}"
if [ ! -f "$SHOMARA_KEYSTORE_PATH" ]; then
  echo "Signing keystore not found: $SHOMARA_KEYSTORE_PATH" >&2
  exit 1
fi
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=${SHOMARA_ROOT:-$(cd "$SCRIPT_DIR/.." && pwd)}
APP=$ROOT/android-native/app/src/main
SDK=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$ROOT/.android-sdk}}
BT=$SDK/build-tools/34.0.0
PLAT=$SDK/platforms/android-34/android.jar
KOTLINC=${KOTLINC_PATH:-$ROOT/.kotlinc/bin/kotlinc}
OUT=$ROOT/android-native/build
VER_CODE=48
VER_NAME=16.0
FINAL=${SHOMARA_OUTPUT_APK:-$ROOT/download/Shomara-v${VER_NAME}.apk}

rm -rf "$OUT"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/dex" "$(dirname "$FINAL")"

# 1) resources
$BT/aapt2 compile --dir $APP/res -o $OUT/res.zip

# 2) link (assets optional)
AARGS=""
[ -d $APP/assets ] && AARGS="-A $APP/assets"
$BT/aapt2 link -o $OUT/base.apk -I $PLAT \
  --manifest $APP/AndroidManifest.xml $AARGS \
  --java $OUT/gen --min-sdk-version 21 --target-sdk-version 34 \
  --version-code $VER_CODE --version-name $VER_NAME \
  $OUT/res.zip

# 3) generate R.kt (no javac needed in this sandbox)
$BT/aapt2 dump resources $OUT/base.apk | grep "resource 0x" > $OUT/res.txt
python3 $ROOT/scripts/gen_r_kt.py $OUT/res.txt $OUT/R.kt

# 4) Kotlin
$KOTLINC $APP/java $OUT/R.kt -cp $PLAT -include-runtime -d $OUT/app.jar 2> $OUT/kotlin.log || { tail -80 $OUT/kotlin.log; exit 1; }
grep -E "error:" $OUT/kotlin.log | head -40 || true

# 5) dex
$BT/d8 --release --lib $PLAT --min-api 21 --output $OUT/dex $OUT/app.jar
cp $OUT/dex/classes.dex $OUT/classes.dex
(cd $OUT && zip -q base.apk classes.dex)

# 6) align + sign
$BT/zipalign -f 4 $OUT/base.apk $OUT/aligned.apk
$BT/apksigner sign --ks "$SHOMARA_KEYSTORE_PATH" \
  --ks-pass "env:SHOMARA_KEYSTORE_PASSWORD" --ks-key-alias "$SHOMARA_KEY_ALIAS" \
  --key-pass "env:SHOMARA_KEY_PASSWORD" \
  --out $FINAL $OUT/aligned.apk
$BT/apksigner verify $FINAL && echo SIGN_OK
ls -lh $FINAL
