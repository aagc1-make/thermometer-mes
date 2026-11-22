"""Archive actual Surefire/JaCoCo outputs; run with the bundled Python runtime."""
import argparse,json,pathlib,shutil,xml.etree.ElementTree as ET,hashlib
p=argparse.ArgumentParser();p.add_argument('root');p.add_argument('stage');a=p.parse_args()
root=pathlib.Path(a.root);base=root/'backend/ktg-mes';out=root/'evidence/lab2'/a.stage;out.mkdir(parents=True,exist_ok=True)
summary={'stage':a.stage,'testSuites':[],'totals':{'tests':0,'failures':0,'errors':0,'skipped':0},'coverage':{}}
for f in sorted(base.glob('*/target/surefire-reports/TEST-*.xml')):
 t=ET.parse(f).getroot();row={'class':t.attrib['name'],'module':f.parents[2].name,**{k:int(t.attrib.get(k,0)) for k in summary['totals']},'seconds':float(t.attrib.get('time',0))}
 summary['testSuites'].append(row)
 for k in summary['totals']:summary['totals'][k]+=row[k]
 dest=out/'surefire';dest.mkdir(exist_ok=True);shutil.copy2(f,dest/f.name)
def counters(e):
 return {c.attrib['type']:{'missed':int(c.attrib['missed']),'covered':int(c.attrib['covered'])} for c in e.findall('counter')}
for f in sorted(base.glob('*/target/site/jacoco/jacoco.xml')):
 report=ET.parse(f).getroot();module=f.parents[3].name
 if a.stage=='after' and module!='ktg-mes':continue
 classes={c.attrib['name']:c for package in report.findall('package') for c in package.findall('class')}
 selected={}
 for name in ['com/ktg/mes/linkage/FormLinkagePolicy','com/ktg/mes/wm/service/impl/IssueAutoPickService','com/ktg/mes/pro/service/FeedbackBatchScopeService']:
  if name in classes:selected[name]=counters(classes[name])
 summary['coverage'][module]={'total':counters(report),'selected':selected,'classCount':len(classes)}
 if selected:
  method=next(m for m in classes['com/ktg/mes/linkage/FormLinkagePolicy'].findall('method') if m.attrib['name']=='assertQualityQuantities')
  summary['basisMethod']={'name':method.attrib['name'],'line':method.attrib['line'],'counters':counters(method)}
  shutil.copytree(f.parent,out/'jacoco',dirs_exist_ok=True)
  execfile=f.parents[2]/'jacoco.exec'
  if execfile.exists():shutil.copy2(execfile,out/'jacoco.exec')
log=root/'.tmp-lab2'/f'{a.stage}.log'
if log.exists():shutil.copy2(log,out/'maven.log')
summary['productionHashes']={str(f.relative_to(root)):hashlib.sha256(f.read_bytes()).hexdigest() for f in [base/'ktg-mes/src/main/java/com/ktg/mes/linkage/FormLinkagePolicy.java',base/'ktg-mes/src/main/java/com/ktg/mes/wm/service/impl/IssueAutoPickService.java',base/'ktg-mes/src/main/java/com/ktg/mes/pro/service/FeedbackBatchScopeService.java']}
(out/'summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps(summary,ensure_ascii=True))
