import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";
import { requireOwner } from "@/lib/auth";
export async function GET(){const guard=await requireOwner();if("error" in guard)return guard.error;const s=await createClient();const {data,error}=await s.from("profiles").select("id,employee_code,full_name,username,designation,role,is_active,created_at").order("created_at");if(error)return NextResponse.json({error:error.message},{status:400});return NextResponse.json({data});}
