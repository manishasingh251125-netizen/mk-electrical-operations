"use client";
import {useEffect,useState} from "react";
type Employee={id:string;employee_code:string;full_name:string;username:string;designation:string;role:string;is_active:boolean};
export default function EmployeeDirectory(){
 const [items,setItems]=useState<Employee[]>([]);const [selected,setSelected]=useState<Employee|null>(null);const [error,setError]=useState("");
 useEffect(()=>{fetch("/api/employees").then(async r=>{const d=await r.json();if(!r.ok)throw new Error(d.error||"Unable to load employee IDs");setItems(d.data||[])}).catch(e=>setError(e.message));},[]);
 return <main className="shell"><section className="module-head"><div><div className="brand">MK ELECTRICALS</div><h1 className="title">Employee ID cards</h1><p className="muted">Preview each employee’s company ID and print or save it as a PDF.</p></div><span className="user-chip">{items.length} employees</span></section>
 {error&&<p className="note">{error}</p>}
 <section className="panel"><div className="section-row"><h2>Employee directory</h2><span className="muted">Select an ID to preview</span></div>
 {items.length===0?<p className="muted">No employee records available.</p>:<div className="employee-grid">{items.map(x=><article className="employee-tile" key={x.id}><div><strong>{x.full_name}</strong><p className="muted">{x.employee_code||"ID not assigned"} · {x.designation||"Employee"}</p><span className="muted">{x.is_active?"Active":"Inactive"}</span></div><button className="btn secondary" onClick={()=>setSelected(x)}>Preview ID</button></article>)}</div>}</section>
 {selected&&<div className="id-overlay"><div className="id-dialog"><div className="section-row no-print"><strong>Employee ID preview</strong><button className="btn secondary" onClick={()=>setSelected(null)}>Close</button></div>
 <article className="id-card"><img className="id-logo" src="/mk-electricals-logo.svg" alt="MK Electricals logo"/><div className="id-band">EMPLOYEE IDENTITY CARD</div><div className="id-details"><div className="id-avatar">{selected.full_name.split(/\s+/).map(s=>s[0]).slice(0,2).join("").toUpperCase()}</div><div><h2>{selected.full_name}</h2><p>{selected.designation||"Employee"}</p><strong>{selected.employee_code||"ID pending"}</strong></div></div><div className="id-footer"><span>MK ELECTRICALS</span><span>{selected.role.toUpperCase()}</span></div></article>
 <div className="id-actions no-print"><button className="btn" onClick={()=>window.print()}>Print / Save PDF</button></div></div></div>}
 </main>;
}