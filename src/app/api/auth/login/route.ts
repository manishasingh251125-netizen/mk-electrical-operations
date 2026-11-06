import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";
import { adminClient, loginEmail } from "@/lib/supabase/admin";

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const username = String(body.username ?? "").trim().toLowerCase();
    const password = String(body.password ?? "");
    if (!username || !password) return NextResponse.json({ error: "Enter username and password." }, { status: 400 });

    const admin = adminClient();
    const { data: profile, error: profileError } = await admin.from("profiles")
      .select("id,is_active").eq("username", username).maybeSingle();
    if (profileError || !profile || !profile.is_active) {
      return NextResponse.json({ error: "Invalid username or password." }, { status: 401 });
    }

    const supabase = await createClient();
    const { error } = await supabase.auth.signInWithPassword({ email: loginEmail(username), password });
    if (error) return NextResponse.json({ error: "Invalid username or password." }, { status: 401 });
    return NextResponse.json({ ok: true });
  } catch {
    return NextResponse.json({ error: "Unable to sign in. Please try again." }, { status: 500 });
  }
}
