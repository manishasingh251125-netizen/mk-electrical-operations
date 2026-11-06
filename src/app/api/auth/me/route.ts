import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";

export async function GET() {
  const supabase = await createClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return NextResponse.json({ profile: null }, { status: 401 });
  const { data: profile, error } = await supabase.from("profiles")
    .select("id,username,full_name,role,is_active,employee_code,designation,photo_path")
    .eq("id", user.id).maybeSingle();
  if (error || !profile || !profile.is_active) {
    await supabase.auth.signOut();
    return NextResponse.json({ profile: null }, { status: 403 });
  }
  return NextResponse.json({ profile });
}
