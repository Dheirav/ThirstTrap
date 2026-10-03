#!/usr/bin/env python3
"""Score a render against the plate it is copying, on the things that were
measured off the plate in the first place. Looking at two images side by side
tells you they differ; this tells you which number to move."""
import sys, numpy as np, colorsys
from PIL import Image

def stats(path):
    a = np.asarray(Image.open(path).convert('RGB').resize((836, 470)), dtype=float)/255
    mx, mn = a.max(2), a.min(2)
    S = np.where(mx > 0, (mx-mn)/np.maximum(mx, 1e-6), 0)
    r,g,b = a[...,0],a[...,1],a[...,2]
    gm = (g>r*1.04)&(g>b*1.04)&(mx>0.06)
    return dict(dark=100*(mx<0.18).mean(), lit=100*(mx>0.60).mean(),
                p50=np.percentile(mx,50), p95=np.percentile(mx,95), p99=np.percentile(mx,99),
                sat=S.mean(), sat95=np.percentile(S,95), foliage=100*gm.mean(),
                mean=mx.mean())

ref, got = stats(sys.argv[1]), stats(sys.argv[2])
print(f'{"metric":22}{"ref":>9}{"render":>9}{"delta":>9}')
for k, fmt, tol in (('dark','%.1f%%',4), ('lit','%.1f%%',2), ('p50','%.3f',.03),
                    ('p95','%.3f',.05), ('p99','%.3f',.05), ('mean','%.3f',.03),
                    ('sat','%.3f',.05), ('sat95','%.3f',.08), ('foliage','%.1f%%',4)):
    d = got[k]-ref[k]
    flag = '' if abs(d) <= tol else ('  HIGH' if d > 0 else '  LOW')
    print(f'{k:22}{fmt%ref[k]:>9}{fmt%got[k]:>9}{fmt%d:>9}{flag}')
