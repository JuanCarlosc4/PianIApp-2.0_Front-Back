/**
 * PianIA - Panel local de anuncios (ADMIN)
 * - Guarda baseUrl y jwt en localStorage
 * - Permite crear (POST), listar (GET paginado) y desactivar (DELETE)
 *
 * Nota: si abres el HTML como file://, el navegador puede bloquear CORS.
 * Recomendado: servirlo con un server local:
 *   - python -m http.server 5173
 * y abrir http://localhost:5173/piania-anuncios/
 */

const LS_BASE_URL = "piania_admin_baseUrl";
const LS_JWT = "piania_admin_jwt";

const $ = (id) => document.getElementById(id);

function setResult(el, data) {
  el.textContent = typeof data === "string" ? data : JSON.stringify(data, null, 2);
}

function getBaseUrl() {
  const raw = $("baseUrl").value.trim();
  return raw.replace(/\/+$/, "");
}

function getJwt() {
  return $("jwt").value.trim();
}

function authHeaders() {
  const jwt = getJwt();
  if (!jwt) return { "Content-Type": "application/json" };
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${jwt}`,
  };
}

function toIsoOrNull(datetimeLocalValue) {
  const v = (datetimeLocalValue || "").trim();
  if (!v) return null;
  // datetime-local viene como "YYYY-MM-DDTHH:mm"
  // backend suele aceptar "YYYY-MM-DDTHH:mm:ss" o "YYYY-MM-DDTHH:mm:ss.SSS"
  // Lo dejamos tal cual, y si quieres segundos podrías añadirlos.
  return v;
}

async function httpJson(url, options = {}) {
  const res = await fetch(url, options);
  const text = await res.text();
  let body = null;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    body = text;
  }
  if (!res.ok) {
    const err = new Error(`HTTP ${res.status} ${res.statusText}`);
    err.status = res.status;
    err.body = body;
    throw err;
  }
  return body;
}

function renderAnnouncementItem(a) {
  const div = document.createElement("div");
  div.className = "item";

  const title = document.createElement("p");
  title.className = "item__title";
  title.textContent = a.title ?? "(sin título)";

  const msg = document.createElement("p");
  msg.className = "item__msg";
  msg.textContent = a.message ?? "";

  const meta = document.createElement("div");
  meta.className = "item__meta";

  const badge = document.createElement("span");
  badge.className = "badge badge--ok";
  badge.textContent = `ID: ${a.id}`;

  const createdAt = document.createElement("span");
  createdAt.textContent = `Creado: ${a.createdAt ?? "-"}`;

  const expiresAt = document.createElement("span");
  expiresAt.textContent = `Caduca: ${a.expiresAt ?? "-"}`;

  meta.appendChild(badge);
  meta.appendChild(createdAt);
  meta.appendChild(expiresAt);

  const actions = document.createElement("div");
  actions.className = "row";

  const btnDeactivate = document.createElement("button");
  btnDeactivate.className = "btn btn--danger";
  btnDeactivate.textContent = "Desactivar";
  btnDeactivate.onclick = async () => {
    if (!confirm(`Desactivar anuncio ${a.id}?`)) return;
    try {
      btnDeactivate.disabled = true;
      await deactivateAnnouncement(a.id);
      await loadAnnouncements();
    } catch (e) {
      alert(`Error desactivando: ${e.message}\n\n${JSON.stringify(e.body ?? {}, null, 2)}`);
    } finally {
      btnDeactivate.disabled = false;
    }
  };

  actions.appendChild(btnDeactivate);

  div.appendChild(title);
  div.appendChild(msg);
  div.appendChild(meta);
  div.appendChild(actions);

  return div;
}

async function createAnnouncement() {
  const baseUrl = getBaseUrl();
  const payload = {
    title: $("title").value.trim(),
    message: $("message").value,
    expiresAt: toIsoOrNull($("expiresAt").value),
  };

  // Si expiresAt es null, lo eliminamos para que no mande null si el backend no lo admite
  if (!payload.expiresAt) delete payload.expiresAt;

  // Backend nuevo (vía API Gateway -> core-service)
  const url = `${baseUrl}/piania/core/announcements`;
  return httpJson(url, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify(payload),
  });
}

async function loadAnnouncements() {
  const baseUrl = getBaseUrl();
  const size = parseInt($("pageSize").value || "50", 10);

  // Backend nuevo (vía API Gateway -> core-service)
  const url = `${baseUrl}/piania/core/announcements?page=0&size=${encodeURIComponent(size)}`;

  const page = await httpJson(url, {
    method: "GET",
    headers: authHeaders(),
  });

  const listEl = $("list");
  listEl.innerHTML = "";

  const items = page?.content ?? [];
  if (!items.length) {
    const empty = document.createElement("p");
    empty.className = "muted";
    empty.textContent = "No hay anuncios activos.";
    listEl.appendChild(empty);
    return page;
  }

  for (const a of items) {
    listEl.appendChild(renderAnnouncementItem(a));
  }
  return page;
}

async function deactivateAnnouncement(id) {
  const baseUrl = getBaseUrl();
  // Backend nuevo (vía API Gateway -> core-service)
  const url = `${baseUrl}/piania/core/announcements/${encodeURIComponent(id)}`;
  return httpJson(url, {
    method: "DELETE",
    headers: authHeaders(),
  });
}

function loadFromLocalStorage() {
  // IMPORTANTE:
  // En tu docker-compose el API Gateway publica "8090:8080"
  // y además tu puerto 8080 local está ocupado por Oracle XDB.
  //
  // El problema que ves (sigue saliendo 8080) normalmente es porque YA tenías
  // guardado en localStorage el valor viejo (8080) y se vuelve a cargar.
  //
  // Para evitarlo, si detectamos 8080, lo migramos automáticamente a 8090.
  const stored = localStorage.getItem(LS_BASE_URL);
  const migrated =
    stored && /^http:\/\/localhost:8080\/?$/i.test(stored.trim()) ? "http://localhost:8090" : stored;

  const baseUrl = migrated || "http://localhost:8090";
  const jwt = localStorage.getItem(LS_JWT) || "";
  $("baseUrl").value = baseUrl;
  $("jwt").value = jwt;

  // Persistimos la migración para que no vuelva a aparecer 8080
  if (baseUrl === "http://localhost:8090" && stored !== "http://localhost:8090") {
    localStorage.setItem(LS_BASE_URL, "http://localhost:8090");
  }
}

function saveToLocalStorage() {
  localStorage.setItem(LS_BASE_URL, getBaseUrl());
  localStorage.setItem(LS_JWT, getJwt());
}

function clearCreateForm() {
  $("title").value = "";
  $("message").value = "";
  $("expiresAt").value = "";
  $("createResult").textContent = "";
}

function init() {
  loadFromLocalStorage();

  $("btnSave").onclick = () => {
    saveToLocalStorage();
    setResult($("listResult"), "Configuración guardada en localStorage.");
  };

  $("btnLoad").onclick = async () => {
    try {
      setResult($("listResult"), "Cargando...");
      const page = await loadAnnouncements();
      setResult($("listResult"), page);
    } catch (e) {
      setResult($("listResult"), `Error: ${e.message}\n\n${JSON.stringify(e.body ?? {}, null, 2)}`);
    }
  };

  $("btnRefresh").onclick = $("btnLoad").onclick;

  $("btnCreate").onclick = async () => {
    try {
      setResult($("createResult"), "Creando...");
      const created = await createAnnouncement();
      setResult($("createResult"), created);
      await loadAnnouncements();
    } catch (e) {
      setResult($("createResult"), `Error: ${e.message}\n\n${JSON.stringify(e.body ?? {}, null, 2)}`);
    }
  };

  $("btnClear").onclick = clearCreateForm;
}

init();
