import http from 'node:http';
import crypto from 'node:crypto';
import { URL } from 'node:url';
import { WebSocketServer } from 'ws';

const PORT = Number(process.env.PORT || 8080);
const SUPABASE_URL = (process.env.SUPABASE_URL || '').replace(/\/$/, '');
const SUPABASE_SERVICE_ROLE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || '';
const MAX_BODY = 40 * 1024 * 1024;
const childSockets = new Map();
const pending = new Map();

if (!SUPABASE_URL || !SUPABASE_SERVICE_ROLE_KEY) {
  console.error('Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY');
  process.exit(1);
}

async function db(path, options = {}) {
  const r = await fetch(`${SUPABASE_URL}/rest/v1/${path}`, {
    ...options,
    headers: {
      apikey: SUPABASE_SERVICE_ROLE_KEY,
      Authorization: `Bearer ${SUPABASE_SERVICE_ROLE_KEY}`,
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
  });
  const text = await r.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = text; }
  if (!r.ok) throw new Error(`Supabase ${r.status}: ${typeof data === 'string' ? data : JSON.stringify(data)}`);
  return data;
}

async function selectOne(table, query) {
  const rows = await db(`${table}?${query}&limit=1`, { headers: { Accept: 'application/json' } });
  return Array.isArray(rows) ? rows[0] : null;
}

function json(res, status, body) {
  const b = Buffer.from(JSON.stringify(body));
  res.writeHead(status, {'Content-Type':'application/json','Content-Length':b.length});
  res.end(b);
}
function body(req) {
  return new Promise((resolve,reject)=>{
    const chunks=[]; let n=0;
    req.on('data',c=>{ n+=c.length; if(n>MAX_BODY){req.destroy();reject(new Error('body too large'));} else chunks.push(c); });
    req.on('end',()=>resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error',reject);
  });
}
function bearer(req) {
  const h=req.headers.authorization||'';
  return h.startsWith('Bearer ') ? h.slice(7).trim() : '';
}
function newCode() { return crypto.randomBytes(6).toString('base64url').replace(/[-_]/g,'').toUpperCase().slice(0,8); }
async function uniqueCode() {
  for (let i=0;i<20;i++) {
    const code = newCode();
    const found = await selectOne('parents', `pairing_code=eq.${encodeURIComponent(code)}`);
    if (!found) return code;
  }
  throw new Error('could not allocate pairing code');
}
async function parentByToken(token) {
  if (!token) return null;
  return selectOne('parents', `parent_token=eq.${encodeURIComponent(token)}`);
}
async function parentById(id) {
  return selectOne('parents', `id=eq.${encodeURIComponent(id)}`);
}

const server=http.createServer(async (req,res)=>{
  const u=new URL(req.url,`http://${req.headers.host}`);
  try {
    if(req.method==='GET' && u.pathname==='/health') return json(res,200,{ok:true,storage:'supabase'});

    if(req.method==='POST' && u.pathname==='/api/parent/session'){
      const x=JSON.parse(await body(req)||'{}');
      if(!x.parentId||!x.parentToken) return json(res,400,{ok:false,error:'missing parent credentials'});
      const code = await uniqueCode();
      const now = new Date().toISOString();
      const existing = await parentById(x.parentId);
      if (existing) {
        await db(`parents?id=eq.${encodeURIComponent(x.parentId)}`, {method:'PATCH', body:JSON.stringify({parent_token:x.parentToken,pairing_code:code,updated_at:now})});
      } else {
        await db('parents', {method:'POST', body:JSON.stringify({id:x.parentId,parent_token:x.parentToken,pairing_code:code,created_at:now,updated_at:now})});
      }
      return json(res,200,{ok:true,code});
    }

    if(req.method==='GET' && u.pathname==='/api/parent/devices'){
      const p=await parentByToken(bearer(req));
      if(!p) return json(res,401,{ok:false,error:'unauthorized'});
      const devices=await db(`devices?parent_id=eq.${encodeURIComponent(p.id)}&select=id,name,model,os,last_seen,created_at&order=created_at.asc`);
      return json(res,200,{ok:true,devices:(devices||[]).map(d=>({id:d.id,name:d.name,model:d.model,os:d.os,online:childSockets.has(d.id),lastSeen:d.last_seen}))});
    }

    if(req.method==='POST' && u.pathname==='/api/child/pair'){
      const x=JSON.parse(await body(req)||'{}');
      const code=String(x.code||'').toUpperCase();
      const p=await selectOne('parents',`pairing_code=eq.${encodeURIComponent(code)}`);
      if(!p) return json(res,401,{ok:false,error:'invalid pairing code'});
      if(!x.deviceId||!x.childToken) return json(res,400,{ok:false,error:'missing child credentials'});
      const now=new Date().toISOString();
      const existing=await selectOne('devices',`id=eq.${encodeURIComponent(x.deviceId)}`);
      const payload={id:x.deviceId,parent_id:p.id,child_token:x.childToken,name:x.name||'Child Device',model:x.model||'Android',os:x.os||'Android',last_seen:now,updated_at:now};
      if(existing) await db(`devices?id=eq.${encodeURIComponent(x.deviceId)}`,{method:'PATCH',body:JSON.stringify(payload)});
      else await db('devices',{method:'POST',body:JSON.stringify({...payload,created_at:now})});
      // Pairing code is permanent until the parent explicitly changes it.
      return json(res,200,{ok:true,deviceId:x.deviceId});
    }

    if(['GET','POST','PUT','PATCH','DELETE'].includes(req.method) && u.pathname.startsWith('/tunnel/')){
      const token=bearer(req); const deviceId=u.pathname.split('/')[2];
      const d=await selectOne('devices',`id=eq.${encodeURIComponent(deviceId)}`); const p=d&&await parentById(d.parent_id);
      if(!d||!p||p.parent_token!==token) return json(res,401,{ok:false,error:'unauthorized'});
      const ws=childSockets.get(deviceId); if(!ws||ws.readyState!==1) return json(res,503,{ok:false,error:'child offline'});
      const raw=await body(req); const requestId=crypto.randomUUID();
      const payload={type:'http_request',requestId,method:req.method,path:'/'+u.pathname.split('/').slice(3).join('/')+(u.search||''),bodyBase64:Buffer.from(raw).toString('base64')};
      const promise=new Promise(resolve=>pending.set(requestId,{resolve,timer:setTimeout(()=>{pending.delete(requestId);resolve(null)},35000)}));
      ws.send(JSON.stringify(payload)); const r=await promise;
      if(!r) return json(res,504,{ok:false,error:'child request timeout'});
      const bytes=Buffer.from(r.bodyBase64||'','base64');
      res.writeHead(r.status||502,{'Content-Type':r.contentType||'application/octet-stream','Content-Length':bytes.length}); return res.end(bytes);
    }
    return json(res,404,{ok:false,error:'not found'});
  } catch(e) { console.error(e); return json(res,500,{ok:false,error:e.message||'server error'}); }
});

const wss=new WebSocketServer({server,path:'/ws',maxPayload:MAX_BODY});
wss.on('connection',(ws,req)=>{
  (async()=>{
    try{
      const u=new URL(req.url,`http://${req.headers.host}`); const id=u.searchParams.get('id'); const role=u.searchParams.get('role');
      const auth=(req.headers.authorization||'').replace(/^Bearer\s+/,'');
      const d=await selectOne('devices',`id=eq.${encodeURIComponent(id||'')}`);
      if(role!=='child'||!d||d.child_token!==auth){ws.close(1008,'unauthorized');return;}
      const old=childSockets.get(id); if(old) try{old.close(4000,'replaced')}catch{}
      childSockets.set(id,ws); await db(`devices?id=eq.${encodeURIComponent(id)}`,{method:'PATCH',body:JSON.stringify({last_seen:new Date().toISOString(),updated_at:new Date().toISOString()})});
      ws.on('message',raw=>{try{const x=JSON.parse(raw.toString());if(x.type==='http_response'&&pending.has(x.requestId)){const p=pending.get(x.requestId);clearTimeout(p.timer);pending.delete(x.requestId);p.resolve(x);}}catch{}});
      ws.on('close',async()=>{if(childSockets.get(id)===ws)childSockets.delete(id);try{await db(`devices?id=eq.${encodeURIComponent(id)}`,{method:'PATCH',body:JSON.stringify({last_seen:new Date().toISOString(),updated_at:new Date().toISOString()})});}catch{}});
    }catch(e){console.error(e);try{ws.close(1011,'error')}catch{}}
  })();
});

server.listen(PORT,()=>console.log(`Game Centre Supabase relay listening on ${PORT}`));
