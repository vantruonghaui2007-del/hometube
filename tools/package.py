"""Package a verified local build; never include the signing key/passwords in source."""
from pathlib import Path
import hashlib, shutil, zipfile, json, xml.etree.ElementTree as ET

root=Path(__file__).resolve().parents[1]
out=root/'outputs';out.mkdir(exist_ok=True)
apk=root/'app/build/outputs/apk/release/app-release.apk'
if not apk.is_file():raise SystemExit('Build release first')
tests=[]
for file in sorted((root/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')):
    s=ET.parse(file).getroot()
    tests.append(dict(name=s.attrib['name'],tests=int(s.attrib['tests']),failures=int(s.attrib['failures']),errors=int(s.attrib['errors']),skipped=int(s.attrib['skipped'])))
if not tests or any(t['failures'] or t['errors'] for t in tests):raise SystemExit('Tests are absent or failing')
lint=ET.parse(root/'app/build/reports/lint-results-debug.xml').getroot()
if any(i.get('severity') in ('Error','Fatal') for i in lint.findall('issue')):raise SystemExit('Lint has errors')
sig=(root/'docs/signature-verification.log').read_text(encoding='utf-8-sig')
if 'Verified using v1 scheme (JAR signing): true' not in sig:raise SystemExit('Verify signature first')
shutil.copy2(apk,out/'HomeTube.apk')
shutil.copy2(root/'docs/INSTALL.md',out/'HUONG-DAN-CAI-DAT.md')
for img in (root/'app/build/outputs/previews').glob('*.png'):
    shutil.copy2(img,out/img.name)
(root/'docs/qa-summary.json').write_text(json.dumps(dict(tests=tests,lint_warnings=len(lint.findall('issue')),v1=True),indent=2,ensure_ascii=False),encoding='utf-8')
excluded={'.git','.gradle','.kotlin','.idea','.superpowers','build','outputs','__pycache__'}
with zipfile.ZipFile(out/'HomeTube-source.zip','w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(root.rglob('*')):
        rel=p.relative_to(root)
        if any(part in excluded for part in rel.parts) or not p.is_file():continue
        if p.name in ('keystore.properties','local.properties') or p.suffix in ('.jks','.keystore','.log'):continue
        z.write(p,Path('HomeTube')/rel)
paths=[out/'HomeTube.apk',out/'HomeTube-source.zip']
(out/'SHA256SUMS.txt').write_text('\n'.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name for p in paths)+'\n',encoding='ascii')
print(json.dumps(dict(tests=sum(t['tests'] for t in tests),skipped=sum(t['skipped'] for t in tests),files=[dict(name=p.name,size=p.stat().st_size) for p in sorted(out.iterdir()) if p.is_file()]),ensure_ascii=False))
