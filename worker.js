// Cloudflare Worker entry — serves the static site in docs/ AND proxies the chatbot
// to Groq. Giving the Worker this code is what lets you add the GROQ_API_KEY secret
// (a static-assets-only Worker has no compute, so variables are disabled).
//
// Requests to /api/chat are handled here with the GROQ_API_KEY secret (server-side).
// Everything else is served from the static assets (the docs/ folder via env.ASSETS).

const GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === "/api/chat") {
      if (request.method === "OPTIONS") return new Response(null, { headers: CORS });
      if (request.method !== "POST") {
        return new Response("Method Not Allowed", { status: 405, headers: CORS });
      }
      return handleChat(request, env);
    }

    // Serve the static site (docs/) for all other requests.
    return env.ASSETS.fetch(request);
  },
};

async function handleChat(request, env) {
  if (!env.GROQ_API_KEY) {
    return json({ error: "GROQ_API_KEY is not configured on this Worker." }, 500);
  }

  let payload;
  try {
    payload = await request.json();
  } catch {
    return json({ error: "Invalid JSON body" }, 400);
  }

  const body = {
    model: payload.model || "llama-3.3-70b-versatile",
    messages: payload.messages || [],
    temperature: 0.3,
    max_tokens: 700,
  };

  const groqRes = await fetch(GROQ_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${env.GROQ_API_KEY}`, // secret, stays server-side
    },
    body: JSON.stringify(body),
  });

  const text = await groqRes.text();
  return new Response(text, {
    status: groqRes.status,
    headers: { "Content-Type": "application/json", ...CORS },
  });
}

function json(obj, status) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "Content-Type": "application/json", ...CORS },
  });
}

