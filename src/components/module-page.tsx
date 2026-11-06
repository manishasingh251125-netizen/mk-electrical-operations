import { createClient } from "@/lib/supabase/server";
type Section = "dashboard" | "attendance" | "tasks" | "materials" | "updates" | "reports" | "employees";
const config: Record<Section,{title:string;table:string;description:string;columns:string[]}> = {
 dashboard:{title:"Operations Dashboard",table:"attendance",description:"Today's attendance and field activity at a glance.",columns:["clock_in","clock_out","worker_id"]},
 attendance:{title:"Attendance",table:"attendance",description:"Clock-in and clock-out records for your team.",columns:["clock_in","clock_out","worker_id","notes"]},
 tasks:{title:"Daily Tasks",table:"daily_tasks",description:"Work assigned to employees and current progress.",columns:["work_date","title","status","hours","worker_id"]},
 materials:{title:"Materials",table:"material_transactions",description:"Material issued, consumed and returned.",columns:["transaction_date","material_name","quantity","unit","transaction_type"]},
 updates:{title:"Work Updates",table:"work_updates",description:"Daily field updates shared by employees.",columns:["created_at","update_text","worker_id"]},
 reports:{title:"Reports",table:"attendance",description:"Attendance and work records available to your account.",columns:["clock_in","clock_out","worker_id","notes"]},
 employees:{title:"Employee IDs",table:"profiles",description:"Employee directory and account status.",columns:["employee_code","full_name","username","designation","role","is_active"]}
};
function display(v:unknown){if(v===null||v===undefined||v==="")return "—";if(typeof v==="boolean")return v?"Active":"Inactive";if(typeof v==="string"&&v.includes("T")){const d=new Date(v);if(!Number.isNaN(d.getTime()))return d.toLocaleString("en-IN",{dateStyle:"medium",timeStyle:"short"});}return String(v);}
export default async function ModulePage({section}:{section:Section}){
 const supabase=await createClient(); const {data:{user}}=await supabase.auth.getUser();
 if(!user)return <main className="shell"><section className="panel"><h1 className="title">Please sign in</h1><a href="/">Return to login</a></section></main>;
 const {data:profile}=await supabase.from("profiles").select("full_name,role,is_active").eq("id",user.id).maybeSingle();
 if(!profile?.is_active)return <main className="shell"><section className="panel"><h1 className="title">Account unavailable</h1><a href="/">Return to login</a></section></main>;
 const item=config[section]; const query=supabase.from(item.table).select("*").order(section==="tasks"?"work_date":section==="materials"?"transaction_date":"created_at",{ascending:false}).limit(50);
 const {data, error}=await query;
 const rows=(data??[]) as Record<string,unknown>[];
 return <main className="shell"><section className="module-head"><div><div className="brand">MK ELECTRICALS</div><h1 className="title">{item.title}</h1><p className="muted">{item.description}</p></div><div className="user-chip">{profile.full_name} · {profile.role}</div></section>
 <section className="panel"><div className="section-row"><h2>Recent records</h2><span className="muted">{rows.length} shown</span></div>
 {error?<p className="note">Unable to load records. Please check account access and database settings.</p>:rows.length===0?<p className="muted">No records yet. New activity will appear here.</p>:<div className="table-wrap"><table><thead><tr>{item.columns.map(c=><th key={c}>{c.replaceAll("_"," ")}</th>)}</tr></thead><tbody>{rows.map((row,i)=><tr key={String(row.id??i)}>{item.columns.map(c=><td key={c}>{display(row[c])}</td>)}</tr>)}</tbody></table></div>}</section></main>;
}
