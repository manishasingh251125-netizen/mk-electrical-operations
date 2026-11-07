"use client";

import { useState } from "react";

export default function SignOutButton() {
  const [busy, setBusy] = useState(false);
  async function signOut() {
    if (busy) return;
    setBusy(true);
    try {
      await fetch("/api/auth/logout", { method: "POST" });
    } finally {
      window.location.assign("/");
    }
  }
  return (
    <button
      type="button"
      onClick={signOut}
      disabled={busy}
      style={{
        color: "white",
        background: "#7b263d",
        border: "1px solid rgba(255,255,255,.25)",
        borderRadius: 7,
        padding: "7px 11px",
        fontSize: 13,
        cursor: busy ? "wait" : "pointer",
      }}
    >
      {busy ? "Signing out…" : "Sign out"}
    </button>
  );
}
