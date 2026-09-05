import { serve } from "https://deno.land/std@0.224.0/http/server.ts";
const BASE_URL = "https://yisol-idm-vton.hf.space";
const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization,apikey,content-type",
  "Access-Control-Allow-Methods": "POST,OPTIONS",
};
serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const { personB64, garmentB64, prompt = "a photo of a garment" } = await req.json();
    if (!personB64 || !garmentB64) throw new Error("personB64/garmentB64 required");
    const token = Deno.env.get("HF_TOKEN") ?? Deno.env.get("HF_TOKEN2") ?? "";
    const auth = token ? { Authorization: `Bearer ${token}` } : {};
    const upload = async (b64: string, name: string) => {
      const bytes = Uint8Array.from(atob(b64.split(",").pop()!), (c) => c.charCodeAt(0));
      const blob = new Blob([bytes], { type: "image/jpeg" });
      const fd = new FormData(); fd.append("files", blob, name);
      const id = crypto.randomUUID().replaceAll("-", "");
      const r = await fetch(`${BASE_URL}/upload?upload_id=${id}`, { method: "POST", headers: auth, body: fd });
      const t = await r.text(); if (!r.ok) throw new Error(`Upload ${r.status}:${t}`);
      return JSON.parse(t)[0];
    };
    const pPath = await upload(personB64, "person.jpg");
    const gPath = await upload(garmentB64, "garment.jpg");
    const hash = crypto.randomUUID().replaceAll("-", "");
    const payload = { fn_index: 2, session_hash: hash, data: [{ background: { path: pPath }, layers: [], composite: { path: pPath } }, { path: gPath }, prompt, true, false, 30, Math.floor(Math.random() * 2147483647)] };
    const j = await fetch(`${BASE_URL}/queue/join`, { method: "POST", headers: { ...auth, "Content-Type": "application/json" }, body: JSON.stringify(payload) });
    const jt = await j.text(); if (!j.ok) throw new Error(`Join ${j.status}:${jt}`);
    const s = await fetch(`${BASE_URL}/queue/data?session_hash=${hash}`, { headers: { ...auth, Accept: "text/event-stream" } });
    if (!s.body) throw new Error("No stream");
    const r = s.body.getReader(), dec = new TextDecoder(); let buf = "", url: string | null = null;
    const deadline = Date.now() + 8 * 60 * 1000;
    while (Date.now() < deadline) {
      const { done, value } = await r.read(); if (done) break;
      buf += dec.decode(value, { stream: true }); const lines = buf.split("\n"); buf = lines.pop() ?? "";
      for (const l of lines) {
        if (!l.startsWith("data: ")) continue;
        const d = l.slice(5).trim(); if (!d) continue;
        try {
          const ev = JSON.parse(d);
          if (ev.msg?.includes("complete")) {
            const out = ev.output?.data ?? ev.data ?? ev.output;
            if (!out || (Array.isArray(out) && out.length === 0)) {
              if (ev.success === false) throw new Error(`HF upstream no data: ${d} - Space sleeping or queue full, retry`);
              continue;
            }
            const first = Array.isArray(out) ? out[0] : out;
            const raw = typeof first === "string" ? first : (first?.url ?? first?.path ?? first?.name ?? "");
            if (!raw) throw new Error(`No url in ${d}`);
            url = raw.startsWith("http") ? raw : raw.startsWith("/file=") ? BASE_URL + raw : `${BASE_URL}/file=${raw}`;
            break;
          }
          if (ev.msg === "error" || ev.msg === "unexpected_error") throw new Error(d);
        } catch (e) { if ((e as Error).message.startsWith("No url") || (e as Error).message.startsWith("HF upstream")) throw e; }
      }
      if (url) break;
    }
    await r.cancel().catch(() => {});
    if (!url) throw new Error("No url in stream - HF Space may be sleeping, retry");
    const img = await fetch(url); const bytes = new Uint8Array(await img.arrayBuffer());
    let bin = ""; bytes.forEach((b) => bin += String.fromCharCode(b));
    return new Response(JSON.stringify({ image: `data:image/png;base64,${btoa(bin)}` }), { headers: { ...cors, "Content-Type": "application/json" } });
  } catch (e) { return new Response(JSON.stringify({ error: (e as Error).message }), { status: 500, headers: { ...cors, "Content-Type": "application/json" } }); }
});
