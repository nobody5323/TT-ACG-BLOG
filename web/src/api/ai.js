const isLocalPreview =
  typeof window !== "undefined" &&
  ["127.0.0.1", "localhost"].includes(window.location.hostname);

const AI_API_BASE_URL =
  import.meta.env.VITE_AI_API_BASE_URL || (isLocalPreview ? "http://127.0.0.1:8000" : "");

const AI_API_KEY = import.meta.env.VITE_AI_API_KEY || "";

function buildAiHeaders(extra = {}) {
  return {
    ...(AI_API_KEY ? { "X-API-Key": AI_API_KEY } : {}),
    ...extra,
  };
}

async function parseAiResponse(response) {
  let data = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }

  if (!response.ok) {
    const message = data?.error?.message || data?.message || `AI request failed: ${response.status}`;
    throw new Error(message);
  }

  return data;
}

export async function sendAiChat(payload) {
  const response = await fetch(`${AI_API_BASE_URL}/chat`, {
    method: "POST",
    headers: buildAiHeaders({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify(payload),
  });
  return parseAiResponse(response);
}

export async function uploadAiFile(file) {
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${AI_API_BASE_URL}/ingest/upload`, {
    method: "POST",
    headers: buildAiHeaders(),
    body: formData,
  });
  return parseAiResponse(response);
}
