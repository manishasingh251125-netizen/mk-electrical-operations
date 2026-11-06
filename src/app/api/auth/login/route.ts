import { NextResponse } from "next/server";
import { createClient } from "@/lib/supabase/server";
import { loginEmail } from "@/lib/supabase/admin";

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const username = String(body.username ?? "").trim().toLowerCase();
    const password = String(body.password ?? "");
    if (!username || !password) return NextResponse.json({ error: "Enter username and password." }, { status: 400 });
    const supabase = await createClient();
    const { error } = await supabase.auth.signInWithPassword({ email: loginEmail(username), password });
    if (error) return NextResponse.json({ error: "Invalid username or password." }, { status: 401 });
    const { data: { user } } = await supabase.auth.getUser();
    if (!user) return NextResponse.json({ error: "Unable to verify your account." }, { status: 401 });
    const { data: profile, error: profileError } = await supabase.from("profiles")
      .select("is_active").eq("id", user.id).maybeSingle();
    if (profileError || !profile?.is_active) {
      await supabase.auth.signOut();
      return NextResponse.json({ error: "This account is inactive. Contact the owner." }, { status: 403 });
    }
    return NextResponse.json({ ok: true });
  } catch {
    return NextResponse.json({ error: "Unable to sign in. Please try again." }, { status: 500 });
  }
}
