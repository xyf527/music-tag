const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
class Element {
  constructor(tag) { this.tagName = tag; this.children = []; this.textContent = ''; this.value = ''; this.files = []; }
  append(...children) { this.children.push(...children); }
  replaceChildren(...children) { this.children = children; }
  setAttribute(key,value) { this[key] = value; }
  set innerHTML(_) { throw new Error('unsafe HTML assignment'); }
}
const ids = ['items','assets','plan','status','counts','confirm','execute','retry','zip','report','files','directory','upload','restore','taskId','limits'];
const elements = Object.fromEntries(ids.map(id => [id,new Element(id)]));
const danger = '<img src=x onerror=globalThis.injected=true>';
const detail = {task:{id:1,status:'DRAFT',confirmed:false},total:3,kinds:{AUDIO:1,LYRICS:1,COVER:1},counts:{CONFLICT:1},items:[
  {id:1,resourceId:1,kind:'AUDIO',relativePath:danger,title:danger,artist:danger,status:danger,matchBasis:danger,userMessage:danger,lyricsItemId:null,coverItemId:null,candidatesJson:JSON.stringify({lyrics:[2],covers:[3]})},
  {id:2,kind:'LYRICS',relativePath:danger,status:'UNBOUND'},
  {id:3,kind:'COVER',relativePath:danger,status:'UNBOUND'}
]};
const calls = [], context = vm.createContext({
  document:{getElementById:id => elements[id],createElement:tag => new Element(tag),createTextNode:text => ({textContent:text}),querySelectorAll:() => []},
  location:{search:''},history:{replaceState(){}},URLSearchParams,console,
  setTimeout:() => 1,clearTimeout(){},FormData:class { constructor(){this.entries=[];} append(...entry){this.entries.push(entry);} },
  fetch:async (url,options) => {calls.push([url,options]);const payload = url.endsWith('/policy') ? {maxFiles:10,maxBytes:1000,maxAudioBytes:1000,concurrency:2} : url === '/api/batches' ? {id:1} : url.endsWith('/bindings') ? null : detail;return {ok:true,status:200,headers:{get:() => null},text:async()=>payload === null ? '' : JSON.stringify(payload)};}
});
(async () => {
  const script = fs.readFileSync('src/main/resources/static/batch.js','utf8');
  vm.runInContext(script,context);
  vm.runInContext('taskId=1',context);
  await vm.runInContext('refresh()',context);
  const descendants = element => [element,...(element.children || []).flatMap(child => typeof child === 'object' ? descendants(child) : [{textContent:child}])];
  const rendered = descendants(elements.items);
  assert.ok(rendered.some(e => e.textContent === danger));
  assert.ok(descendants(elements.assets).some(e => e.textContent.includes(danger)));
  assert.equal(rendered.filter(e => e.tagName === 'img').length,0);
  assert.equal(context.injected,undefined);
  const selects = rendered.filter(e => e.tagName === 'select');
  assert.equal(selects.length,2); selects[0].value='2'; selects[1].value='3';
  await rendered.find(e => e.tagName === 'button').onclick();
  assert.deepEqual(JSON.parse(calls.find(([url]) => url.endsWith('/bindings'))[1].body),{lyricsItemId:2,coverItemId:3});
  elements.files.files=[{name:'one.mp3',size:1}];elements.directory.files=[{name:'two.flac',webkitRelativePath:'album/two.flac',size:1}];
  await elements.upload.onclick();
  const upload = calls.find(([url]) => url === '/api/batches')[1].body.entries;
  assert.deepEqual(upload.filter(e => e[0]==='paths').map(e => e[1]),['one.mp3','album/two.flac']);
  assert.ok(elements.limits.textContent.includes('10'));
  assert.ok(!script.includes('innerHTML'));
  console.log('batch page safety passed');
})().catch(error => {console.error(error);process.exitCode=1;});
