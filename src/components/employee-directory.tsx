"use client";
import {useEffect,useState} from "react";
type Employee={id:string;employee_code:string;full_name:string;username:string;designation:string;role:string;is_active:boolean;residential_address?:string|null;joining_date?:string|null;verification_code?:string|null;id_serial?:string|null};
const companyAddress="Office No. 2, Dattakrupa Building, 20/2 Kashinath Patil Nagar, Dhankawadi, Pune – 411043.";
export default function EmployeeDirectory(){
 const [items,setItems]=useState<Employee[]>([]),[selected,setSelected]=useState<Employee|null>(null),[error,setError]=useState(""),[saving,setSaving]=useState(false);
 const [form,setForm]=useState({full_name:"",employee_code:"",username:"",password:"",designation:"",joining_date:"",residential_address:""});
 async function load(){const r=await fetch("/api/employees");const d=await r.json();if(!r.ok)throw new Error(d.error||"Unable to load employees");setItems(d.data||[]);}
 useEffect(()=>{load().catch(e=>setError(e.message));},[]);
 async function add(e:React.FormEvent){e.preventDefault();setSaving(true);setError("");try{const r=await fetch("/api/employees",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(form)});const d=await r.json();if(!r.ok)throw new Error(d.error||"Unable to add employee");setForm({full_name:"",employee_code:"",username:"",password:"",designation:"",joining_date:"",residential_address:""});await load();}catch(e:any){setError(e.message)}finally{setSaving(false)}}
 const verifyUrl=selected?.verification_code?typeof window!=="undefined"?window.location.origin+"/verify/"+selected.verification_code:"": "";
 const qrUrl=verifyUrl?"https://api.qrserver.com/v1/create-qr-code/?size=180x180&margin=5&data="+encodeURIComponent(verifyUrl):"";
 return <main className="shell"><section className="module-head"><div><div className="brand">MK ELECTRICALS</div><h1 className="title">Employee IDs</h1><p className="muted">Create employee accounts and preview two-sided, verifiable ID cards.</p></div><span className="user-chip">{items.length} employees</span></section>
 {error&&<p className="note">{error}</p>}
 <section className="panel"><h2>Add employee</h2><form onSubmit={add} className="form-grid">
 <label className="field">Full name<input required value={form.full_name} onChange={e=>setForm({...form,full_name:e.target.value})}/></label>
 <label className="field">Employee code / ID number<input required value={form.employee_code} onChange={e=>setForm({...form,employee_code:e.target.value})}/></label>
 <label className="field">Username<input required autoCapitalize="none" value={form.username} onChange={e=>setForm({...form,username:e.target.value})}/></label>
 <label className="field">Temporary password<input required type="password" minLength={8} value={form.password} onChange={e=>setForm({...form,password:e.target.value})}/></label>
 <label className="field">Designation<input required value={form.designation} onChange={e=>setForm({...form,designation:e.target.value})}/></label>
 <label className="field">Joining date<input type="date" value={form.joining_date} onChange={e=>setForm({...form,joining_date:e.target.value})}/></label>
 <label className="field" style={{gridColumn:"1/-1"}}>Residential address<textarea rows={2} value={form.residential_address} onChange={e=>setForm({...form,residential_address:e.target.value})}/></label>
 <button className="btn" disabled={saving}>{saving?"Creating account…":"Add employee"}</button>
 </form><p className="muted">Login is username + password. Use a unique employee code and a password of at least 8 characters.</p></section>
 <section className="panel" style={{marginTop:18}}><div className="section-row"><h2>Employee directory</h2><span className="muted">Select an ID to preview</span></div>
 {items.length===0?<p className="muted">No employee records available.</p>:<div className="employee-grid">{items.map(x=><article className="employee-tile" key={x.id}><div><strong>{x.full_name}</strong><p className="muted">{x.employee_code||"ID not assigned"} · {x.designation||"Employee"}</p><span className="muted">{x.is_active?"Active":"Inactive"}</span></div><button className="btn secondary" onClick={()=>setSelected(x)}>Preview ID</button></article>)}</div>}</section>
 {selected&&<div className="id-overlay"><div className="id-dialog id-dialog-wide"><div className="section-row no-print"><strong>Two-sided employee ID</strong><button className="btn secondary" onClick={()=>setSelected(null)}>Close</button></div><div className="id-sides">
 <article className="id-card id-front"><img className="id-logo" src="/mk-electricals-logo.jpg" alt="MK Electricals logo"/><div className="id-band">EMPLOYEE IDENTITY CARD</div><div className="id-details"><div className="id-avatar">{selected.full_name.split(/\s+/).map(s=>s[0]).slice(0,2).join("").toUpperCase()}</div><div><h2>{selected.full_name}</h2><p>{selected.designation||"Employee"}</p><strong>{selected.employee_code||"ID pending"}</strong></div></div><div className="id-footer"><span>MK ELECTRICALS</span><span>{selected.id_serial||"MK-"+selected.employee_code}</span></div></article>
 <article className="id-card id-back"><div className="id-band">EMPLOYEE DETAILS & VERIFICATION</div><div className="id-back-body"><div className="id-address"><b>Residential address</b><p>{selected.residential_address||"Not provided"}</p><b>Company address</b><p>{companyAddress}</p><p><b>Joining date:</b> {selected.joining_date||"Not provided"}</p><p><b>Card serial:</b> {selected.id_serial||"Pending"}</p></div><div className="id-qr">{qrUrl?<img src={qrUrl} alt="QR code for employee verification"/>:<span>QR pending</span>}<small>Scan to verify ID</small><div className="seal">MK<br/>SEAL</div></div></div><div className="id-footer"><span>OFFICIAL COMPANY ID</span><span>IF FOUND, RETURN TO OFFICE</span></div></article></div>
 <div className="id-actions no-print"><button className="btn" onClick={()=>window.print()}>Print / Save both sides as PDF</button></div></div></div>}
 </main>;
}
