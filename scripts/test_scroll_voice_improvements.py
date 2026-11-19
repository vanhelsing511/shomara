#!/usr/bin/env python3
from pathlib import Path
root = Path(__file__).resolve().parent.parent / 'android-native/app/src/main/java/ir/shomara/app'
checks = []
def check(name, condition):
    checks.append((name, bool(condition)))

# Screen scroll-tail regression: major screen roots no longer carry large 30dp/24dp bottom pads.
screens = ['ScreenAssistantReconcileProjects.kt','ScreenHome.kt','ScreenInvoices.kt','ScreenItemsWarehouse.kt','ScreenLedgerReports.kt','ScreenPartnersCashChecks.kt','ScreenToolsGuide.kt']
text = '\n'.join((root/f).read_text() for f in screens)
check('screen root bottom padding trimmed from 30dp', 'UI.dp(c, 30)' not in text)
check('home bottom padding trimmed from 24dp', 'UI.dp(c, 24)' not in text)
check('minimum comfortable 10dp content tail remains', text.count('UI.dp(c, 10)') >= 10)

voice = (root/'Voice.kt').read_text()
brain = (root/'VoiceBrain.kt').read_text()
check('hold-to-talk waits for delayed final callback', 'finish() }, 1400)' in voice)
check('final speech candidates use confidence scores when provided', 'SpeechRecognizer.CONFIDENCE_SCORES' in voice and 'maxByOrNull { scores[it] }' in voice)
check('partial transcript retained as best transcript', 'best = p\n            onUpdate?.invoke(p)' in voice)
check('mic released still finalizes fallback transcript', 'val text = best\n        handler.post { onFinal?.invoke(text) }' in voice)
check('voice navigation includes carry-me and enter commands', '"ببر", "وارد"' in brain)
check('voice navigation routes to all module names from route table', 'navByTable(t)' in brain and 'val ROUTES = listOf' in brain)
check('open command supports opening destinations', 'seqIn(t, "باز", "کن")' in brain)
check('voice-sheet final results fallback to partial transcript', 'ifEmpty { partialBest.trim() }' in voice)
check('voice-sheet no-match fallback executes captured partial text', 'handleText?.invoke(fallbackText)' in voice)

for name, ok in checks:
    print(('PASS' if ok else 'FAIL') + ' — ' + name)
failed = [n for n, ok in checks if not ok]
print(f'{len(checks)-len(failed)}/{len(checks)} checks passed')
if failed:
    raise SystemExit(1)
