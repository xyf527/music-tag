const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
class Element {
  constructor(){this.value='KEEP';this.files=[];this.textContent='';this.innerHTML='';this.disabled=false;this.classList={remove(){},add(){}};this.listeners={};}
  addEventListener(name,callback){this.listeners[name]=callback;}
  showModal(){this.open=true;}
  close(){this.open=false;}
  removeAttribute(name){delete this[name];}
}
const ids=['formats','limit','metadata','title','artist','album','lyricsText','lyricsAction','artworkAction','result','upload','preview','process','titleAction','artistAction','albumAction','previewBox','previewTitle','previewArtist','previewAlbum','previewLyrics','previewFormat','previewCover','previewDialog','closePreview','editAgain','confirmPreview','uploadStatus','attachmentHint','editor','outputCard','download','report'];
const elements=Object.fromEntries(ids.map(id=>[id,new Element()]));const inputs=Object.fromEntries(['audio','lyrics','cover'].map(name=>[name,new Element()]));
const capabilities={m4a:{output:true,lyrics:true,artwork:true},ogg:{output:true,lyrics:true,artwork:true},wav:{output:true,textTags:true,lyrics:true,artwork:true},opus:{output:false,lyrics:false,artwork:false}};
const context=vm.createContext({document:{getElementById:id=>elements[id],querySelector:query=>inputs[query.match(/name=([^\]]+)/)[1]]},fetch:async()=>({json:async()=>({formats:['M4A','OGG','WAV'],capabilities,maxAudioMegabytes:'500 MB'})}),console});
(async()=>{
  vm.runInContext(fs.readFileSync('src/main/resources/static/app.js','utf8'),context);await vm.runInContext('loadPolicy()',context);
  assert.equal(inputs.audio.accept,'.m4a,.ogg,.wav');assert.ok(elements.formats.textContent.includes('UNSUPPORTED'));
  inputs.audio.files=[{name:'test.wav'}];inputs.audio.listeners.change();assert.equal(inputs.lyrics.disabled,false);assert.equal(inputs.cover.disabled,false);
  vm.runInContext('showMetadata({originalFilename:"test.wav",capabilities:{textTags:true,lyrics:true,artwork:true},metadata:{}})',context);assert.ok(elements.attachmentHint.textContent.includes('WAV'));assert.ok(elements.attachmentHint.textContent.includes('歌词支持'));assert.ok(elements.attachmentHint.textContent.includes('封面支持'));
  elements.download.href='/api/versions/1/download';elements.report.href='/api/tasks/1/report';inputs.lyrics.value='previous.lrc';inputs.cover.value='previous.png';
  vm.runInContext('resourceId=1;uploadedCover=true;resourceCapability={lyrics:false,artwork:false}',context);
  inputs.audio.files=[{name:'second.m4a'}];inputs.audio.listeners.change();assert.equal(inputs.lyrics.disabled,false);assert.equal(inputs.cover.disabled,false);
  assert.equal(inputs.lyrics.value,'');assert.equal(inputs.cover.value,'');assert.equal(elements.download.href,undefined);assert.equal(elements.report.href,undefined);assert.equal(vm.runInContext('resourceId',context),null);assert.equal(vm.runInContext('uploadedCover',context),false);
  vm.runInContext('showMetadata({capabilities:{lyrics:false,artwork:false},metadata:{title:"<img src=x onerror=evil()>"}})',context);
  assert.equal(elements.lyricsAction.disabled,true);assert.equal(elements.artworkAction.disabled,true);assert.equal(elements.lyricsAction.value,'KEEP');assert.ok(elements.metadata.innerHTML.includes('&lt;img'));assert.ok(!elements.metadata.innerHTML.includes('<img'));
  vm.runInContext('showMetadata({capabilities:{lyrics:true,artwork:true},metadata:{}})',context);assert.equal(elements.lyricsAction.disabled,false);assert.equal(elements.artworkAction.disabled,false);
  elements.titleAction.value='SET';elements.title.value='<script>unsafe</script>';elements.artworkAction.value='REMOVE';
  vm.runInContext('renderPreview({current:{title:"旧标题",artist:"原歌手",lyrics:"[00:00.00] 第一行\\n[00:01.00] 第二行"},changes:["<img src=x>"],warnings:[]})',context);
  assert.equal(elements.previewTitle.textContent,'<script>unsafe</script>');assert.equal(elements.previewArtist.textContent,'原歌手');
  assert.equal(elements.previewLyrics.textContent,'第一行\n第二行');assert.equal(elements.previewDialog.open,true);
  assert.ok(elements.previewBox.innerHTML.includes('&lt;img'));assert.equal(elements.previewCover.src,undefined);
  elements.editAgain.onclick();assert.equal(elements.previewDialog.open,false);
  assert.equal(vm.runInContext('lyricsExcerpt("[ar:artist]\\n[00:00]a\\n[00:01]b\\n[00:02]c\\n[00:03]d\\n[00:04]e")',context),'a\nb\nc\nd');
  console.log('song capability controls and text rendering passed');
})().catch(error=>{console.error(error);process.exitCode=1;});
