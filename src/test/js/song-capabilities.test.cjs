const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
class Element {
  constructor(){this.value='';this.files=[];this.textContent='';this.innerHTML='';this.disabled=false;this.classList={remove(){}};this.listeners={};}
  addEventListener(name,callback){this.listeners[name]=callback;}
}
const ids=['formats','limit','metadata','title','artist','album','lyricsText','lyricsAction','artworkAction','result','upload','preview','process'];
const elements=Object.fromEntries(ids.map(id=>[id,new Element()]));const inputs=Object.fromEntries(['audio','lyrics','cover'].map(name=>[name,new Element()]));
const capabilities={m4a:{output:true,lyrics:true,artwork:true},ogg:{output:true,lyrics:true,artwork:true},wav:{output:true,lyrics:false,artwork:false},opus:{output:false,lyrics:false,artwork:false}};
const context=vm.createContext({document:{getElementById:id=>elements[id],querySelector:query=>inputs[query.match(/name=([^\]]+)/)[1]]},fetch:async()=>({json:async()=>({formats:['M4A','OGG','WAV'],capabilities,maxAudioMegabytes:'500 MB'})}),console});
(async()=>{
  vm.runInContext(fs.readFileSync('src/main/resources/static/app.js','utf8'),context);await vm.runInContext('loadPolicy()',context);
  assert.equal(inputs.audio.accept,'.m4a,.ogg,.wav');assert.ok(elements.formats.textContent.includes('UNSUPPORTED'));
  inputs.audio.listeners.change({target:{files:[{name:'test.wav'}]}});assert.equal(inputs.lyrics.disabled,true);assert.equal(inputs.cover.disabled,true);
  inputs.audio.listeners.change({target:{files:[{name:'test.m4a'}]}});assert.equal(inputs.lyrics.disabled,false);assert.equal(inputs.cover.disabled,false);
  vm.runInContext('showMetadata({capabilities:{lyrics:false,artwork:false},metadata:{title:"<img src=x onerror=evil()>"}})',context);
  assert.equal(elements.lyricsAction.disabled,true);assert.equal(elements.artworkAction.disabled,true);assert.equal(elements.lyricsAction.value,'KEEP');assert.ok(elements.metadata.innerHTML.includes('&lt;img'));assert.ok(!elements.metadata.innerHTML.includes('<img'));
  vm.runInContext('showMetadata({capabilities:{lyrics:true,artwork:true},metadata:{}})',context);assert.equal(elements.lyricsAction.disabled,false);assert.equal(elements.artworkAction.disabled,false);
  console.log('song capability controls and text rendering passed');
})().catch(error=>{console.error(error);process.exitCode=1;});
