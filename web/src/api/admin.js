const isLocalPreview =
  typeof window !== "undefined" &&
  ["127.0.0.1", "localhost"].includes(window.location.hostname) &&
  ["4173", "5173"].includes(window.location.port);

const ADMIN_API_BASE_URL =
  import.meta.env.VITE_ADMIN_API_BASE_URL || (isLocalPreview ? "http://127.0.0.1:7980/api/admin" : "/api/admin");

function getAdminAuthHeader() {
  const token = typeof window !== "undefined" ? window.localStorage.getItem("acg-admin-token") : "";
  if (!token) {
    return {};
  }
  return {
    Authorization: `Bearer ${token}`,
  };
}

async function request(path, options = {}) {
  const response = await fetch(`${ADMIN_API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...getAdminAuthHeader(),
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

async function requestForm(path, formData) {
  const response = await fetch(`${ADMIN_API_BASE_URL}${path}`, {
    method: "POST",
    headers: {
      ...getAdminAuthHeader(),
    },
    body: formData,
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

export async function adminLogin(payload) {
  return request("/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function adminMe() {
  return request("/auth/me");
}

export async function adminPermissions() {
  return request("/auth/permissions");
}

export async function listAdminPosts(params = {}) {
  const search = new URLSearchParams();
  if (params.keyword) search.set("keyword", params.keyword);
  if (typeof params.status !== "undefined" && params.status !== null && params.status !== "") {
    search.set("status", String(params.status));
  }
  if (params.pageNum) search.set("pageNum", String(params.pageNum));
  if (params.pageSize) search.set("pageSize", String(params.pageSize));
  const query = search.toString();
  return request(`/posts${query ? `?${query}` : ""}`);
}

export async function getAdminPost(id) {
  return request(`/posts/${encodeURIComponent(id)}`);
}

export async function createAdminPost(payload) {
  return request("/posts", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateAdminPost(id, payload) {
  return request(`/posts/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function publishAdminPost(id) {
  return request(`/posts/${encodeURIComponent(id)}/publish`, {
    method: "POST",
  });
}

export async function offlineAdminPost(id) {
  return request(`/posts/${encodeURIComponent(id)}/offline`, {
    method: "POST",
  });
}

export async function deleteAdminPost(id) {
  return request(`/posts/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
}

export async function listAdminComments(params = {}) {
  const search = new URLSearchParams();
  if (params.keyword) search.set("keyword", params.keyword);
  if (typeof params.status !== "undefined" && params.status !== null && params.status !== "") {
    search.set("status", String(params.status));
  }
  if (params.pageNum) search.set("pageNum", String(params.pageNum));
  if (params.pageSize) search.set("pageSize", String(params.pageSize));
  const query = search.toString();
  return request(`/comments${query ? `?${query}` : ""}`);
}

export async function getAdminComment(id) {
  return request(`/comments/${encodeURIComponent(id)}`);
}

export async function replyAdminComment(id, payload) {
  return request(`/comments/${encodeURIComponent(id)}/reply`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function patchAdminCommentStatus(id, status) {
  return request(`/comments/${encodeURIComponent(id)}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

export async function deleteAdminComment(id) {
  return request(`/comments/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
}

export async function batchAdminCommentStatus(ids, status) {
  return request("/comments/batch/status", {
    method: "POST",
    body: JSON.stringify({ ids, status }),
  });
}

export async function listAdminUsers(params = {}) {
  const search = new URLSearchParams();
  if (params.keyword) search.set("keyword", params.keyword);
  if (typeof params.status !== "undefined" && params.status !== null && params.status !== "") {
    search.set("status", String(params.status));
  }
  if (params.pageNum) search.set("pageNum", String(params.pageNum));
  if (params.pageSize) search.set("pageSize", String(params.pageSize));
  const query = search.toString();
  return request(`/users${query ? `?${query}` : ""}`);
}

export async function getAdminUser(id) {
  return request(`/users/${encodeURIComponent(id)}`);
}

export async function createAdminUser(payload) {
  return request("/users", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateAdminUser(id, payload) {
  return request(`/users/${encodeURIComponent(id)}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function patchAdminUserRoles(id, roles) {
  return request(`/users/${encodeURIComponent(id)}/roles`, {
    method: "PATCH",
    body: JSON.stringify({ roles }),
  });
}

export async function patchAdminUserStatus(id, status) {
  return request(`/users/${encodeURIComponent(id)}/status`, {
    method: "PATCH",
    body: JSON.stringify({ status }),
  });
}

export async function resetAdminUserPassword(id) {
  return request(`/users/${encodeURIComponent(id)}/reset-password`, {
    method: "POST",
  });
}

export async function getAdminDashboardOverview() {
  return request("/dashboard/overview");
}

export async function getAdminDashboardTimeline() {
  return request("/dashboard/timeline");
}

export async function uploadAdminImage(file) {
  const form = new FormData();
  form.append("file", file);
  return requestForm("/upload/image", form);
}

export async function uploadAdminFiles(files) {
  const form = new FormData();
  files.forEach((file) => form.append("file", file));
  return requestForm("/upload/file", form);
}
