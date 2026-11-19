#!/usr/bin/env python3
# تغییر کامل برند: هلو‌یار/هلوشاپ → شمارا | پکیج ir.holooshop.app → ir.shomara.app
import os, re, shutil

ROOT = "/home/z/my-project/android-native/app/src/main"
OLD_JAVA = os.path.join(ROOT, "java/ir/holooshop/app")
NEW_JAVA = os.path.join(ROOT, "java/ir/shomara/app")

# ۱) انتقال پوشهٔ پکیج
os.makedirs(os.path.dirname(NEW_JAVA), exist_ok=True)
if os.path.isdir(OLD_JAVA):
    if os.path.isdir(NEW_JAVA):
        shutil.rmtree(NEW_JAVA)
    shutil.move(OLD_JAVA, NEW_JAVA)
    print("moved package dir ->", NEW_JAVA)

# ۲) جایگزینی متن‌ها — ترتیب مهم است (بلندترین اول)
REPL = [
    ("ir.holooshop.app", "ir.shomara.app"),
    ("هلو‌یار ۵", "شمارا ۵"),
    ("هلو‌یار", "شمارا"),
    ("هلو‌فهم", "هوشیار"),
    ("هولوشاپ", "شمارا"),
    ("هلوشاپ", "شمارا"),
    ("راهنمای هلو", "راهنمای شمارا"),
    ("حسابداری هلو", "حسابداری حرفه‌ای"),
    ("ماژول‌های حسابداری هلو", "ماژول‌های حسابداری"),
    ("ماژول‌های هلو", "ماژول‌ها"),
    ("مثل خود هلو", "مثل نرم‌افزارهای حسابداری حرفه‌ای"),
    ("مثل هلو،", "در شمارا،"),
    ("مثل منوی خود هلو", "منوی کامل ماژول‌ها"),
    ("مثل اپ خود هلو", "مثل اپ‌های حسابداری حرفه‌ای"),
    ("holoo-store.ts", "store.ts"),
    ("holooyar", "shomara"),
    ("holoo-voice", "shomara-voice"),
    ("HolooYar", "Shomara"),
    ("\"holo\"", "\"shm\""),
    ("ir/holooshop/app", "ir/shomara/app"),
]

targets = []
for dirpath, _, files in os.walk(ROOT):
    for f in files:
        if f.endswith((".kt", ".xml")):
            targets.append(os.path.join(dirpath, f))

changed = 0
for p in targets:
    with open(p, "r", encoding="utf-8") as fh:
        s = fh.read()
    orig = s
    for a, b in REPL:
        s = s.replace(a, b)
    if s != orig:
        with open(p, "w", encoding="utf-8") as fh:
            fh.write(s)
        changed += 1
        print("edited:", os.path.relpath(p, ROOT))
print("files changed:", changed)

# ۳) باقی‌مانده‌های «هلو» را گزارش بده تا دستی اصلاح شود
left = []
for p in targets:
    with open(p, "r", encoding="utf-8") as fh:
        for i, line in enumerate(fh, 1):
            if "هلو" in line or "holoo" in line.lower():
                left.append(f"{os.path.relpath(p, ROOT)}:{i}: {line.strip()[:120]}")
print("\n=== REMAINING ===")
for l in left:
    print(l)
