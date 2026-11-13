import glob, sys
from PIL import Image
fs = sorted(glob.glob('.hoplite/shots/s_*.png'), key=lambda f: (int(f.split('s_')[1].split('@')[0]), float(f.split('@')[1][:-4])))
ims = [Image.open(f).crop((16, 16, 544, 400)) for f in fs]
w, h = ims[0].size
n = 8
for part in range((len(ims) + n - 1) // n):
    sub = ims[part * n:part * n + n]
    sheet = Image.new('RGB', (w * 2, h * 4), 'white')
    for i, im in enumerate(sub):
        sheet.paste(im, ((i % 2) * w, (i // 2) * h))
    sheet.save(f'.hoplite/shots/big{part}.png')
print(len(ims), w, h)
