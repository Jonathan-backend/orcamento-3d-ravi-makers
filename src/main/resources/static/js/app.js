const profile=JSON.parse(localStorage.getItem('profile')||'{}');
if(!profile.email) location.href='/login';
const isOperator=false;
let supportAdmin=false;
userName.textContent=profile.name||profile.email;role.textContent='Administrador';
avatar.textContent=(profile.name||profile.email||'OP').split(/\s+/).map(x=>x[0]).join('').slice(0,2).toUpperCase();
async function performLogout(reason=''){
 try{await fetch('/api/auth/logout',{method:'POST'})}
 finally{localStorage.clear();location.href=reason?`/login?${reason}=1`:'/login'}
}
logout.onclick=()=>performLogout();
const IDLE_TIMEOUT_MS=30*60*1000,IDLE_WARNING_MS=2*60*1000,ACTIVITY_KEY='ravi_last_activity';
let lastActivityWrite=0,idleWarningShown=false,idleLogoutStarted=false;
function registerActivity(){
 const now=Date.now();if(now-lastActivityWrite<15000)return;lastActivityWrite=now;idleWarningShown=false;localStorage.setItem(ACTIVITY_KEY,String(now));
}
function checkIdleSession(){
 const last=Number(localStorage.getItem(ACTIVITY_KEY)||Date.now()),idleFor=Date.now()-last;
 if(idleFor>=IDLE_TIMEOUT_MS&&!idleLogoutStarted){idleLogoutStarted=true;performLogout('inactive');return}
 if(idleFor>=IDLE_TIMEOUT_MS-IDLE_WARNING_MS&&!idleWarningShown){idleWarningShown=true;notify('Sua sessão será encerrada em 2 minutos por inatividade. Interaja com a página para continuar.','error')}
}
['pointerdown','keydown','scroll','touchstart'].forEach(eventName=>addEventListener(eventName,registerActivity,{passive:true}));
addEventListener('storage',event=>{if(event.key===ACTIVITY_KEY)idleWarningShown=false});
document.addEventListener('visibilitychange',()=>{if(!document.hidden)checkIdleSession()});
registerActivity();setInterval(checkIdleSession,15000);
menuButton.onclick=()=>document.querySelector('.sidebar').classList.toggle('open');
const hashViews={painel:'dashboard',estoque:'inventory',clientes:'customers',orcamento:'quote',producao:'production',catalogo:'catalog',produtos:'products',impressoras:'printers',historico:'quotes',configuracoes:'settings',relatorios:'supportReports'};
function routeView(){
 let key=location.hash.slice(1)||'painel';if((isOperator&&['configuracoes','impressoras'].includes(key))||(key==='relatorios'&&!supportAdmin)){key='painel';history.replaceState({},'','#painel')}const view=hashViews[key]||'dashboard';
 document.querySelectorAll('.app-view').forEach(section=>section.classList.toggle('hidden',section.dataset.view!==view));
 document.querySelectorAll('[data-view-link]').forEach(link=>link.classList.toggle('active',link.dataset.viewLink===view));
 document.querySelector('.sidebar').classList.remove('open');window.scrollTo({top:0,behavior:'instant'});
}
document.querySelectorAll('[data-view-link]').forEach(link=>link.onclick=()=>setTimeout(routeView,0));
window.addEventListener('hashchange',routeView);
document.querySelectorAll('.soon-link').forEach(link=>link.onclick=e=>{e.preventDefault();document.querySelector('.sidebar').classList.remove('open');moduleToast.textContent=`${link.dataset.module} será disponibilizado em breve.`;moduleToast.classList.remove('hidden');setTimeout(()=>moduleToast.classList.add('hidden'),2600)});
const money=v=>Number(v).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
const BRL_MAX=9999999999.99;
function brlValue(input){const raw=String(input?.value||'').replace(/\./g,'').replace(',','.').replace(/[^\d.-]/g,'');return Math.min(BRL_MAX,Math.max(0,Number(raw)||0))}
function brlText(value){return Math.min(BRL_MAX,Math.max(0,Number(value)||0)).toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2})}
function applyBrlMask(input){
 let digits=input.value.replace(/\D/g,'').slice(0,12),value=Math.min(BRL_MAX,Number(digits||0)/100);input.value=brlText(value);
}
document.querySelectorAll('.brl-input').forEach(input=>{input.maxLength=16;input.addEventListener('input',()=>applyBrlMask(input));input.addEventListener('focus',()=>input.select());if(input.value)applyBrlMask(input)});
const duration=m=>m>=60?`${Math.floor(m/60)}h ${Math.round(m%60)}min`:`${Math.max(1,Math.round(m))} min`;
const toastStack=document.body.appendChild(Object.assign(document.createElement('div'),{className:'app-toast-stack'}));
let lastToast='',lastToastAt=0;
function notify(message,type='success'){
 const text=String(message||'').trim();if(!text)return;
 const now=Date.now();if(text===lastToast&&now-lastToastAt<1500)return;lastToast=text;lastToastAt=now;
 const toast=document.createElement('div');toast.className=`app-toast ${type}`;toast.innerHTML=`<span>${type==='success'?'✓':'!'}</span><div><strong>${type==='success'?'Tudo certo':'Verifique os dados'}</strong><p>${escapeHtml(text)}</p></div><button type="button" aria-label="Fechar">×</button>`;
 toast.querySelector('button').onclick=()=>toast.remove();toastStack.appendChild(toast);
 requestAnimationFrame(()=>toast.classList.add('visible'));setTimeout(()=>{toast.classList.remove('visible');setTimeout(()=>toast.remove(),220)},4200);
}
document.addEventListener('invalid',event=>{
 const field=event.target;if(!(field instanceof HTMLInputElement||field instanceof HTMLSelectElement||field instanceof HTMLTextAreaElement))return;
 const label=field.closest('label')?.childNodes[0]?.textContent?.trim()||field.getAttribute('aria-label')||'campo obrigatório';
 notify(field.validity.valueMissing?`Preencha o campo “${label}”.`:`Confira o valor informado em “${label}”.`,'error');
},true);
function xhrApi(url,options){
 return new Promise((resolve,reject)=>{
  const xhr=new XMLHttpRequest();xhr.open(options.method||'GET',url,true);xhr.responseType='blob';xhr.timeout=180000;
  Object.entries(options.headers||{}).forEach(([name,value])=>xhr.setRequestHeader(name,value));
  xhr.onload=()=>resolve(new Response(xhr.response,{status:xhr.status,statusText:xhr.statusText}));
  xhr.onerror=()=>reject(Error('Falha de conexão com o servidor'));
  xhr.onabort=()=>reject(Error('Envio cancelado'));
  xhr.ontimeout=()=>reject(Error('O servidor demorou demais para responder'));
  xhr.send(options.body??null);
 });
}
async function api(url,options={}){
 const successMessage=options.successMessage;delete options.successMessage;
 options.headers={...(options.headers||{})};
 let r,lastError;
 for(let attempt=0;attempt<3;attempt++){
  try{r=await fetch(url,options);break}
  catch(e){lastError=e;if(attempt<2)await new Promise(resolve=>setTimeout(resolve,700*(attempt+1)))}
 }
 if(!r){
  try{r=await xhrApi(url,options)}
  catch(e){const message='Não foi possível conectar ao servidor. Verifique se a aplicação está ativa e tente novamente.';notify(message,'error');throw Error(message)}
 }
 if(r.status===401){localStorage.clear();location.href='/login';throw Error('Sessão expirada')}
 if(r.status===403){notify('Você não tem permissão para realizar esta ação.','error');throw Error('Você não tem permissão para realizar esta ação.')}
 if(!r.ok){
  r.clone().json().then(data=>notify(data.message||'Não foi possível concluir a operação.','error')).catch(()=>notify('Não foi possível concluir a operação.','error'));
 }else{
  const method=(options.method||'GET').toUpperCase(),isMutation=['POST','PUT','PATCH','DELETE'].includes(method),isAnalysis=url.includes('/analyze')||url.includes('/preview');
  if(isMutation&&!isAnalysis)notify(successMessage||({POST:'Salvo com sucesso.',PUT:'Alterações salvas com sucesso.',PATCH:'Atualizado com sucesso.',DELETE:'Excluído com sucesso.'}[method]));
 }
 return r;
}
function show(q){
 quoteAwaiting.classList.add('hidden');result.classList.remove('hidden');resultName.textContent=q.fileName;total.textContent=money(q.total);
 time.textContent=duration(q.printTimeMinutes);grams.textContent=`${q.filamentGrams.toFixed(1)} g`;
 material.textContent=money(q.materialCost);machine.textContent=money(q.machineCost);energy.textContent=money(q.energyCost);
}
let manualFinalPrice=false;
function showBudget(b){
 const totals=b.plates.reduce((a,p)=>({minutes:a.minutes+p.printTimeMinutes,grams:a.grams+p.filamentGrams,
 material:a.material+Number(p.materialCost),machine:a.machine+Number(p.machineCost),energy:a.energy+Number(p.energyCost)}),{minutes:0,grams:0,material:0,machine:0,energy:0});
 quoteAwaiting.classList.add('hidden');result.classList.remove('hidden');resultName.textContent=b.id?`#${b.id} · ${b.title}`:b.title;total.textContent=money(b.total);
 time.textContent=duration(totals.minutes);grams.textContent=`${totals.grams.toFixed(1)} g`;material.textContent=money(totals.material);machine.textContent=money(totals.machine);energy.textContent=money(totals.energy);
 breakFilament.textContent=money(b.filamentCost??b.materialCost);breakMagnets.textContent=money(b.consumableCost||0);magnetCostLine.classList.toggle('has-cost',Number(b.consumableCost)>0);breakMaterial.textContent=money(b.materialCost);breakEnergy.textContent=money(b.energyCost);breakMachine.textContent=money(b.machineCost);breakLabor.textContent=money(b.laborCost);breakMaintenance.textContent=money(b.maintenanceCost);breakAdditional.textContent=money(b.additionalCost);breakFailure.textContent=money(b.failureCost);breakCostTotal.textContent=money(b.costTotal);budgetFinalPrice.value=brlText(b.total);manualFinalPrice=false;budgetProfit.textContent=money(b.profit);budgetPurpose.value=b.purpose||'STANDARD_SALE';result.querySelector('.success-badge').textContent='CALCULADO';
}
async function load(){
 const r=await api('/api/quotes');const rows=await r.json();empty.hidden=rows.length>0;
 quoteCount.textContent=rows.length;
 if(rows.length){lastTotal.textContent=`último: ${money(rows[0].total)}`;lastTime.textContent=`último projeto: ${duration(rows[0].printTimeMinutes)}`}
 history.innerHTML=rows.map(q=>`<tr><td><strong>${escapeHtml(q.fileName)}</strong></td><td>${escapeHtml(q.customer)}</td>
 <td>${duration(q.printTimeMinutes)}</td><td>${q.filamentGrams.toFixed(1)} g</td><td><strong>${money(q.total)}</strong></td>
 <td>${new Date(q.createdAt).toLocaleDateString('pt-BR')}</td></tr>`).join('');
}
const typeNames={MOONRAKER:'Klipper / Moonraker',OCTOPRINT:'OctoPrint',PRUSALINK:'PrusaLink',BAMBU_LAN:'Bambu LAN',GENERIC:'Outro',MANUAL:'Manual'};
const printerModelDirectory={
 'Creality':['K2','K2 Combo','K2 Plus','K2 Plus Combo','K1C','K1 Max','K1 SE','Ender-3 V3','Ender-3 V3 Plus','Ender-3 V3 KE','Ender-3 V3 SE','Creality Hi','Creality Hi Combo'],
 'Bambu Lab':['H2D','X1 Carbon','X1E','P1S','P1P','A1','A1 Mini'],
 'Prusa Research':['CORE One+','CORE One L','MK4S','XL','MINI+'],
 'Anycubic':['Kobra X','Kobra S1','Kobra S1 Combo','Kobra S1 Max','Kobra 3','Kobra 3 Combo','Kobra 2 Pro','Kobra 2 Max'],
 'Elegoo':['Centauri Carbon','Centauri Carbon 2','Centauri Carbon 2 Combo','Neptune 4','Neptune 4 Pro','Neptune 4 Plus','Neptune 4 Max','OrangeStorm Giga'],
 'Flashforge':['Adventurer 5M','Adventurer 5M Pro','AD5X','Creator 5','Creator 5 Pro','Guider 3 Ultra'],
 'QIDI Tech':['Q2C','Q1 Pro','Plus4','X-Max 3','X-Plus 3','X-Smart 3'],
 'Sovol':['SV08','SV08 Max','Sovol Zero','SV07','SV07 Plus','SV06','SV06 Plus'],
 'Artillery':['Sidewinder X4 Pro','Sidewinder X4 Plus','Sidewinder X3 Pro','Sidewinder X3 Plus','Genius Pro'],
 'Snapmaker':['U1','Artisan','J1S','Snapmaker 2.0 A150','A250T','A350T'],'Phrozen':['Arco'],
 'UltiMaker':['S8','S8 Pro Bundle','S6','S5','S3','Method','Method X','Method XL','Factor 4 Plus'],
 'Raise3D':['Pro3','Pro3 Plus','Pro3 HS','Pro3 Plus HS','E2','E2CF','RMF500'],'MakerBot':['Sketch','Sketch Large','Method','Method X'],
 'AnkerMake':['M5','M5C'],'Kingroon':['KP3S','KP3S Pro','KP5L','KLP1'],'FLSUN':['T1','T1 Pro','S1','V400','Super Racer'],
 'Tronxy':['X5SA','X5SA Pro','X5SA-500 Pro','VEHO 600','VEHO 1000'],'Geeetech':['M1','M1 Mini','Thunder','A10M','A20M','A30M'],
 'Zortrax':['M200 Plus','M300 Plus','M300 Dual','Endureal'],'LulzBot':['TAZ WorkHorse','TAZ Pro','Mini 2'],
 'Voron Design':['Voron 0.2','Voron Trident','Voron 2.4','Voron Switchwire'],'Rat Rig':['V-Core 4','V-Core 3.1','V-Minion'],
 'Formbot':['Troodon 2.0','T-Rex 3.0','Voron kits']
};
const printerKnownSpecs={'Creality|K2':{powerWatts:1350,type:'MANUAL'},'Creality|K2 Plus':{powerWatts:1200,type:'MANUAL'},'Creality|K1C':{powerWatts:350,type:'MANUAL'},'Creality|Ender-3 V3':{powerWatts:350,type:'MANUAL'},'Bambu Lab|P1S':{powerWatts:1000,type:'BAMBU_LAN'},'Bambu Lab|X1 Carbon':{powerWatts:1000,type:'BAMBU_LAN'},'Bambu Lab|A1':{powerWatts:1300,type:'BAMBU_LAN'},'Bambu Lab|A1 Mini':{powerWatts:1000,type:'BAMBU_LAN'},'Prusa Research|MK4S':{powerWatts:240,type:'PRUSALINK'},'Elegoo|Neptune 4 Pro':{powerWatts:400,type:'MANUAL'},'Anycubic|Kobra 3':{powerWatts:400,type:'MANUAL'}};
const printerModelCatalog=Object.entries(printerModelDirectory).flatMap(([manufacturer,models])=>models.map(model=>({manufacturer,model,...(printerKnownSpecs[`${manufacturer}|${model}`]||{})})));
printerModelSuggestions.innerHTML=printerModelCatalog.map(item=>`<option value="${escapeHtml(item.model)}" label="${escapeHtml(item.manufacturer)}"></option>`).join('');
let printersCache=[];
function printerCard(p){
 const model=[p.manufacturer,p.model].filter(Boolean).join(' · ')||typeNames[p.type],monitored=p.monitoringEnabled?'Monitoramento ativo':'Controle manual',depreciation=p.usefulLifeHours?Number(p.acquisitionCost||0)/p.usefulLifeHours:0;
 const detail=p.status.detail?`<p class="muted">${escapeHtml(p.status.detail)}</p>`:'';
 return `<article class="printer-card"><div class="printer-card-head"><div><h3>${escapeHtml(p.name)}</h3><p class="muted">${escapeHtml(model)}</p></div><span class="power-badge">${p.powerWatts} W</span></div>
 <div class="printer-status-row"><div class="printer-status ${p.status.code}"><i></i>${escapeHtml(p.status.label)}</div><small>${escapeHtml(monitored)}</small></div>${detail}
 ${p.status.fileName?`<div class="printer-job">▤ ${escapeHtml(p.status.fileName)}</div>`:''}
 <div class="printer-meta"><span><small>AQUISIÇÃO</small><strong>${money(p.acquisitionCost||0)}</strong></span><span><small>DEPRECIAÇÃO/H</small><strong>${money(depreciation)}</strong></span></div>
 <div class="printer-actions"><button class="icon-button" type="button" data-edit-printer="${p.id}">✎ Editar</button><button class="danger-button" type="button" data-delete-printer="${p.id}">♲ Excluir</button></div></article>`;
}
function renderPrinters(){
 const query=printerSearch.value.trim().toLowerCase(),filtered=printersCache.filter(p=>`${p.name} ${p.manufacturer||''} ${p.model||''}`.toLowerCase().includes(query));
 printerList.innerHTML=filtered.map(printerCard).join('');printerEmpty.hidden=printersCache.length>0;printerResultCount.textContent=`${filtered.length} ${filtered.length===1?'máquina':'máquinas'}`;
 document.querySelectorAll('[data-edit-printer]').forEach(btn=>btn.onclick=()=>editPrinter(Number(btn.dataset.editPrinter)));
 document.querySelectorAll('[data-delete-printer]').forEach(btn=>btn.onclick=async()=>{if(!confirm('Excluir esta impressora?'))return;await api(`/api/printers/${btn.dataset.deletePrinter}`,{method:'DELETE'});await loadPrinters()});
}
async function loadPrinters(){
 let r;try{r=await api('/api/printers')}catch(e){return}const printers=await r.json();printersCache=printers;
 dashboardPrinters.textContent=printers.length;printerCount.textContent=printers.length;printerTotalPower.textContent=`${printers.reduce((sum,p)=>sum+p.powerWatts,0).toLocaleString('pt-BR')} W`;printerOnlineCount.textContent=printers.filter(p=>p.monitoringEnabled).length;renderPrinters();
 dashboardPrinterCaption.textContent=`${printers.length} ${printers.length===1?'impressora cadastrada':'impressoras cadastradas'}`;
 refreshPlateOptions();
}
let inventoryCache=[];
const colorMap={preto:'#111827',branco:'#f8fafc',azul:'#2489ff',vermelho:'#ef4444',verde:'#22c55e',amarelo:'#facc15',laranja:'#f97316',roxo:'#9333ea',rosa:'#ec4899',cinza:'#94a3b8',marrom:'#92400e'};
function inventoryRow(f){
 const chip=colorMap[f.color.toLowerCase()]||'#15c8ed';
 return `<tr><td><strong>${escapeHtml(f.brand)}</strong></td><td>${escapeHtml(f.material)}</td><td><span class="color-chip" style="background:${chip}"></span>${escapeHtml(f.color)}</td><td>${money(f.pricePerKg)}</td><td class="stock-positive">${f.weightGrams} g</td><td><div class="inventory-actions"><button class="icon-button" data-stock-action="add" data-id="${f.id}" title="Adicionar">＋</button><button class="icon-button" data-stock-action="remove" data-id="${f.id}" title="Retirar">−</button><button class="icon-button" data-stock-action="edit" data-id="${f.id}" title="Editar">✎</button><button class="icon-button" data-stock-action="history" data-id="${f.id}" title="Histórico">↶</button><button class="danger-button" data-stock-action="delete" data-id="${f.id}" title="Excluir">×</button></div></td></tr>`;
}
function renderInventory(){
 const query=inventorySearch.value.trim().toLowerCase();
 const filtered=inventoryCache.filter(f=>`${f.brand} ${f.material} ${f.color}`.toLowerCase().includes(query));
 inventoryRows.innerHTML=filtered.map(inventoryRow).join('');inventoryEmpty.hidden=inventoryCache.length>0;
 document.querySelectorAll('[data-stock-action]').forEach(button=>button.onclick=()=>stockAction(button.dataset.stockAction,Number(button.dataset.id)));
}
const LOW_FILAMENT_GRAMS=250;
function updateAffiliateRecommendations(){
 const low=inventoryCache.filter(f=>Number(f.weightGrams)<=LOW_FILAMENT_GRAMS);
 inventoryAffiliateOffer.classList.toggle('hidden',low.length===0);
 if(low.length)inventoryAffiliateReason.textContent=low.length===1
  ?`${low[0].material} ${low[0].color}: restam apenas ${Number(low[0].weightGrams).toLocaleString('pt-BR')} g.`
  :`${low.length} filamentos estão com 250 g ou menos em estoque.`;
 const demand=new Map();
 document.querySelectorAll('.filament-use-row').forEach(row=>{const id=Number(row.querySelector('.use-filament')?.value||0),grams=['.use-piece','.use-purge','.use-tower','.use-support'].reduce((sum,selector)=>sum+Number(row.querySelector(selector)?.value||0),0);if(id&&grams)demand.set(id,(demand.get(id)||0)+grams)});
 const shortages=[...demand].map(([id,required])=>{const filament=inventoryCache.find(f=>f.id===id),available=Number(filament?.weightGrams||0);return {filament,required,available,missing:Math.max(0,required-available),remaining:available-required}}).filter(x=>x.missing>0||x.remaining<=LOW_FILAMENT_GRAMS);
 quoteAffiliateOffer.classList.toggle('hidden',shortages.length===0);
 if(shortages.length){const x=shortages[0],name=x.filament?`${x.filament.material} ${x.filament.color}`:'filamento',materialName=x.filament?.material||'';quoteAffiliateReason.textContent=x.missing>0?`Faltam aproximadamente ${Math.ceil(x.missing).toLocaleString('pt-BR')} g de ${name} para produzir este projeto.`:`Após a produção restarão somente ${Math.max(0,Math.floor(x.remaining)).toLocaleString('pt-BR')} g de ${name}.`;quoteOffersLink.href=`/ofertas?origem=orcamento&material=${encodeURIComponent(materialName)}&quantidade=${Math.ceil(x.missing||1000)}`;}
}
async function loadInventory(){
 let r;try{r=await api('/api/inventory')}catch(e){return}const data=await r.json();inventoryCache=data.filaments;
 inventoryCount.textContent=data.registered;inventoryKg.textContent=`${Number(data.totalKg).toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:3})} kg`;inventoryValue.textContent=money(data.totalValue);renderInventory();
 dashboardInventoryKg.textContent=inventoryKg.textContent;dashboardStockValue.textContent=inventoryValue.textContent;
 refreshPlateOptions();updateAffiliateRecommendations();
}
newFilament.onclick=()=>{filamentForm.reset();filamentId.value='';filamentDialogTitle.textContent='Novo filamento';filamentError.textContent='';filamentDialog.showModal()};
inventorySearch.oninput=renderInventory;
document.querySelectorAll('[data-close]').forEach(button=>button.onclick=()=>document.getElementById(button.dataset.close).close());
filamentForm.onsubmit=async e=>{
 e.preventDefault();filamentError.textContent='';const id=filamentId.value;
 const body={material:filamentMaterial.value,color:filamentColor.value,brand:filamentBrand.value,weightGrams:Number(filamentWeight.value),pricePerKg:brlValue(filamentPrice)};
 const r=await api(id?`/api/inventory/${id}`:'/api/inventory',{method:id?'PUT':'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
 const data=await r.json();if(!r.ok){filamentError.textContent=data.message||'Não foi possível salvar';return}filamentDialog.close();await loadInventory();
};
function stockAction(action,id){
 const f=inventoryCache.find(item=>item.id===id);if(!f)return;
 if(action==='edit'){filamentId.value=f.id;filamentMaterial.value=f.material;filamentColor.value=f.color;filamentBrand.value=f.brand;filamentWeight.value=f.weightGrams;filamentPrice.value=brlText(f.pricePerKg);filamentDialogTitle.textContent='Editar filamento';filamentError.textContent='';filamentDialog.showModal();return}
 if(action==='add'||action==='remove'){movementFilamentId.value=id;movementType.value=action;movementTitle.textContent=action==='add'?`Adicionar · ${f.brand} ${f.material}`:`Retirar · ${f.brand} ${f.material}`;movementForm.reset();movementFilamentId.value=id;movementType.value=action;movementError.textContent='';movementDialog.showModal();return}
 if(action==='history'){showMovementHistory(f);return}
 if(action==='delete'&&confirm(`Excluir ${f.brand} ${f.material} e todo o histórico?`))deleteFilament(id);
}
movementForm.onsubmit=async e=>{
 e.preventDefault();movementError.textContent='';const id=movementFilamentId.value,type=movementType.value;
 const r=await api(`/api/inventory/${id}/${type}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({grams:Number(movementGrams.value),note:movementNote.value})});
 const data=await r.json();if(!r.ok){movementError.textContent=data.message||'Não foi possível movimentar';return}movementDialog.close();await loadInventory();
};
async function deleteFilament(id){await api(`/api/inventory/${id}`,{method:'DELETE'});await loadInventory()}
async function showMovementHistory(f){
 historyTitle.textContent=`Histórico · ${f.brand} ${f.material}`;movementHistory.innerHTML='<p class="empty">Carregando...</p>';historyDialog.showModal();
 const r=await api(`/api/inventory/${f.id}/history`),rows=await r.json();
 movementHistory.innerHTML=rows.length?rows.map(m=>`<div class="movement-row"><strong class="${m.deltaGrams>0?'positive':'negative'}">${m.deltaGrams>0?'+':''}${m.deltaGrams} g</strong><div>${escapeHtml(m.type)}${m.note?`<small> · ${escapeHtml(m.note)}</small>`:''}</div><small>${new Date(m.createdAt).toLocaleString('pt-BR')}</small></div>`).join(''):'<p class="empty">Nenhuma movimentação.</p>';
}
const consumableCategories={MAGNET:'Ímã',GLUE:'Cola / adesivo',RESIN:'Resina',HARDWARE:'Ferragem',PACKAGING:'Embalagem',PAINT:'Tinta / acabamento',OTHER:'Outro'};
const consumableUnits={UNIT:'un.',G:'g',KG:'kg',ML:'ml',L:'l',M:'m'};
let consumablesCache=[];
function consumableRow(c){return `<tr><td><span class="category-badge">${escapeHtml(consumableCategories[c.category]||c.category)}</span></td><td><strong>${escapeHtml(c.name)}</strong></td><td>${escapeHtml(c.brand||'—')}</td><td>${money(c.unitPrice)}</td><td class="stock-positive">${Number(c.quantity).toLocaleString('pt-BR',{maximumFractionDigits:3})} ${consumableUnits[c.unit]}</td><td><strong>${money(c.stockValue)}</strong></td><td><div class="inventory-actions"><button class="icon-button" data-consumable-action="add" data-id="${c.id}" title="Adicionar">＋</button><button class="icon-button" data-consumable-action="remove" data-id="${c.id}" title="Retirar">−</button><button class="icon-button" data-consumable-action="edit" data-id="${c.id}" title="Editar">✎</button><button class="danger-button" data-consumable-action="delete" data-id="${c.id}" title="Excluir">×</button></div></td></tr>`}
function renderConsumables(){const q=consumableSearch.value.trim().toLowerCase(),rows=consumablesCache.filter(c=>`${consumableCategories[c.category]} ${c.name} ${c.brand||''}`.toLowerCase().includes(q));consumableRows.innerHTML=rows.map(consumableRow).join('');consumableEmpty.hidden=consumablesCache.length>0;document.querySelectorAll('[data-consumable-action]').forEach(b=>b.onclick=()=>consumableAction(b.dataset.consumableAction,Number(b.dataset.id)))}
async function loadConsumables(){let r;try{r=await api('/api/consumables')}catch(e){return}consumablesCache=await r.json();consumableCount.textContent=consumablesCache.length;consumableValue.textContent=money(consumablesCache.reduce((s,c)=>s+Number(c.stockValue),0));renderConsumables();if(typeof refreshPlateOptions==='function')refreshPlateOptions()}
newConsumable.onclick=()=>{consumableForm.reset();consumableId.value='';consumableUnitPrice.value='0,00';consumableDialogTitle.textContent='Novo consumível';consumableError.textContent='';consumableDialog.showModal()};
consumableSearch.oninput=renderConsumables;
function consumableAction(action,id){const c=consumablesCache.find(x=>x.id===id);if(!c)return;if(action==='edit'){consumableId.value=c.id;consumableCategory.value=c.category;consumableName.value=c.name;consumableBrand.value=c.brand||'';consumableUnit.value=c.unit;consumableQuantity.value=c.quantity;consumableUnitPrice.value=brlText(c.unitPrice);consumableNotes.value=c.notes||'';consumableDialogTitle.textContent='Editar consumível';consumableError.textContent='';consumableDialog.showModal();return}if(action==='delete'){if(confirm(`Excluir ${c.name}?`))deleteConsumable(id);return}const amount=Number(prompt(`${action==='add'?'Adicionar':'Retirar'} quantos ${consumableUnits[c.unit]}?`));if(amount>0)moveConsumable(id,action,amount)}
consumableForm.onsubmit=async e=>{e.preventDefault();consumableError.textContent='';const id=consumableId.value,body={category:consumableCategory.value,name:consumableName.value,brand:consumableBrand.value,unit:consumableUnit.value,quantity:Number(consumableQuantity.value),unitPrice:brlValue(consumableUnitPrice),notes:consumableNotes.value},r=await api(id?`/api/consumables/${id}`:'/api/consumables',{method:id?'PUT':'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}),data=await r.json();if(!r.ok){consumableError.textContent=data.message||'Não foi possível salvar';return}consumableDialog.close();await loadConsumables()};
async function moveConsumable(id,action,quantity){const r=await api(`/api/consumables/${id}/${action}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({quantity})});if(!r.ok){const d=await r.json();alert(d.message||'Não foi possível movimentar');return}await loadConsumables()}
async function deleteConsumable(id){await api(`/api/consumables/${id}`,{method:'DELETE'});await loadConsumables()}
let customerCache=[];
function formatDocument(value){
 const d=(value||'').replace(/\D/g,'');if(d.length===11)return d.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/,'$1.$2.$3-$4');
 if(d.length===14)return d.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/,'$1.$2.$3/$4-$5');return value||'—';
}
function customerRow(c){
 const contact=[c.phone,c.whatsapp].filter(Boolean).join(' · ');
 return `<tr><td class="customer-name"><strong>${escapeHtml(c.name)}</strong>${c.tradeName?`<small>${escapeHtml(c.tradeName)}</small>`:''}</td><td>${c.personType==='PJ'?'Pessoa jurídica':'Pessoa física'}</td><td>${escapeHtml(formatDocument(c.document))}</td><td><div class="contact-stack">${c.email?`<span>${escapeHtml(c.email)}</span>`:''}${contact?`<small>${escapeHtml(contact)}</small>`:''}</div></td><td>${escapeHtml([c.city,c.addressState].filter(Boolean).join('/'))||'—'}</td><td><span class="status-badge ${c.active?'active':'inactive'}">${c.active?'ATIVO':'INATIVO'}</span></td><td><div class="inventory-actions"><button class="icon-button" data-customer-action="edit" data-id="${c.id}" title="Editar">✎</button><button class="danger-button" data-customer-action="delete" data-id="${c.id}" title="Excluir">×</button></div></td></tr>`;
}
function renderCustomers(){
 const q=customerSearch.value.trim().toLowerCase(),filtered=customerCache.filter(c=>`${c.name} ${c.tradeName||''} ${c.document||''} ${c.email||''} ${c.phone||''} ${c.whatsapp||''}`.toLowerCase().includes(q));
 customerRows.innerHTML=filtered.map(customerRow).join('');customerEmpty.hidden=customerCache.length>0;
 document.querySelectorAll('[data-customer-action]').forEach(button=>button.onclick=()=>customerAction(button.dataset.customerAction,Number(button.dataset.id)));
}
async function loadCustomers(){
 let r;try{r=await api('/api/customers')}catch(e){return}const data=await r.json();customerCache=data.customers;
 customerCount.textContent=data.total;customerActive.textContent=data.active;dashboardCustomers.textContent=data.active;
 quoteCustomer.innerHTML='<option value="">Consumidor não identificado</option>'+customerCache.filter(c=>c.active).map(c=>`<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');renderCustomers();
}
newCustomer.onclick=()=>{customerForm.reset();customerId.value='';customerActiveField.checked=true;customerDialogTitle.textContent='Novo cliente';customerError.textContent='';customerDialog.showModal()};
customerSearch.oninput=renderCustomers;
function customerAction(action,id){
 const c=customerCache.find(item=>item.id===id);if(!c)return;
 if(action==='delete'){if(confirm(`Excluir o cliente ${c.name}?`))deleteCustomer(id);return}
 customerId.value=c.id;customerPersonType.value=c.personType;customerName.value=c.name;customerTradeName.value=c.tradeName||'';customerDocument.value=c.document||'';customerStateRegistration.value=c.stateRegistration||'';customerEmail.value=c.email||'';customerPhone.value=c.phone||'';customerWhatsapp.value=c.whatsapp||'';customerPostalCode.value=c.postalCode||'';customerStreet.value=c.street||'';customerAddressNumber.value=c.addressNumber||'';customerComplement.value=c.complement||'';customerDistrict.value=c.district||'';customerCity.value=c.city||'';customerState.value=c.addressState||'';customerNotes.value=c.notes||'';customerActiveField.checked=c.active;customerDialogTitle.textContent='Editar cliente';customerError.textContent='';customerDialog.showModal();
}
customerForm.onsubmit=async e=>{
 e.preventDefault();customerError.textContent='';const id=customerId.value,body={personType:customerPersonType.value,name:customerName.value,tradeName:customerTradeName.value,document:customerDocument.value,stateRegistration:customerStateRegistration.value,email:customerEmail.value,phone:customerPhone.value,whatsapp:customerWhatsapp.value,postalCode:customerPostalCode.value,street:customerStreet.value,addressNumber:customerAddressNumber.value,complement:customerComplement.value,district:customerDistrict.value,city:customerCity.value,addressState:customerState.value,notes:customerNotes.value,active:customerActiveField.checked};
 const r=await api(id?`/api/customers/${id}`:'/api/customers',{method:id?'PUT':'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
 let data={};try{data=await r.json()}catch(e){}if(!r.ok){customerError.textContent=data.message||'Confira os dados informados.';return}customerDialog.close();await loadCustomers();
};
async function deleteCustomer(id){await api(`/api/customers/${id}`,{method:'DELETE'});await loadCustomers()}
function resetPrinterForm(){printerForm.reset();printerId.value='';printerFormTitle.textContent='Nova impressora';printerMonitoring.checked=true;printerUsefulLife.value=10000;printerAcquisitionCost.value='0,00';printerMaintenance.value='0,00';printerMessage.textContent=''}
togglePrinterForm.onclick=()=>{resetPrinterForm();printerDialog.showModal();printerName.focus()};
cancelPrinter.onclick=()=>printerDialog.close();closePrinterForm.onclick=()=>printerDialog.close();printerSearch.oninput=renderPrinters;
function editPrinter(id){
 const p=printersCache.find(item=>item.id===id);if(!p)return;printerId.value=p.id;printerName.value=p.name;printerManufacturer.value=p.manufacturer||'';printerModel.value=p.model||'';printerPower.value=p.powerWatts;printerAcquisitionCost.value=brlText(p.acquisitionCost);printerUsefulLife.value=p.usefulLifeHours||10000;printerMaintenance.value=brlText(p.maintenancePerHour);printerNotes.value=p.notes||'';printerType.value=p.type;printerUrl.value=p.baseUrl||'';printerApiKey.value='';printerMonitoring.checked=p.monitoringEnabled;printerFormTitle.textContent=`Editar · ${p.name}`;printerMessage.textContent='';printerType.onchange();printerDialog.showModal();
}
printerModel.onchange=()=>{
 const model=printerModel.value.trim().toLowerCase(),maker=printerManufacturer.value.trim().toLowerCase();const selected=printerModelCatalog.find(item=>item.model.toLowerCase()===model&&(!maker||item.manufacturer.toLowerCase()===maker))||printerModelCatalog.find(item=>item.model.toLowerCase()===model);if(!selected)return;
 printerModel.value=selected.model;printerManufacturer.value=selected.manufacturer;if(selected.powerWatts)printerPower.value=selected.powerWatts;if(selected.type){printerType.value=selected.type;printerType.onchange()}
 printerMessage.textContent=selected.powerWatts?'Dados técnicos sugeridos pelo catálogo. Confirme a potência conforme a tensão e o uso da sua máquina.':'Fabricante preenchido. Informe a potência média conforme as especificações da sua máquina.';
};
printerType.onchange=()=>{const manual=printerType.value==='MANUAL';printerUrl.disabled=manual;printerApiKey.disabled=manual;printerMonitoring.disabled=manual;if(manual)printerMonitoring.checked=false};
printerForm.onsubmit=async e=>{
 e.preventDefault();printerMessage.textContent='';const id=printerId.value;
 const payload={name:printerName.value,manufacturer:printerManufacturer.value,model:printerModel.value,powerWatts:Number(printerPower.value),acquisitionCost:brlValue(printerAcquisitionCost),usefulLifeHours:Number(printerUsefulLife.value||10000),maintenancePerHour:brlValue(printerMaintenance),notes:printerNotes.value,type:printerType.value,baseUrl:printerUrl.value,apiKey:printerApiKey.value,monitoringEnabled:printerMonitoring.checked,active:true};
 const r=await api(id?`/api/printers/${id}`:'/api/printers',{method:id?'PUT':'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(payload),successMessage:id?'Impressora atualizada com sucesso.':'Impressora cadastrada com sucesso.'});
 const data=await r.json();if(!r.ok){printerMessage.textContent=data.message||'Não foi possível cadastrar';return}
 resetPrinterForm();printerDialog.close();await loadPrinters();
};
let plateCounter=0;
function printerOptions(selected=''){return '<option value="">Configuração padrão</option>'+((typeof printersCache!=='undefined'?printersCache:[])||[]).filter(p=>p.active).map(p=>`<option value="${p.id}" ${String(p.id)===String(selected)?'selected':''}>${escapeHtml(p.name)} · ${p.powerWatts} W</option>`).join('')}
function filamentOptions(selected=''){return '<option value="">Preço padrão</option>'+inventoryCache.map(f=>`<option value="${f.id}" ${String(f.id)===String(selected)?'selected':''}>${escapeHtml(f.brand)} · ${escapeHtml(f.material)} · ${escapeHtml(f.color)} (${f.weightGrams} g)</option>`).join('')}
function magnetOptions(selected=''){return '<option value="">Selecione o ímã do estoque</option>'+consumablesCache.filter(c=>c.category==='MAGNET').map(c=>`<option value="${c.id}" ${String(c.id)===String(selected)?'selected':''}>${escapeHtml(c.name)} · ${Number(c.quantity).toLocaleString('pt-BR')} ${consumableUnits[c.unit]}</option>`).join('')}
function addFilamentUseRow(card,data={}){
 const row=document.createElement('div');row.className='filament-use-row';row.innerHTML=`<input class="use-color" maxlength="60" placeholder="Cor" value="${escapeHtml(data.color||'')}"><select class="use-filament">${filamentOptions(data.filamentId)}</select><input class="use-piece" type="number" min="0" step="0.01" value="${data.piece||0}"><input class="use-purge" type="number" min="0" step="0.01" value="${data.purge||0}"><input class="use-tower" type="number" min="0" step="0.01" value="${data.tower||0}"><input class="use-support" type="number" min="0" step="0.01" value="${data.support||0}"><strong class="use-total">0,00 g</strong><button type="button" class="remove-use">×</button>`;
 card.querySelector('.filament-use-rows').appendChild(row);row.querySelector('.remove-use').onclick=()=>{if(card.querySelectorAll('.filament-use-row').length>1){row.remove();updatePlateFilamentTotal(card)}};row.querySelectorAll('input[type=number]').forEach(i=>i.oninput=()=>updatePlateFilamentTotal(card));row.querySelector('.use-filament').onchange=updateAffiliateRecommendations;updatePlateFilamentTotal(card);
}
function updatePlateFilamentTotal(card){let total=0;card.querySelectorAll('.filament-use-row').forEach(row=>{const value=['.use-piece','.use-purge','.use-tower','.use-support'].reduce((s,x)=>s+Number(row.querySelector(x).value||0),0);row.querySelector('.use-total').textContent=`${value.toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2})} g`;total+=value});card.querySelector('.plate-grams').value=total.toFixed(2);updateProjectTotals();updateAffiliateRecommendations()}
function addPlateCard(data={}){
 const id=++plateCounter,index=document.querySelectorAll('.plate-card').length+1;
 const card=document.createElement('div');card.className='plate-card';card.dataset.plate=id;card.innerHTML=`<div class="plate-head"><h3>▱ Placa ${index}</h3><button type="button" class="remove-plate" title="Remover">×</button></div>
 <div class="plate-upload-row"><label class="plate-upload">⇧ Enviar G-code<input class="plate-file" type="file" accept=".gcode,.gco,.gc"></label><span class="plate-file-name">ou preencha manualmente</span></div>
 <input class="plate-source-name" type="hidden"><div class="plate-grid"><label class="wide">Nome da placa<input class="plate-name" maxlength="140" value="${escapeHtml(data.name||`Placa ${index}`)}"></label>
 <label>Horas<input class="plate-hours" type="number" min="0" step="1" value="${data.hours||0}"></label><label>Minutos<input class="plate-minutes" type="number" min="0" step="0.01" value="${data.minutes||0}"></label>
 <label>Peso total (g)<input class="plate-grams" type="number" min="0" step="0.01" value="${data.grams||0}"></label><label>Comprimento (m)<input class="plate-meters" type="number" min="0" step="0.01" value="${data.meters||0}"></label>
 <label class="wide">Impressora<select class="plate-printer">${printerOptions(data.printerId??globalBudgetPrinter.value)}</select></label></div>
 <div class="gcode-metadata hidden"><div class="gcode-metadata-head"><strong>✓ Dados detectados no G-code</strong><small>Preenchidos automaticamente. Você ainda pode editar os campos do orçamento.</small></div><div class="gcode-metadata-grid"></div></div>
 <div class="manual-filament"><div class="manual-filament-head"><div><strong>Filamento (edição manual)</strong><small>O total é usado no custo e na baixa do estoque.</small></div><button type="button" class="add-filament-use">＋ Adicionar cor</button></div><div class="filament-use-labels"><span>Cor</span><span>Filamento (estoque)</span><span>Peça (g)</span><span>Purga (g)</span><span>Torre (g)</span><span>Suporte (g)</span><span>Total (g)</span><span></span></div><div class="filament-use-rows"></div></div>
 <div class="magnet-detection hidden"><div><strong>◉ Possível inserção de ímã detectada</strong><small>Confirme o item e a quantidade antes de enviar à produção.</small></div><label>Ímã do estoque<select class="plate-magnet">${magnetOptions()}</select></label><label>Quantidade<input class="plate-magnet-count" type="number" min="0" max="10000" value="0"></label></div>`;
 plateList.appendChild(card);card.querySelector('.remove-plate').onclick=()=>{if(document.querySelectorAll('.plate-card').length>1){card.remove();renumberPlates();updateProjectTotals()}};
 card.querySelector('.plate-file').onchange=e=>analyzePlate(card,e.target.files[0]);
 card.querySelector('.add-filament-use').onclick=()=>{if(card.querySelectorAll('.filament-use-row').length<16)addFilamentUseRow(card)};
 addFilamentUseRow(card,{piece:data.grams||0,filamentId:data.filamentId});
 updateProjectTotals();
}
function renumberPlates(){document.querySelectorAll('.plate-card').forEach((c,i)=>{c.querySelector('h3').textContent=`▱ Placa ${i+1}`})}
function updateProjectTotals(){
 const cards=[...document.querySelectorAll('.plate-card')],totals=cards.reduce((a,c)=>({
  minutes:a.minutes+Number(c.querySelector('.plate-hours').value||0)*60+Number(c.querySelector('.plate-minutes').value||0),
  grams:a.grams+Number(c.querySelector('.plate-grams').value||0),meters:a.meters+Number(c.querySelector('.plate-meters').value||0)
 }),{minutes:0,grams:0,meters:0});
 plateCountSummary.textContent=`${cards.length} ${cards.length===1?'placa':'placas'}`;
 projectTotalWeight.textContent=`${totals.grams.toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2})} g`;
 projectTotalTime.textContent=totals.minutes?duration(totals.minutes):'0 min';
 projectTotalLength.textContent=`${totals.meters.toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2})} m`;
}
async function analyzePlate(card,file){
 if(!file)return;const label=card.querySelector('.plate-file-name');label.textContent='Analisando G-code...';
 try{const body=new FormData();body.append('file',file);const r=await api('/api/budgets/analyze',{method:'POST',body});const a=await r.json();if(!r.ok)throw Error(a.message||'Falha ao analisar');
  card.querySelector('.plate-source-name').value=a.fileName;card.querySelector('.plate-file-name').textContent=a.fileName;card.querySelector('.plate-name').value=a.fileName.replace(/\.(gcode|gco|gc)$/i,'');
  card.querySelector('.plate-hours').value=Math.floor(a.printTimeMinutes/60);card.querySelector('.plate-minutes').value=(a.printTimeMinutes%60).toFixed(2);card.querySelector('.plate-meters').value=a.filamentMeters.toFixed(2);
  if(a.matchedPrinterId){card.querySelector('.plate-printer').value=a.matchedPrinterId;globalBudgetPrinter.value=a.matchedPrinterId}
  const rows=card.querySelector('.filament-use-rows');rows.innerHTML='';(a.filaments?.length?a.filaments:[{grams:a.filamentGrams,color:'',stockFilamentId:null}]).forEach(f=>addFilamentUseRow(card,{piece:f.grams||0,color:f.color||'',filamentId:f.stockFilamentId}));
  const missing=(a.filaments||[]).filter(f=>!f.stockFilamentId&&f.grams>0);label.textContent=missing.length?`${a.fileName} · ${missing.map(f=>[f.material,f.color].filter(Boolean).join(' ')||'filamento').join(', ')} não encontrado no estoque`:`${a.fileName} · dados carregados automaticamente`;
  const metadata=card.querySelector('.gcode-metadata'),items=[['Fatiador',a.slicer||'Não identificado']];
  metadata.querySelector('.gcode-metadata-head strong').textContent='✓ Fatiador detectado';
  metadata.querySelector('.gcode-metadata-head small').textContent='Identificado automaticamente pelo G-code.';
  metadata.classList.remove('hidden');metadata.querySelector('.gcode-metadata-grid').innerHTML=items.map(([k,v])=>`<div><span>${escapeHtml(k)}</span><strong>${escapeHtml(String(v))}</strong></div>`).join('');
  updatePlateFilamentTotal(card);
  if(a.magnetInsertionDetected){card.querySelector('.magnet-detection').classList.remove('hidden');card.querySelector('.plate-magnet-count').value=a.magnetCount||0}
 }catch(e){label.textContent=e.message}
}
function refreshPlateOptions(){
 const globalValue=globalBudgetPrinter.value;globalBudgetPrinter.innerHTML=printerOptions(globalValue);
 document.querySelectorAll('.plate-printer').forEach(s=>{const v=s.value;s.innerHTML=printerOptions(v)});
 document.querySelectorAll('.use-filament').forEach(s=>{const v=s.value;s.innerHTML=filamentOptions(v)});
 document.querySelectorAll('.plate-magnet').forEach(s=>{const v=s.value;s.innerHTML=magnetOptions(v)});
 globalBudgetPrinter.options[0].textContent='Selecione uma impressora';
 document.querySelectorAll('.plate-printer').forEach(s=>{s.required=true;s.options[0].textContent='Selecione uma impressora'});
 document.querySelectorAll('.use-filament').forEach(s=>s.options[0].textContent='Selecione o filamento do estoque');
}
globalBudgetPrinter.onchange=()=>document.querySelectorAll('.plate-printer').forEach(select=>select.value=globalBudgetPrinter.value);
newBudgetPrinter.onclick=()=>{resetPrinterForm();printerDialog.showModal();printerName.focus()};
addPlate.onclick=()=>{if(document.querySelectorAll('.plate-card').length>=20){message.textContent='O limite é de 20 placas por orçamento.';return}message.textContent='';addPlateCard()};addPlateCard();
plateList.addEventListener('input',e=>{if(e.target.matches('.plate-hours,.plate-minutes,.plate-grams,.plate-meters'))updateProjectTotals()});
function budgetPayload(status='DRAFT',includeFinal=false){
 const plates=[...document.querySelectorAll('.plate-card')].map((c,i)=>{const uses=[...c.querySelectorAll('.filament-use-row')].map(row=>({filamentId:row.querySelector('.use-filament').value?Number(row.querySelector('.use-filament').value):null,color:row.querySelector('.use-color').value,pieceGrams:Number(row.querySelector('.use-piece').value||0),purgeGrams:Number(row.querySelector('.use-purge').value||0),towerGrams:Number(row.querySelector('.use-tower').value||0),supportGrams:Number(row.querySelector('.use-support').value||0)}));return {name:c.querySelector('.plate-name').value||`Placa ${i+1}`,fileName:c.querySelector('.plate-source-name').value||null,printTimeMinutes:Number(c.querySelector('.plate-hours').value||0)*60+Number(c.querySelector('.plate-minutes').value||0),filamentGrams:Number(c.querySelector('.plate-grams').value||0),filamentMeters:Number(c.querySelector('.plate-meters').value||0),printerId:c.querySelector('.plate-printer').value?Number(c.querySelector('.plate-printer').value):null,filamentId:null,filamentUses:uses,magnetConsumableId:c.querySelector('.plate-magnet').value?Number(c.querySelector('.plate-magnet').value):null,magnetQuantity:Number(c.querySelector('.plate-magnet-count').value||0)}});
 return {title:budgetTitle.value,customerId:quoteCustomer.value?Number(quoteCustomer.value):null,marginPercent:Number(quoteMargin.value),finalPrice:includeFinal&&manualFinalPrice&&budgetFinalPrice.value!==''?brlValue(budgetFinalPrice):null,purpose:budgetPurpose.value||'STANDARD_SALE',postProcessHours:Number(postProcessHours.value)+Number(postProcessMinutes.value)/60,packingCost:brlValue(packingCost),otherCosts:brlValue(otherCosts),status,plates};
}
async function calculateBudget(save,status='DRAFT'){
 if(!uploadForm.reportValidity())return;
 const invalidPrinter=[...document.querySelectorAll('.plate-printer')].find(select=>!select.value);
 if(invalidPrinter){message.textContent='Selecione uma impressora em todas as placas.';invalidPrinter.focus();return}
 const invalidRow=[...document.querySelectorAll('.filament-use-row')].find(row=>['.use-piece','.use-purge','.use-tower','.use-support'].some(s=>Number(row.querySelector(s).value||0)>0)&&!row.querySelector('.use-filament').value);
 if(invalidRow){message.textContent='Selecione o filamento do estoque em todas as linhas que possuem consumo.';invalidRow.querySelector('.use-filament').focus();return}
 message.textContent='';const button=save?(status==='PRODUCTION'?analyze:saveDraft):previewBudget;button.disabled=true;
 const old=button.textContent;button.textContent=save?'Salvando...':'Calculando...';
 try{const r=await api(save?'/api/budgets':'/api/budgets/preview',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(budgetPayload(status,save))});const b=await r.json();if(!r.ok)throw Error(b.message||'Não foi possível calcular o orçamento');showBudget(b);if(save)message.textContent=status==='PRODUCTION'?`Orçamento #${b.id} enviado para produção.`:`Orçamento #${b.id} salvo como rascunho.`}
 catch(e){message.textContent=e.message}finally{button.disabled=false;button.textContent=old}
}
uploadForm.onsubmit=e=>e.preventDefault();
previewBudget.onclick=()=>calculateBudget(false);
analyze.onclick=()=>calculateBudget(true,'PRODUCTION');
saveDraft.onclick=()=>calculateBudget(true,'DRAFT');
budgetFinalPrice.addEventListener('input',()=>{manualFinalPrice=true;const cost=Number((breakCostTotal.textContent||'').replace(/[^\d,]/g,'').replace(',','.'))||0;budgetProfit.textContent=money(brlValue(budgetFinalPrice)-cost)});
function invalidateCalculatedPrice(e){if(e.target===budgetFinalPrice||result.classList.contains('hidden'))return;manualFinalPrice=false;budgetFinalPrice.value='';result.querySelector('.success-badge').textContent='RECALCULE'}
uploadForm.addEventListener('input',invalidateCalculatedPrice);
uploadForm.addEventListener('change',invalidateCalculatedPrice);
quoteMargin.oninput=()=>marginValue.textContent=`${quoteMargin.value}%`;
let pricingSettingsCache=null;
async function loadPricingSettings(){
 let r;try{r=await api('/api/settings/pricing')}catch(e){return}const s=await r.json();if(!r.ok)return;pricingSettingsCache=s;
 settingEnergy.value=brlText(s.energyPricePerKwh);settingLabor.value=brlText(s.laborPricePerHour);settingFailure.value=Number(s.failureRatePercent);settingMargin.value=Number(s.defaultMarginPercent);settingCurrency.value=s.currency||'BRL';
 quoteMargin.value=Number(s.defaultMarginPercent);marginValue.textContent=`${quoteMargin.value}%`;failureLabel.textContent=`Reserva para falhas (${Number(s.failureRatePercent).toLocaleString('pt-BR')}%)`;
}
pricingSettingsForm.onsubmit=async e=>{e.preventDefault();settingsMessage.textContent='Salvando...';const body={energyPricePerKwh:brlValue(settingEnergy),laborPricePerHour:brlValue(settingLabor),failureRatePercent:Number(settingFailure.value),defaultMarginPercent:Number(settingMargin.value),currency:settingCurrency.value},r=await api('/api/settings/pricing',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}),data=await r.json();if(!r.ok){settingsMessage.textContent=data.message||'Não foi possível salvar';return}settingsMessage.textContent='Parâmetros atualizados.';await loadPricingSettings();setTimeout(()=>settingsMessage.textContent='',2500)};
let companyLogoData=null;
function renderCompanyLogo(value){companyLogoPreview.innerHTML=value?`<img src="${value}" alt="Logomarca da empresa">`:'SUA LOGO'}
async function loadCompanySettings(){let r;try{r=await api('/api/settings/company')}catch(e){return}const c=await r.json();if(!r.ok)return;companyResponsible.value=c.responsibleName||'';companyName.value=c.companyName||'';companyTaxId.value=c.taxId||'';companyPhone.value=c.phone||'';companyEmail.value=c.commercialEmail||'';companyWebsite.value=c.website||'';companyAddress.value=c.address||'';companyLogoData=c.logoDataUrl||null;renderCompanyLogo(companyLogoData)}
companyLogoFile.onchange=()=>{const file=companyLogoFile.files[0];if(!file)return;if(file.size>1500000){companySettingsMessage.textContent='A imagem deve ter no máximo 1,5 MB.';companyLogoFile.value='';return}const reader=new FileReader();reader.onload=()=>{companyLogoData=reader.result;renderCompanyLogo(companyLogoData)};reader.readAsDataURL(file)};
companySettingsForm.onsubmit=async e=>{e.preventDefault();companySettingsMessage.textContent='Salvando...';const body={responsibleName:companyResponsible.value,companyName:companyName.value,taxId:companyTaxId.value,phone:companyPhone.value,commercialEmail:companyEmail.value,website:companyWebsite.value,address:companyAddress.value,logoDataUrl:companyLogoData},r=await api('/api/settings/company',{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),successMessage:'Dados da empresa salvos com sucesso.'}),data=await r.json();if(!r.ok){companySettingsMessage.textContent=data.message||'Não foi possível salvar';return}companySettingsMessage.textContent='Dados da empresa atualizados.';await loadCompanySettings();setTimeout(()=>companySettingsMessage.textContent='',2500)};
let productCache=[],productImages=[],productPrimaryImage=0,couponCache=[];
const productPictures=p=>Array.isArray(p.images)&&p.images.length?p.images:[p.imageDataUrl,p.image2DataUrl,p.image3DataUrl].filter(Boolean);
function renderProductImage(){productImagePreview.innerHTML=productImages.length?productImages.map((value,i)=>`<div class="product-gallery-item ${i===productPrimaryImage?'primary':''}"><button type="button" data-product-main="${i}"><img src="${value}" alt="Foto ${i+1}"><span>${i===productPrimaryImage?'Principal':'Definir principal'}</span></button><button type="button" class="product-gallery-remove" data-product-remove="${i}" aria-label="Remover foto">&times;</button></div>`).join(''):'Sem imagens'}
function updateProductMargin(){const price=brlValue(productPrice),cost=brlValue(productTechnicalCost),margin=price-cost,percent=cost>0?margin/cost*100:0;productMarginPreview.textContent=`Margem estimada: ${money(margin)} (${percent.toLocaleString('pt-BR',{maximumFractionDigits:1})}%)`}
function resetProductForm(){productForm.reset();productId.value='';productCategory.value='Decoração';productPrice.value='0,00';productTechnicalCost.value='0,00';productPublishedCheck.checked=true;productImages=[];productPrimaryImage=0;renderProductImage();updateProductMargin();productDialogTitle.textContent='Novo produto';productError.textContent=''}
function renderProducts(){
 productTotal.textContent=productCache.length;productPublished.textContent=productCache.filter(p=>p.published).length;catalogPublishedCount.textContent=productCache.filter(p=>p.published).length;productEmpty.hidden=productCache.length>0;
 productGrid.innerHTML=productCache.map(p=>`<article class="panel product-admin-card">${productPictures(p)[0]?`<img src="${productPictures(p)[0]}" alt="${escapeHtml(p.name)}">`:'<div class="product-placeholder">3D</div>'}<div class="product-card-content"><span>${escapeHtml(p.category)}</span><h3>${escapeHtml(p.name)}</h3><p>${escapeHtml(p.description||'Sem descrição')}</p><strong>${money(p.price)}</strong><small>Custo ${money(p.technicalCost)} · Margem ${money(p.marginValue)}</small><div><small class="status-badge ${p.published?'active':'inactive'}">${p.published?'PUBLICADO':'RASCUNHO'}</small>${p.featured?'<small class="featured-badge">DESTAQUE</small>':''}</div></div><div class="product-card-actions"><button class="icon-button" data-product-action="edit" data-id="${p.id}">✎ Editar</button><button class="danger-button" data-product-action="delete" data-id="${p.id}">× Excluir</button></div></article>`).join('');
}
async function loadProducts(){let r;try{r=await api('/api/products')}catch(e){return}productCache=await r.json();if(!r.ok)return;renderProducts()}
async function loadCatalogInfo(){let r;try{r=await api('/api/products/catalog-info')}catch(e){return}const c=await r.json();if(!r.ok)return;const link=`${location.origin}/catalogo/${c.ownerId}`;catalogPublicLink.value=link;openCatalogLink.href=link;catalogPublishedCount.textContent=c.products.length}
newProduct.onclick=()=>{resetProductForm();productDialog.showModal();productName.focus()};
productImageFiles.onchange=async()=>{const files=[...productImageFiles.files];productError.textContent='';if(productImages.length+files.length>10){productError.textContent='Você pode selecionar no máximo 10 fotos.';productImageFiles.value='';return}if(files.some(file=>file.size>10000000)){productError.textContent='Cada imagem deve ter no máximo 10 MB.';productImageFiles.value='';return}if(files.some(file=>!['image/png','image/jpeg','image/webp'].includes(file.type))){productError.textContent='Use apenas imagens PNG, JPG ou WebP.';productImageFiles.value='';return}const read=file=>new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=reject;reader.readAsDataURL(file)});try{productImages.push(...await Promise.all(files.map(read)));renderProductImage()}catch{productError.textContent='Não foi possível carregar uma das fotos.'}productImageFiles.value=''};
productImagePreview.onclick=e=>{const remove=e.target.closest('[data-product-remove]'),main=e.target.closest('[data-product-main]');if(remove){const index=Number(remove.dataset.productRemove);productImages.splice(index,1);if(productPrimaryImage===index)productPrimaryImage=0;else if(productPrimaryImage>index)productPrimaryImage--;renderProductImage();return}if(main){productPrimaryImage=Number(main.dataset.productMain);renderProductImage()}};
productPrice.addEventListener('input',updateProductMargin);productTechnicalCost.addEventListener('input',updateProductMargin);
productForm.onsubmit=async e=>{e.preventDefault();const id=productId.value,ordered=productImages.length?[productImages[productPrimaryImage],...productImages.filter((_,i)=>i!==productPrimaryImage)]:[],body={name:productName.value,category:productCategory.value,price:brlValue(productPrice),technicalCost:brlValue(productTechnicalCost),colors:productColors.value,sizes:productSizes.value,description:productDescription.value,imageDataUrl:ordered[0]||null,image2DataUrl:ordered[1]||null,image3DataUrl:ordered[2]||null,images:ordered,published:productPublishedCheck.checked,featured:productFeatured.checked};const r=await api(id?`/api/products/${id}`:'/api/products',{method:id?'PUT':'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}),data=await r.json();if(!r.ok){productError.textContent=data.message||'Não foi possível salvar o produto';return}productDialog.close();await loadProducts();await loadCatalogInfo()};
productGrid.onclick=async e=>{const button=e.target.closest('[data-product-action]');if(!button)return;const p=productCache.find(x=>x.id===Number(button.dataset.id));if(button.dataset.productAction==='edit'){productId.value=p.id;productName.value=p.name;productCategory.value=p.category;productPrice.value=brlText(p.price);productTechnicalCost.value=brlText(p.technicalCost);productColors.value=p.colors||'';productSizes.value=p.sizes||'';productDescription.value=p.description||'';productPublishedCheck.checked=p.published;productFeatured.checked=p.featured;productImages=[...productPictures(p)];productPrimaryImage=0;renderProductImage();updateProductMargin();productDialogTitle.textContent='Editar produto';productError.textContent='';productDialog.showModal();return}if(button.dataset.productAction==='delete'&&confirm(`Excluir o produto “${p.name}”?`)){await api(`/api/products/${p.id}`,{method:'DELETE'});await loadProducts();await loadCatalogInfo()}};
copyCatalogLink.onclick=async()=>{try{await navigator.clipboard.writeText(catalogPublicLink.value);catalogMessage.textContent='Link copiado para a área de transferência.'}catch{catalogPublicLink.select();catalogMessage.textContent='Link selecionado. Use Ctrl+C para copiar.'}};
function renderCoupons(){couponList.innerHTML=couponCache.map(c=>`<div><strong>${escapeHtml(c.code)}</strong><span>${Number(c.discountPercent).toLocaleString('pt-BR')}% · ${c.active?'Ativo':'Inativo'}</span><button class="icon-button" data-coupon-action="edit" data-id="${c.id}">✎</button><button class="danger-button" data-coupon-action="delete" data-id="${c.id}">×</button></div>`).join('')}
async function loadCoupons(){const r=await api('/api/coupons');couponCache=await r.json();if(r.ok)renderCoupons()}
couponForm.onsubmit=async e=>{e.preventDefault();const id=couponId.value,r=await api(id?`/api/coupons/${id}`:'/api/coupons',{method:id?'PUT':'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({code:couponCode.value,discountPercent:Number(couponPercent.value),active:couponActive.checked})}),data=await r.json();if(!r.ok){couponMessage.textContent=data.message||'Não foi possível salvar o cupom';return}couponForm.reset();couponId.value='';couponActive.checked=true;couponMessage.textContent='Cupom salvo.';await loadCoupons()};
couponList.onclick=async e=>{const button=e.target.closest('[data-coupon-action]');if(!button)return;const c=couponCache.find(x=>x.id===Number(button.dataset.id));if(button.dataset.couponAction==='edit'){couponId.value=c.id;couponCode.value=c.code;couponPercent.value=c.discountPercent;couponActive.checked=c.active;return}if(confirm(`Excluir o cupom ${c.code}?`)){await api(`/api/coupons/${c.id}`,{method:'DELETE'});await loadCoupons()}};
let productionCache=[],productionTab='line';
let dashboardDays=30;
const productionStatusLabels={PRODUCTION:'Na fila',PRINTING:'Imprimindo',PAUSED:'Pausada',COMPLETED:'Concluída',CANCELLED:'Cancelada',DRAFT:'Orçamento'};
const dashboardStatusColors={PRODUCTION:'#ff6a00',PRINTING:'#ff9a3d',PAUSED:'#ffd0a6',COMPLETED:'#f3f3f3',CANCELLED:'#ff465f'};
function productionDuration(minutes){const value=Number(minutes||0);return value>=60?`${Math.floor(value/60)}h ${Math.round(value%60)}min`:`${Math.round(value)} min`}
function dashboardFilteredOrders(){
 const limit=Date.now()-dashboardDays*86400000;
 return productionCache.filter(o=>o.status!=='DRAFT'&&new Date(o.createdAt).getTime()>=limit);
}
function renderDashboardChart(orders){
 const bucketCount=Math.min(dashboardDays,12),bucketMs=dashboardDays*86400000/bucketCount,now=Date.now(),points=[];
 for(let i=0;i<bucketCount;i++){const start=now-(bucketCount-i)*bucketMs,end=start+bucketMs,rows=orders.filter(o=>{const t=new Date(o.createdAt).getTime();return t>=start&&t<end}),revenue=rows.filter(o=>o.status==='COMPLETED').reduce((s,o)=>s+Number(o.total),0),profit=rows.filter(o=>o.status==='COMPLETED').reduce((s,o)=>s+Math.max(0,Number(o.total)-Number(o.costTotal)),0);points.push({label:new Date(end).toLocaleDateString('pt-BR',{day:'2-digit',month:'2-digit'}),revenue,profit})}
 const max=Math.max(1,...points.flatMap(p=>[p.revenue,p.profit])),coords=key=>points.map((p,i)=>`${points.length===1?50:5+i*(90/(points.length-1))},${92-(p[key]/max)*76}`).join(' ');
 dashboardChart.innerHTML=`<svg viewBox="0 0 100 100" preserveAspectRatio="none" aria-label="Gráfico de faturamento e lucro"><defs><linearGradient id="revenueFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ff6a00" stop-opacity=".35"/><stop offset="1" stop-color="#ff6a00" stop-opacity="0"/></linearGradient></defs><g class="chart-grid">${[16,35,54,73,92].map(y=>`<line x1="5" y1="${y}" x2="95" y2="${y}"/>`).join('')}</g><polygon points="5,92 ${coords('revenue')} 95,92" fill="url(#revenueFill)"/><polyline class="chart-revenue" points="${coords('revenue')}"/><polyline class="chart-profit" points="${coords('profit')}"/></svg><div class="dashboard-axis">${points.map(p=>`<span>${p.label}</span>`).join('')}</div>`;
}
function renderDashboard(){
 const orders=dashboardFilteredOrders(),completed=orders.filter(o=>o.status==='COMPLETED'),active=orders.filter(o=>['PRODUCTION','PRINTING','PAUSED'].includes(o.status)),revenue=completed.reduce((s,o)=>s+Number(o.total),0),cost=completed.reduce((s,o)=>s+Number(o.costTotal),0),receivable=active.reduce((s,o)=>s+Number(o.total),0),hours=orders.reduce((s,o)=>s+Number(o.printTimeMinutes),0)/60;
 dashboardRevenue.textContent=money(revenue);dashboardProfit.textContent=money(Math.max(0,revenue-cost));dashboardReceivable.textContent=money(receivable);dashboardFailures.textContent=money(orders.reduce((s,o)=>s+Number(o.costTotal)*.05,0));dashboardActivePrints.textContent=active.length;dashboardPrintHours.textContent=`${hours.toLocaleString('pt-BR',{minimumFractionDigits:1,maximumFractionDigits:1})} h`;
 renderDashboardChart(orders);
 const statusCounts=Object.keys(dashboardStatusColors).map(status=>({status,count:orders.filter(o=>o.status===status).length})).filter(x=>x.count),totalStatus=statusCounts.reduce((s,x)=>s+x.count,0);let cursor=0,segments=statusCounts.map(x=>{const start=cursor,end=cursor+(x.count/Math.max(1,totalStatus))*360;cursor=end;return `${dashboardStatusColors[x.status]} ${start}deg ${end}deg`});
 dashboardDonut.style.background=segments.length?`conic-gradient(${segments.join(',')})`:'#1c2c43';dashboardDonutTotal.textContent=totalStatus;dashboardStatusLegend.innerHTML=statusCounts.length?statusCounts.map(x=>`<span><i style="background:${dashboardStatusColors[x.status]}"></i>${productionStatusLabels[x.status]} <strong>${x.count}</strong></span>`).join(''):'<span class="muted">Nenhuma ordem no período.</span>';
 const recent=[...orders].sort((a,b)=>new Date(b.createdAt)-new Date(a.createdAt)).slice(0,5),row=o=>`<a href="#producao"><div><strong>${escapeHtml(o.title)}</strong><small>${escapeHtml(o.customer||'Sem cliente')} · ${productionDuration(o.printTimeMinutes)}</small></div><span class="dashboard-status-chip ${o.status}">${productionStatusLabels[o.status]}</span><b>${money(o.total)}</b></a>`;
 dashboardRecentProduction.innerHTML=recent.length?recent.map(row).join(''):'<p class="dashboard-empty">Nenhuma produção recente.</p>';
 const upcoming=recent.filter(o=>['PRODUCTION','PRINTING','PAUSED'].includes(o.status));dashboardUpcoming.innerHTML=upcoming.length?upcoming.map(o=>{const forecast=new Date(Date.now()+Math.max(1,Math.ceil(Number(o.printTimeMinutes)/1440))*86400000);return `<a href="#producao"><div><strong>${escapeHtml(o.title)}</strong><small>Previsão calculada pelo tempo de impressão</small></div><span class="dashboard-status-chip ${o.status}">${productionStatusLabels[o.status]}</span><b>${forecast.toLocaleDateString('pt-BR')}</b></a>`}).join(''):'<p class="dashboard-empty">Nenhuma entrega em andamento.</p>';
}
function renderProduction(){
 const term=productionSearch.value.trim().toLowerCase(),status=productionFilter.value;
 const rows=productionCache.filter(o=>(productionTab==='drafts'?o.status==='DRAFT':o.status!=='DRAFT')&&(!status||o.status===status)&&(!term||[o.title,o.customer,o.printer,String(o.id)].some(v=>String(v||'').toLowerCase().includes(term))));
 productionRows.innerHTML=rows.map(o=>`<tr><td><div class="production-order"><strong>#${o.id} · ${escapeHtml(o.title)}</strong><small>${o.plates} ${o.plates===1?'placa':'placas'} · ${new Date(o.createdAt).toLocaleDateString('pt-BR')}</small></div></td><td>${escapeHtml(o.customer||'—')}</td><td>${escapeHtml(o.printer||'—')}</td><td>${Number(o.filamentGrams).toLocaleString('pt-BR',{minimumFractionDigits:2,maximumFractionDigits:2})} g</td><td>${productionDuration(o.printTimeMinutes)}</td><td>${money(o.costTotal)}</td><td><strong>${money(o.total)}</strong></td><td>${o.status==='DRAFT'?'<span class="production-status DRAFT">Orçamento</span>':`<select class="production-status-select ${o.status}" data-production-id="${o.id}">${['PRODUCTION','PRINTING','PAUSED','COMPLETED','CANCELLED'].map(s=>`<option value="${s}" ${s===o.status?'selected':''}>${productionStatusLabels[s]}</option>`).join('')}</select>`}</td><td><div class="production-actions"><button type="button" title="Visualizar" data-production-action="view" data-id="${o.id}">Ver</button><button type="button" title="Editar" data-production-action="edit" data-id="${o.id}">Editar</button><button type="button" title="PDF" data-production-action="pdf" data-id="${o.id}">PDF</button><button type="button" title="Criar nova impressão deste orçamento" data-production-action="duplicate" data-id="${o.id}">Duplicar</button><button type="button" class="danger-button" title="Excluir" data-production-action="delete" data-id="${o.id}">Excluir</button></div></td></tr>`).join('');
 productionEmpty.hidden=rows.length>0;
 productionRows.querySelectorAll('.production-status-select').forEach(select=>select.onchange=()=>updateProductionStatus(select));
}
async function loadProduction(){
 let r;try{r=await api('/api/budgets/production')}catch(e){productionMessage.textContent=e.message;return}
 const data=await r.json();if(!r.ok){productionMessage.textContent=data.message||'Não foi possível carregar a produção';return}
 productionCache=data;const orders=data.filter(o=>o.status!=='DRAFT'),active=orders.filter(o=>['PRODUCTION','PRINTING','PAUSED'].includes(o.status)),completed=orders.filter(o=>o.status==='COMPLETED'),drafts=data.filter(o=>o.status==='DRAFT');
 productionTotal.textContent=orders.length;productionActive.textContent=active.length;productionCompleted.textContent=completed.length;productionRevenue.textContent=money(completed.reduce((sum,o)=>sum+Number(o.total),0));productionDrafts.textContent=drafts.length;productionLineCount.textContent=orders.length;productionDraftCount.textContent=drafts.length;renderProduction();renderDashboard();
}
async function updateProductionStatus(select){
 const previous=productionCache.find(o=>o.id===Number(select.dataset.productionId))?.status;select.disabled=true;productionMessage.textContent='';
 const r=await api(`/api/budgets/${select.dataset.productionId}/status`,{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({status:select.value})}),data=await r.json();
 if(!r.ok){select.value=previous;productionMessage.textContent=data.message||'Não foi possível atualizar o status'}else{const index=productionCache.findIndex(o=>o.id===data.id);if(index>=0)productionCache[index]=data;productionMessage.textContent=`Ordem #${data.id} atualizada para ${productionStatusLabels[data.status]}.`;await loadProduction()}select.disabled=false;
}
document.querySelectorAll('[data-production-tab]').forEach(button=>button.onclick=()=>{productionTab=button.dataset.productionTab;document.querySelectorAll('[data-production-tab]').forEach(x=>x.classList.toggle('active',x===button));productionFilter.disabled=productionTab==='drafts';if(productionTab==='drafts')productionFilter.value='';renderProduction()});
productionSearch.oninput=renderProduction;productionFilter.onchange=renderProduction;refreshProduction.onclick=loadProduction;
async function productionDetails(id){
 const r=await api(`/api/budgets/${id}`),data=await r.json();if(!r.ok)throw Error(data.message||'Não foi possível abrir o orçamento');return data;
}
function renderProductionDetail(data){
 productionDetailTitle.textContent=`#${data.id} · ${data.title}`;productionDetailBody.innerHTML=`<div class="production-detail-summary"><div><span>Cliente</span><strong>${escapeHtml(data.customer||'Consumidor não identificado')}</strong></div><div><span>Status</span><strong>${escapeHtml(productionStatusLabels[data.status]||data.status)}</strong></div><div><span>Custo</span><strong>${money(data.costTotal)}</strong></div><div><span>Preço final</span><strong>${money(data.total)}</strong></div></div><h3>Placas do orçamento</h3><div class="production-detail-plates">${data.plates.map((p,i)=>`<div><strong>${i+1}. ${escapeHtml(p.name)}</strong><span>${escapeHtml(p.printerName||'Sem impressora')} · ${productionDuration(p.printTimeMinutes)} · ${Number(p.filamentGrams).toLocaleString('pt-BR',{minimumFractionDigits:2})} g</span><b>${money(p.total)}</b></div>`).join('')}</div>`;
 productionDetailPdf.dataset.id=data.id;productionDetailWhatsapp.dataset.id=data.id;productionDetailDialog.showModal();
}
async function downloadProductionPdf(id){
 const r=await api(`/api/budgets/${id}/pdf`);if(!r.ok){const e=await r.json().catch(()=>({}));throw Error(e.message||'Não foi possível gerar o PDF')}const blob=await r.blob(),url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=`orcamento-${id}-ravi-makers.pdf`;a.click();setTimeout(()=>URL.revokeObjectURL(url),3000);
}
async function shareProductionWhatsapp(id){
 const popup=window.open('about:blank','_blank');try{const data=await productionDetails(id),phone=String(data.customerWhatsapp||'').replace(/\D/g,'');if(!phone){popup?.close();throw Error('Cadastre o WhatsApp do cliente antes de compartilhar.')}const text=`Olá, ${data.customer||''}! Segue o orçamento #${data.id} - ${data.title}, no valor de ${money(data.total)}. O PDF pode ser enviado logo após esta mensagem.`;popup.location.href=`https://wa.me/${phone.startsWith('55')?phone:'55'+phone}?text=${encodeURIComponent(text)}`}catch(e){popup?.close();throw e}
}
async function editProduction(id){
 const data=await productionDetails(id);productionEditId.value=data.id;productionEditTitle.value=data.title;productionEditTotal.value=brlText(data.total);productionEditStatus.value=data.status;productionEditCustomer.innerHTML='<option value="">Consumidor não identificado</option>'+customerCache.filter(c=>c.active).map(c=>`<option value="${c.id}" ${String(c.id)===String(data.customerId)?'selected':''}>${escapeHtml(c.name)}</option>`).join('');productionEditError.textContent='';productionEditDialog.showModal();
}
productionRows.addEventListener('click',async e=>{const button=e.target.closest('[data-production-action]');if(!button)return;const id=Number(button.dataset.id);productionMessage.textContent='';try{if(button.dataset.productionAction==='view')renderProductionDetail(await productionDetails(id));if(button.dataset.productionAction==='edit')await editProduction(id);if(button.dataset.productionAction==='pdf')await downloadProductionPdf(id);if(button.dataset.productionAction==='duplicate'&&confirm(`Criar uma nova ordem de produção baseada no orçamento #${id}? O estoque será baixado novamente.`)){button.disabled=true;const r=await api(`/api/budgets/${id}/duplicate`,{method:'POST',successMessage:'Orçamento duplicado e enviado para produção.'}),data=await r.json();if(!r.ok)throw Error(data.message||'Não foi possível duplicar');productionMessage.textContent=`Nova ordem #${data.id} criada a partir do orçamento #${id}.`;await loadProduction()}if(button.dataset.productionAction==='delete'&&confirm(`Excluir permanentemente o orçamento #${id}? A movimentação de estoque já realizada não será revertida.`)){const r=await api(`/api/budgets/${id}`,{method:'DELETE'});if(!r.ok)throw Error('Não foi possível excluir');productionMessage.textContent=`Orçamento #${id} excluído.`;await loadProduction()}}catch(err){productionMessage.textContent=err.message}finally{button.disabled=false}});
productionEditForm.onsubmit=async e=>{e.preventDefault();productionEditError.textContent='';const id=Number(productionEditId.value),r=await api(`/api/budgets/${id}`,{method:'PUT',headers:{'Content-Type':'application/json'},body:JSON.stringify({title:productionEditTitle.value,customerId:productionEditCustomer.value?Number(productionEditCustomer.value):null,total:brlValue(productionEditTotal),status:productionEditStatus.value})}),data=await r.json();if(!r.ok){productionEditError.textContent=data.message||'Não foi possível editar';return}productionEditDialog.close();productionMessage.textContent=`Orçamento #${id} atualizado.`;await loadProduction()};
productionDetailPdf.onclick=()=>downloadProductionPdf(Number(productionDetailPdf.dataset.id)).catch(e=>productionMessage.textContent=e.message);
productionDetailWhatsapp.onclick=()=>shareProductionWhatsapp(Number(productionDetailWhatsapp.dataset.id)).catch(e=>productionMessage.textContent=e.message);
document.querySelectorAll('[data-dashboard-days]').forEach(button=>button.onclick=()=>{dashboardDays=Number(button.dataset.dashboardDays);document.querySelectorAll('[data-dashboard-days]').forEach(x=>x.classList.toggle('active',x===button));renderDashboard()});
function validationMessage(field){
 if(field.validity.valueMissing)return 'Este campo é obrigatório.';
 if(field.validity.rangeUnderflow)return `O valor mínimo permitido é ${field.min}.`;
 if(field.validity.rangeOverflow)return `O valor máximo permitido é ${field.max}.`;
 if(field.validity.typeMismatch)return field.type==='email'?'Informe um e-mail válido.':'Confira o formato informado.';
 if(field.validity.tooLong)return `Use no máximo ${field.maxLength} caracteres.`;
 if(field.validity.patternMismatch)return 'O valor informado não segue o formato esperado.';
 return 'Revise este campo.';
}
document.addEventListener('invalid',event=>{const field=event.target;if(!field.matches('input,select,textarea'))return;field.classList.add('field-invalid');let feedback=field.parentElement.querySelector(':scope > .field-validation');if(!feedback){feedback=document.createElement('small');feedback.className='field-validation';field.insertAdjacentElement('afterend',feedback)}feedback.textContent=validationMessage(field)},true);
document.addEventListener('input',event=>{const field=event.target;if(!field.matches('input,select,textarea'))return;if(field.checkValidity()){field.classList.remove('field-invalid');const feedback=field.parentElement.querySelector(':scope > .field-validation');if(feedback)feedback.remove()}else if(field.classList.contains('field-invalid')){const feedback=field.parentElement.querySelector(':scope > .field-validation');if(feedback)feedback.textContent=validationMessage(field)}});

// Relatos de bugs ficam vinculados à conta e podem incluir uma captura de tela.
const feedbackUi={
 openButtons:[document.getElementById('openFeedback'),document.getElementById('openFeedbackMobile')].filter(Boolean),
 dialog:document.getElementById('feedbackDialog'),
 form:document.getElementById('feedbackForm'),description:document.getElementById('feedbackDescription'),
 screenshot:document.getElementById('feedbackScreenshot'),preview:document.getElementById('feedbackPreview'),
 count:document.getElementById('feedbackCount'),error:document.getElementById('feedbackError'),
 submit:document.getElementById('feedbackSubmit')
};
let feedbackPreviewUrl=null;
feedbackUi.openButtons.forEach(button=>button.addEventListener('click',()=>{
 feedbackUi.error.textContent='';document.querySelector('.sidebar').classList.remove('open');feedbackUi.dialog.showModal();
}));
feedbackUi.description?.addEventListener('input',()=>feedbackUi.count.textContent=feedbackUi.description.value.length);
feedbackUi.screenshot?.addEventListener('change',()=>{
 if(feedbackPreviewUrl){URL.revokeObjectURL(feedbackPreviewUrl);feedbackPreviewUrl=null}
 const file=feedbackUi.screenshot.files[0];feedbackUi.preview.innerHTML='';feedbackUi.preview.classList.toggle('hidden',!file);
 if(!file)return;
 if(file.size>5*1024*1024){feedbackUi.screenshot.value='';feedbackUi.preview.classList.add('hidden');notify('A captura de tela deve ter no máximo 5 MB.','error');return}
 feedbackPreviewUrl=URL.createObjectURL(file);
 const image=document.createElement('img');image.alt='Prévia da captura de tela';image.src=feedbackPreviewUrl;
 image.onerror=()=>{feedbackUi.preview.classList.add('preview-error');feedbackUi.preview.textContent='Não foi possível mostrar a prévia, mas o arquivo ainda pode ser enviado.'};
 feedbackUi.preview.classList.remove('preview-error');feedbackUi.preview.appendChild(image);
});
feedbackUi.dialog?.addEventListener('close',()=>{
 if(feedbackPreviewUrl){URL.revokeObjectURL(feedbackPreviewUrl);feedbackPreviewUrl=null}
});
feedbackUi.form?.addEventListener('submit',async event=>{
 event.preventDefault();if(!feedbackUi.form.reportValidity())return;
 feedbackUi.submit.disabled=true;feedbackUi.submit.textContent='Enviando...';feedbackUi.error.textContent='';
 try{
  const data=new FormData();data.append('description',feedbackUi.description.value.trim());data.append('page',location.href);
  const file=feedbackUi.screenshot.files[0];if(file)data.append('screenshot',file);
  const response=await fetch('/api/feedback',{method:'POST',body:data});
  const result=await response.json().catch(()=>({}));
  if(!response.ok)throw Error(result.message||'Não foi possível enviar o relatório.');
  feedbackUi.form.reset();feedbackUi.count.textContent='0';feedbackUi.preview.innerHTML='';feedbackUi.preview.classList.add('hidden');feedbackUi.dialog.close();
  notify(`Relatório #${result.id} enviado. Obrigado pelo feedback!`);
 }catch(error){feedbackUi.error.textContent=error.message;notify(error.message,'error')}
 finally{feedbackUi.submit.disabled=false;feedbackUi.submit.textContent='Enviar relatório'}
});

let supportReports=[];
const supportStatusLabels={OPEN:'Aberto',ANALYZING:'Em análise',RESOLVED:'Resolvido'};
async function initializeSupportAccess(){
 try{
  const response=await api('/api/support/feedback/access'),data=await response.json();
  supportAdmin=response.ok&&data.allowed===true;
 }catch{supportAdmin=false}
 supportReportsLink?.classList.toggle('hidden',!supportAdmin);
 if(supportAdmin){await loadSupportReports();if(location.hash==='#relatorios')routeView()}
 else if(location.hash==='#relatorios')routeView();
}
async function loadSupportReports(){
 if(!supportAdmin)return;
 supportReportMessage.textContent='Carregando relatórios...';
 try{
  const response=await api('/api/support/feedback'),data=await response.json();
  if(!response.ok)throw Error(data.message||'Não foi possível carregar os relatórios.');
  supportReports=data;supportReportMessage.textContent='';renderSupportReports();
 }catch(error){supportReportMessage.textContent=error.message}
}
function renderSupportReports(){
 const search=supportReportSearch.value.trim().toLowerCase(),status=supportReportFilter.value;
 const rows=supportReports.filter(report=>(!status||report.status===status)&&(!search||`${report.reporterName} ${report.reporterEmail} ${report.description}`.toLowerCase().includes(search)));
 supportOpenCount.textContent=supportReports.filter(report=>report.status==='OPEN').length;
 supportAnalyzingCount.textContent=supportReports.filter(report=>report.status==='ANALYZING').length;
 supportResolvedCount.textContent=supportReports.filter(report=>report.status==='RESOLVED').length;
 supportReportList.innerHTML=rows.length?rows.map(report=>`<article class="panel support-report-card">
  <div class="support-report-head"><div><span>#${report.id} · ${new Date(report.createdAt).toLocaleString('pt-BR')}</span><strong>${escapeHtml(report.reporterName)}</strong><small>${escapeHtml(report.reporterEmail)}</small></div><span class="support-status ${report.status}">${supportStatusLabels[report.status]||report.status}</span></div>
  <p>${escapeHtml(report.description)}</p>${safeHttpUrl(report.page)?`<a class="support-report-page" href="${escapeHtml(safeHttpUrl(report.page))}" target="_blank" rel="noopener">Página informada ↗</a>`:''}
  <div class="support-report-actions">${report.hasScreenshot?`<a class="support-screenshot-link" href="/api/support/feedback/${report.id}/screenshot" target="_blank">▧ Abrir print</a>`:'<span class="muted">Sem print</span>'}
  <select data-support-status="${report.id}"><option value="OPEN" ${report.status==='OPEN'?'selected':''}>Aberto</option><option value="ANALYZING" ${report.status==='ANALYZING'?'selected':''}>Em análise</option><option value="RESOLVED" ${report.status==='RESOLVED'?'selected':''}>Resolvido</option></select>
  <button class="danger-outline" type="button" data-support-delete="${report.id}">Excluir</button></div></article>`).join(''):'<div class="panel dashboard-empty">Nenhum relatório encontrado.</div>';
}
supportReportSearch?.addEventListener('input',renderSupportReports);
supportReportFilter?.addEventListener('change',renderSupportReports);
refreshSupportReports?.addEventListener('click',loadSupportReports);
supportReportList?.addEventListener('change',async event=>{
 const select=event.target.closest('[data-support-status]');if(!select)return;
 const response=await api(`/api/support/feedback/${select.dataset.supportStatus}/status`,{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({status:select.value})}),data=await response.json();
 if(!response.ok){notify(data.message||'Não foi possível alterar o status.','error');await loadSupportReports();return}
 notify('Status do relatório atualizado.');await loadSupportReports();
});
supportReportList?.addEventListener('click',async event=>{
 const button=event.target.closest('[data-support-delete]');if(!button||!confirm('Excluir permanentemente este relatório?'))return;
 const response=await api(`/api/support/feedback/${button.dataset.supportDelete}`,{method:'DELETE'});
 if(!response.ok){notify('Não foi possível excluir o relatório.','error');return}
 notify('Relatório excluído.');await loadSupportReports();
});
function escapeHtml(s){const d=document.createElement('div');d.textContent=s;return d.innerHTML}
function safeHttpUrl(value){try{const url=new URL(String(value||''));return ['http:','https:'].includes(url.protocol)?url.href:''}catch{return ''}}

const onboarding={
 steps:[
  {target:'.dashboard-heading',title:'Bem-vindo à RAVI MAKERS',text:'Este painel reúne os números mais importantes da sua operação de impressão 3D.'},
  {target:'[data-view-link="inventory"]',title:'Organize seu estoque',text:'Cadastre filamentos, acompanhe quantidades e mantenha os custos de material atualizados.'},
  {target:'[data-view-link="quote"]',title:'Calcule novos projetos',text:'Envie arquivos G-code para calcular material, tempo, custos e preço de venda.'},
  {target:'[data-view-link="production"]',title:'Acompanhe a produção',text:'Veja o que está na fila, em impressão ou concluído e controle os prazos de entrega.'},
  {target:'[data-view-link="settings"]',title:'Configure sua operação',text:'Defina energia, mão de obra, margem, manutenção e os dados da sua empresa.'},
  {target:'#openFeedback',title:'Conte com o suporte',text:'Se encontrar algum problema, envie uma descrição e uma captura de tela por aqui.'}
 ],
 index:0,active:false,
 key:'ravi_onboarding_v1',
 start(force=false){
  if(!force&&document.cookie.split('; ').includes(`${this.key}=done`))return;
  this.active=true;this.index=0;this.ensureUi();this.show();
 },
 ensureUi(){
  if(document.getElementById('onboardingCard'))return;
  document.body.insertAdjacentHTML('beforeend',`<div id="onboardingShade" class="onboarding-shade hidden"></div><div id="onboardingFocus" class="onboarding-focus hidden"></div><section id="onboardingCard" class="onboarding-card hidden" role="dialog" aria-modal="true" aria-labelledby="onboardingTitle"><div class="onboarding-progress"><span id="onboardingStep"></span><button id="onboardingSkip" type="button">Pular tutorial</button></div><h2 id="onboardingTitle"></h2><p id="onboardingText"></p><div class="onboarding-dots" id="onboardingDots"></div><div class="onboarding-actions"><button id="onboardingBack" class="ghost" type="button">Voltar</button><button id="onboardingNext" type="button">Próximo</button></div></section>`);
  onboardingBack.onclick=()=>this.move(-1);onboardingNext.onclick=()=>this.move(1);onboardingSkip.onclick=()=>this.finish();
  window.addEventListener('resize',()=>this.active&&this.position());
  document.addEventListener('keydown',event=>{if(!this.active)return;if(event.key==='Escape')this.finish();if(event.key==='ArrowRight')this.move(1);if(event.key==='ArrowLeft')this.move(-1)});
 },
 show(){
  const step=this.steps[this.index],target=document.querySelector(step.target);if(!target){this.move(1);return}
  if(innerWidth<=960&&target.closest('.sidebar'))document.querySelector('.sidebar').classList.add('open');
  onboardingTitle.textContent=step.title;onboardingText.textContent=step.text;onboardingStep.textContent=`PASSO ${this.index+1} DE ${this.steps.length}`;
  onboardingDots.innerHTML=this.steps.map((_,i)=>`<i class="${i===this.index?'active':''}"></i>`).join('');
  onboardingBack.disabled=this.index===0;onboardingNext.textContent=this.index===this.steps.length-1?'Concluir':'Próximo';
  onboardingShade.classList.remove('hidden');onboardingFocus.classList.remove('hidden');onboardingCard.classList.remove('hidden');
  target.scrollIntoView({block:'center',behavior:'smooth'});setTimeout(()=>this.position(),260);
 },
 position(){
  const target=document.querySelector(this.steps[this.index].target);if(!target)return;const rect=target.getBoundingClientRect(),pad=7,focus=onboardingFocus,card=onboardingCard;
  Object.assign(focus.style,{left:`${Math.max(5,rect.left-pad)}px`,top:`${Math.max(5,rect.top-pad)}px`,width:`${Math.min(innerWidth-10,rect.width+pad*2)}px`,height:`${Math.min(innerHeight-10,rect.height+pad*2)}px`});
  const cardWidth=Math.min(390,innerWidth-24),spaceBelow=innerHeight-rect.bottom,top=spaceBelow>290?rect.bottom+18:Math.max(12,rect.top-card.offsetHeight-18);
  Object.assign(card.style,{width:`${cardWidth}px`,left:`${Math.min(Math.max(12,rect.left),innerWidth-cardWidth-12)}px`,top:`${top}px`});
 },
 move(delta){
  const next=this.index+delta;if(next<0)return;if(next>=this.steps.length){this.finish();return}this.index=next;this.show();
 },
 finish(){
  this.active=false;document.cookie=`${this.key}=done; Max-Age=31536000; Path=/; SameSite=Lax; Secure`;
  onboardingShade?.classList.add('hidden');onboardingFocus?.classList.add('hidden');onboardingCard?.classList.add('hidden');document.querySelector('.sidebar').classList.remove('open');
 }
};
document.getElementById('openTutorial')?.addEventListener('click',()=>onboarding.start(true));
routeView();initializeSupportAccess();load();loadPrinters();loadInventory();loadConsumables();loadCustomers();loadPricingSettings();loadCompanySettings();loadProduction();loadProducts();loadCatalogInfo();loadCoupons();setInterval(loadPrinters,15000);
setTimeout(()=>onboarding.start(),700);
