const isLocalPreview =
  typeof window !== "undefined" &&
  ["127.0.0.1", "localhost"].includes(window.location.hostname) &&
  ["4173", "5173"].includes(window.location.port);

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || (isLocalPreview ? "http://127.0.0.1:7979/api" : "/api");

function getAuthHeader() {
  const token = typeof window !== "undefined" ? window.localStorage.getItem("acg-token") : "";
  if (!token) {
    return {};
  }
  return {
    Authorization: `Bearer ${token}`,
  };
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...getAuthHeader(),
      ...(options.headers || {}),
    },
    ...options,
  });

  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  const result = await response.json();
  if (result && typeof result.code !== "undefined" && result.code !== 200) {
    throw new Error(result.msg || "Request failed");
  }
  return result.data;
}

export async function getHomeData() {
  return request("/content/home");
}

export async function getArticles(params = {}) {
  const search = new URLSearchParams();

  if (params.category) {
    search.set("category", params.category);
  }

  if (params.keyword) {
    search.set("keyword", params.keyword);
  }

  if (params.sort) {
    search.set("sort", params.sort);
  }

  if (params.tag) {
    search.set("tag", params.tag);
  }

  const query = search.toString();
  return request(`/content/articles${query ? `?${query}` : ""}`);
}

export async function getArticleBySlug(slug) {
  const safeSlug = encodeURIComponent(slug ?? "");
  return request(`/content/articles/${safeSlug}`);
}

export async function searchArticles(keyword) {
  const query = keyword ? `?keyword=${encodeURIComponent(keyword)}` : "";
  return request(`/content/search${query}`);
}

export async function loginUser(payload) {
  return request("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function getUserProfile() {
  return request("/user/profile");
}

export async function getArticleComments(slug, params = {}) {
  const safeSlug = encodeURIComponent(slug ?? "");
  const search = new URLSearchParams();
  if (params.cursor) {
    search.set("cursor", String(params.cursor));
  }
  if (params.size) {
    search.set("size", String(params.size));
  }
  const query = search.toString();
  return request(`/content/articles/${safeSlug}/comments${query ? `?${query}` : ""}`);
}

export async function createArticleComment(slug, payload) {
  const safeSlug = encodeURIComponent(slug ?? "");
  return request(`/content/articles/${safeSlug}/comments`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function replyArticleComment(commentId, payload) {
  return request(`/content/comments/${encodeURIComponent(commentId)}/reply`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function deleteArticleComment(commentId) {
  return request(`/content/comments/${encodeURIComponent(commentId)}`, {
    method: "DELETE",
  });
}
