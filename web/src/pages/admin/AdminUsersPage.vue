<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import {
  listAdminUsers,
  patchAdminUserRoles,
  patchAdminUserStatus,
  resetAdminUserPassword,
} from "../../api/admin";

const users = ref([]);
const loading = ref(false);
const keyword = ref("");
const status = ref("");

const permissionOpen = ref(false);
const currentUser = ref(null);
const roleDraft = reactive({
  roles: [],
});
const savingRoles = ref(false);

const rolePresets = [
  { key: "ADMIN", label: "管理员", perms: ["dashboard:read", "post:create", "comment:read", "user:read"] },
  { key: "EDITOR", label: "编辑", perms: ["dashboard:read", "post:create"] },
  { key: "REVIEWER", label: "审核员", perms: ["dashboard:read", "comment:read"] },
  { key: "AUTHOR", label: "文章博主", perms: ["dashboard:read", "post:create"] },
];

const roleToPerms = Object.fromEntries(rolePresets.map((r) => [r.key, r.perms]));

const mergedPerms = computed(() => {
  const set = new Set();
  roleDraft.roles.forEach((role) => {
    (roleToPerms[role] || []).forEach((perm) => set.add(perm));
  });
  return Array.from(set);
});

function statusText(v) {
  return v === 1 ? "启用" : "停用";
}

function normalizeItem(item) {
  return {
    id: item.id,
    username: item.username || "",
    nickname: item.nickname || "",
    role: Array.isArray(item.roles) && item.roles.length ? item.roles.join(",") : "AUTHOR",
    roleList: Array.isArray(item.roles) && item.roles.length ? [...item.roles] : ["AUTHOR"],
    status: statusText(item.status),
    rawStatus: item.status,
    lastLogin: item.lastLoginTime || "-",
  };
}

async function fetchUsers() {
  loading.value = true;
  try {
    const data = await listAdminUsers({
      keyword: keyword.value.trim() || undefined,
      status: status.value === "" ? undefined : Number(status.value),
      pageNum: 1,
      pageSize: 20,
    });
    users.value = Array.isArray(data?.items) ? data.items.map(normalizeItem) : [];
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "加载用户失败");
  } finally {
    loading.value = false;
  }
}

async function toggleStatus(item) {
  try {
    await patchAdminUserStatus(item.id, item.rawStatus === 1 ? 0 : 1);
    await fetchUsers();
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "切换状态失败");
  }
}

async function handleResetPassword(item) {
  if (!window.confirm(`确认重置 ${item.nickname} 的密码为 123456 吗？`)) return;
  try {
    await resetAdminUserPassword(item.id);
    window.alert("已重置密码为 123456");
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "重置密码失败");
  }
}

function openPermissionEditor(item) {
  currentUser.value = item;
  roleDraft.roles = [...item.roleList];
  permissionOpen.value = true;
}

function closePermissionEditor() {
  permissionOpen.value = false;
  currentUser.value = null;
  roleDraft.roles = [];
}

function toggleRole(role) {
  if (roleDraft.roles.includes(role)) {
    roleDraft.roles = roleDraft.roles.filter((r) => r !== role);
  } else {
    roleDraft.roles.push(role);
  }
}

async function savePermissions() {
  if (!currentUser.value) return;
  if (!roleDraft.roles.length) {
    window.alert("至少选择一个角色");
    return;
  }
  savingRoles.value = true;
  try {
    await patchAdminUserRoles(currentUser.value.id, roleDraft.roles);
    closePermissionEditor();
    await fetchUsers();
  } catch (error) {
    window.alert(error instanceof Error ? error.message : "保存权限失败");
  } finally {
    savingRoles.value = false;
  }
}

onMounted(fetchUsers);
</script>

<template>
  <section class="admin-grid">
    <article class="admin-card">
      <h3>用户管理</h3>
      <div class="admin-toolbar">
        <input v-model="keyword" type="search" placeholder="搜索用户名 / 昵称" @keyup.enter="fetchUsers" />
        <select v-model="status" @change="fetchUsers">
          <option value="">全部状态</option>
          <option value="1">启用</option>
          <option value="0">停用</option>
        </select>
        <button type="button" @click="fetchUsers">刷新</button>
      </div>
    </article>

    <article class="admin-card">
      <p v-if="loading">加载中...</p>
      <table v-else class="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>角色</th>
            <th>状态</th>
            <th>最后登录</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in users" :key="item.id">
            <td>#{{ item.id }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.nickname }}</td>
            <td>{{ item.role }}</td>
            <td><span class="admin-tag">{{ item.status }}</span></td>
            <td>{{ item.lastLogin }}</td>
            <td class="admin-actions">
              <button type="button" @click="openPermissionEditor(item)">权限管理</button>
              <button type="button" @click="toggleStatus(item)">
                {{ item.rawStatus === 1 ? "停用" : "启用" }}
              </button>
              <button type="button" @click="handleResetPassword(item)">重置密码</button>
            </td>
          </tr>
        </tbody>
      </table>
    </article>

    <div v-if="permissionOpen" class="admin-modal-mask" @click.self="closePermissionEditor">
      <section class="admin-modal">
        <header class="admin-modal__header">
          <h3>权限管理 - {{ currentUser?.nickname }}</h3>
          <button type="button" class="admin-modal__close" @click="closePermissionEditor">关闭</button>
        </header>
        <div class="admin-modal__body">
          <div class="admin-perm-panel">
            <p class="admin-tag-panel__title">角色模板</p>
            <div class="admin-tag-panel__pool">
              <button
                v-for="role in rolePresets"
                :key="role.key"
                type="button"
                class="admin-tag-chip"
                :class="{ 'admin-tag-chip--active': roleDraft.roles.includes(role.key) }"
                @click="toggleRole(role.key)"
              >
                {{ role.label }}
              </button>
            </div>
          </div>
          <div class="admin-perm-list">
            <p class="admin-tag-panel__title">最终权限</p>
            <ul>
              <li v-for="perm in mergedPerms" :key="perm">{{ perm }}</li>
            </ul>
          </div>
        </div>
        <footer class="admin-modal__footer">
          <button type="button" class="admin-modal__cancel" @click="closePermissionEditor">取消</button>
          <button type="button" class="admin-modal__submit" :disabled="savingRoles" @click="savePermissions">
            {{ savingRoles ? "保存中..." : "保存权限" }}
          </button>
        </footer>
      </section>
    </div>
  </section>
</template>
