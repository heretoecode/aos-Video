"""Create an isolated test package without touching the installed NOVA's providers."""
from pathlib import Path
import sys
root = Path(sys.argv[1]).resolve()
replacements = {
    'com.archos.media.videocommunity': 'org.courville.nova.markpreview.media',
    'com.archos.media.scrapercommunity': 'org.courville.nova.markpreview.scraper',
    'browser.SearchProviderVideocommunity': 'org.courville.nova.markpreview.browser',
}
counts = {key: 0 for key in replacements}
for project in ('Video', 'MediaLib'):
    for path in (root / project).rglob('*'):
        if path.suffix not in ('.java', '.xml') or 'build' in path.relative_to(root / project).parts:
            continue
        original = path.read_text()
        updated = original
        for before, after in replacements.items():
            counts[before] += updated.count(before)
            updated = updated.replace(before, after)
        if updated != original:
            path.write_text(updated)
manifest = root / 'Video/AndroidManifest.xml'
manifest.write_text(manifest.read_text().replace('android:label="@string/nova"', 'android:label="NOVA Preview"'))
if any(count == 0 for count in counts.values()):
    raise SystemExit('Expected provider authority was not found; refusing an incomplete preview conversion')
print('Prepared separate NOVA Preview package and provider authorities:', counts)
