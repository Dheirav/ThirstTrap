import zipfile, re, xml.etree.ElementTree as ET
NS={'m':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
def load(path):
    z=zipfile.ZipFile(path)
    names=z.namelist()
    shared=[]
    if 'xl/sharedStrings.xml' in names:
        r=ET.fromstring(z.read('xl/sharedStrings.xml'))
        for si in r.findall('m:si',NS):
            shared.append(''.join(t.text or '' for t in si.iter('{%s}t'%NS['m'])))
    sheets=[n for n in names if re.match(r'xl/worksheets/sheet\d+\.xml$',n)]
    out={}
    for sn in sorted(sheets):
        root=ET.fromstring(z.read(sn))
        rows=[]
        for row in root.iter('{%s}row'%NS['m']):
            cells={}
            for c in row.findall('m:c',NS):
                ref=c.get('r'); col=re.match(r'([A-Z]+)',ref).group(1)
                t=c.get('t'); v=c.find('m:v',NS); isel=c.find('m:is',NS)
                if t=='inlineStr' and isel is not None:
                    val=''.join(x.text or '' for x in isel.iter('{%s}t'%NS['m']))
                elif v is None: val=''
                elif t=='s': val=shared[int(v.text)]
                else: val=v.text or ''
                cells[col]=val.strip()
            rows.append(cells)
        out[sn]=rows
    return out
if __name__=='__main__':
    import sys,json
    d=load('plants.xlsx')
    for sn,rows in d.items():
        print('###',sn,len(rows),'rows')
        for r in rows[:3]:
            print(json.dumps(r,ensure_ascii=False)[:2000])
