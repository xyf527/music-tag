(() => {
  const panel = document.createElement('p'); panel.setAttribute('role','status'); document.body.append(panel);
  const labels = {DISABLED:'未启用', PENDING:'待处理', RUNNING:'进行中', SUCCEEDED:'成功', FAILED:'失败，暂缓本地清理'};
  async function refresh() {
    try {
      const state = await fetch('/api/operations').then(r=>r.json());
      const report = document.querySelector('#report');
      const task = report && report.href.match(/\/api\/tasks\/(\d+)/);
      const batch = new URLSearchParams(location.search).get('task');
      let text = state.warning || '';
      if (task) {
        const backup = await fetch('/api/tasks/'+task[1]+'/backup').then(r=>r.json());
        text += ' 备份：'+backup.backups.map(b=>labels[b.status] || b.status).join('、')+' '+backup.cleanupReason;
      } else if (batch && /^\d+$/.test(batch)) {
        const backup = await fetch('/api/batches/'+batch+'/backup').then(r=>r.json());
        text += backup.map(b=>'项目 '+b.id+' 备份：'+(labels[b.status] || '待处理')).join('；');
      }
      panel.textContent = text;
    } catch (_) { panel.textContent = '运维状态暂时不可用'; }
  }
  refresh(); setInterval(refresh,5000);
})();
