'use strict';
const $ = id => document.getElementById(id);
let taskId = new URLSearchParams(location.search).get('task');
let policy, timer;
const labels = {DRAFT:'待确认',CONFIRMED:'计划已确认',RUNNING:'处理中',SUCCESS:'成功',FAILED:'失败',INTERRUPTED:'中断，可重试',SKIPPED:'已跳过',PENDING:'等待处理',CONFLICT:'匹配冲突',COMPLETED:'已完成',COMPLETED_WITH_FAILURES:'部分失败',BOUND:'已绑定',UNBOUND:'未绑定',UNSUPPORTED:'不支持'};
const bases = {RELATIVE_PATH_AND_STEM:'同目录同基名',LRC_TITLE_ARTIST:'LRC 标题和歌手一致',MANUAL:'人工指定',COLLISION:'多候选或重复绑定',UNBOUND:'无唯一匹配'};
const show = message => { $('status').textContent = message; };
function node(tag, text) { const element = document.createElement(tag); if (text !== undefined) element.textContent = text; return element; }
async function request(url, options) {
  const response = await fetch(url, options);
  if (!response.ok) { const error = await response.json().catch(() => ({})); throw new Error(error.message || '请求失败，请重试'); }
  return response.status === 204 || response.headers.get('content-length') === '0' ? null : response.text().then(text => text ? JSON.parse(text) : null);
}
function chooseAsset(items, kind, selected, disabled) {
  const select = node('select'); select.setAttribute('aria-label', kind === 'LYRICS' ? '歌词绑定' : '封面绑定');
  const empty = node('option', '无绑定（解绑）'); empty.value = ''; select.append(empty);
  items.filter(i => i.kind === kind && i.status !== 'FAILED').forEach(item => {
    const option = node('option', item.relativePath + (item.status === 'BOUND' ? '（已绑定）' : '')); option.value = item.id; select.append(option);
  });
  select.value = selected === null ? '' : String(selected); select.disabled = disabled; return select;
}
async function refresh() {
  const detail = await request('/api/batches/' + taskId), body = $('items'), assets = $('assets');
  body.replaceChildren(); assets.replaceChildren(); $('plan').hidden = false;
  const locked = detail.task.confirmed;
  detail.items.forEach(item => {
    if (item.kind !== 'AUDIO') {
      assets.append(node('li', item.relativePath + ' · ' + (labels[item.status] || item.status) + (item.userMessage ? ' · ' + item.userMessage : ''))); return;
    }
    const row = node('tr'), file = node('td'); file.append(node('strong', item.relativePath), node('p', '标题：' + (item.title || '无') + '；歌手：' + (item.artist || '无'))); row.append(file);
    const state = node('td'); state.append(node('p', labels[item.status] || item.status), node('p', bases[item.matchBasis] || item.matchBasis || ''));
    state.append(node('p', item.status === 'CONFLICT' ? '冲突／待确认' : item.lyricsItemId && item.coverItemId ? '完全匹配' : item.lyricsItemId || item.coverItemId ? '部分匹配' : '未匹配附件'));
    if (item.userMessage) state.append(node('p', item.userMessage));
    if (item.candidatesJson) {
      try { const candidates = JSON.parse(item.candidatesJson); const ids = [...(candidates.lyrics || []), ...(candidates.covers || [])]; const names = detail.items.filter(i => ids.includes(i.id)).map(i => i.relativePath); state.append(node('p', '候选：' + (names.join('、') || '无'))); } catch (_) {}
    }
    row.append(state);
    const binding = node('td'); binding.className = 'binding';
    const lyrics = chooseAsset(detail.items,'LYRICS',item.lyricsItemId,locked), cover = chooseAsset(detail.items,'COVER',item.coverItemId,locked);
    binding.append(node('label','歌词'),lyrics,node('label','封面'),cover);
    const save = node('button','保存绑定'); save.disabled = locked || item.resourceId === null;
    save.onclick = async () => { try { await request('/api/batches/' + taskId + '/items/' + item.id + '/bindings', {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({lyricsItemId:lyrics.value ? Number(lyrics.value) : null,coverItemId:cover.value ? Number(cover.value) : null})}); await refresh(); } catch(error) { show(error.message); } };
    binding.append(save); row.append(binding);
    const operation = node('td'), label = node('label'), skip = node('input'); skip.type = 'checkbox'; skip.name = 'skip'; skip.value = item.id; skip.disabled = locked || item.resourceId === null; skip.checked = item.status === 'SKIPPED'; label.append(skip,document.createTextNode('跳过')); operation.append(label); row.append(operation); body.append(row);
  });
  const c = detail.counts, kinds = detail.kinds;
  $('counts').textContent = '总数 ' + detail.total + ' · 音频 ' + (kinds.AUDIO || 0) + ' · 歌词 ' + (kinds.LYRICS || 0) + ' · 封面 ' + (kinds.COVER || 0) + ' · 不支持 ' + (kinds.UNSUPPORTED || 0) + ' · 冲突 ' + (c.CONFLICT || 0) + ' · 成功 ' + (c.SUCCESS || 0) + ' · 失败 ' + (c.FAILED || 0) + ' · 等待／待确认 ' + (c.PENDING || 0) + ' · 处理中 ' + (c.RUNNING || 0) + ' · 跳过 ' + (c.SKIPPED || 0) + ' · 中断 ' + (c.INTERRUPTED || 0);
  show('任务 ID：' + taskId + '；状态：' + (labels[detail.task.status] || detail.task.status));
  $('confirm').disabled = locked; $('execute').disabled = detail.task.status !== 'CONFIRMED';
  $('retry').disabled = !['COMPLETED_WITH_FAILURES','INTERRUPTED','COMPLETED'].includes(detail.task.status) || !(c.FAILED || c.INTERRUPTED);
  const downloadable = ['COMPLETED','COMPLETED_WITH_FAILURES','INTERRUPTED'].includes(detail.task.status);
  $('zip').hidden = $('report').hidden = !downloadable; $('zip').href = '/api/batches/' + taskId + '/zip'; $('report').href = '/api/batches/' + taskId + '/report';
  if (detail.task.status === 'RUNNING') schedule(); else { clearTimeout(timer); timer = null; }
}
function schedule() { clearTimeout(timer); timer = setTimeout(() => refresh().catch(error => show(error.message)), 500); }
function remember(id) { taskId = id; $('taskId').value = id; history.replaceState(null,'','/batch?task=' + id); }
$('upload').onclick = async () => {
  try {
    const files = [...$('files').files,...$('directory').files]; if (!files.length) throw new Error('请选择文件或目录');
    if (policy && files.length > policy.maxFiles) throw new Error('单批文件数量超过上限 ' + policy.maxFiles);
    if (policy && files.reduce((total,file) => total + file.size,0) > policy.maxBytes) throw new Error('单批总大小超过上限');
    const form = new FormData(); files.forEach(file => { form.append('files',file,file.name); form.append('paths',file.webkitRelativePath || file.name); });
    $('upload').disabled = true; show('正在上传…'); const task = await request('/api/batches',{method:'POST',body:form}); remember(task.id); await refresh();
  } catch(error) { show(error.message); } finally { $('upload').disabled = false; }
};
$('restore').onclick = async () => { try { const id = Number($('taskId').value); if (!Number.isSafeInteger(id) || id < 1) throw new Error('请输入有效任务 ID'); remember(id); await refresh(); } catch(error) { show(error.message); } };
$('confirm').onclick = async () => { try { const query = new URLSearchParams(); document.querySelectorAll('input[name=skip]:checked').forEach(box => query.append('skipIds',box.value)); await request('/api/batches/' + taskId + '/confirm?' + query,{method:'POST'}); await refresh(); } catch(error) { show(error.message); } };
for (const action of ['execute','retry']) $(action).onclick = async () => { try { await request('/api/batches/' + taskId + '/' + action,{method:'POST'}); await refresh(); } catch(error) { show(error.message); } };
request('/api/batches/policy').then(value => { policy = value; $('limits').textContent = '每批最多 ' + value.maxFiles + ' 个文件，总大小 ' + Math.floor(value.maxBytes / 1048576) + ' MB；单音频 ' + Math.floor(value.maxAudioBytes / 1048576) + ' MB；并发 ' + value.concurrency + '。'; }).catch(error => show(error.message));
if (taskId) { remember(taskId); refresh().catch(error => show(error.message)); }
