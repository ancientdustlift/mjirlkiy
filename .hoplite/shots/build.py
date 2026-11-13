import re, pathlib
src = pathlib.Path('neck-exercises.html').read_text(encoding='utf-8')
out = re.sub(r'/\*STANDALONE\*/.*?/\*END-STANDALONE\*/\n', '', src, flags=re.S)
out = out.replace('/*EMBED\n', '').replace('END-EMBED*/\n', '')
out = re.sub(r'\s*<!--STANDALONE-->.*?<!--END-STANDALONE-->', '', out, flags=re.S)
assert 'STANDALONE' not in out and 'EMBED' not in out
bad = re.findall(r'#[0-9a-fA-F]{3,8}\b|rgba?\(|hsla?\(|\d(?:vh|svh|lvh|dvh)\b', out)
print('rejected tokens:', bad)
dst = pathlib.Path('.hoplite/artifacts/neck-exercises.html')
dst.parent.mkdir(parents=True, exist_ok=True)
dst.write_text(out, encoding='utf-8')
print(len(out), 'bytes')
