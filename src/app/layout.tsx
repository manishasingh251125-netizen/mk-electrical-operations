import type {Metadata} from "next";
import "./globals.css";
export const metadata:Metadata={title:"MK Electricals Operations"};
export default function RootLayout({children}:{children:React.ReactNode}){
 const links=[["Dashboard","/dashboard"],["Attendance","/attendance"],["Tasks","/tasks"],["Materials","/materials"],["Updates","/updates"],["Reports","/reports"],["Employee IDs","/employees"],["Sites","/sites"]];
 return <html lang="en"><body>
 <header style={{background:"#10243a",color:"white",padding:"12px 16px",display:"flex",flexWrap:"wrap",alignItems:"center",gap:12}}>
  <a href="/dashboard" className="nav-brand"><img src="/mk-electricals-logo.jpg" alt="MK Electricals" /><span>MK Electricals</span></a>
  <nav style={{display:"flex",flexWrap:"wrap",gap:6}}>
   {links.map(([label,href])=><a key={href} href={href} style={{color:"white",textDecoration:"none",fontSize:13,padding:"7px 9px",borderRadius:7,background:"rgba(255,255,255,.1)"}}>{label}</a>)}
  </nav>
 </header>{children}
 </body></html>
}