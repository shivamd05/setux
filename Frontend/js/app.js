/**
 * SetuX Gateway Dashboard - Main Application Script
 * 
 * 100% Credential-Driven Role-Based Access Control (RBAC):
 * - User authenticates with Spring Boot /api/auth/login.
 * - Spring Boot issues JWT with verified database role:
 *     1. PLATFORM_ADMIN: Multi-Tenant Organizations & Admin Provisioning (/api/platform/organizations/**)
 *     2. TENANT_ADMIN:   Company Settings, Route Policies, Versions & Audits (/api/tenant/**)
 *     3. GATEWAY_SERVICE: Smart Gateway In-Memory Routing Cache (/api/gateway/routes)
 * - UI adaptively unlocks the verified views based on returned JWT claims.
 */

// =============================================================================
// 1. Application State
// =============================================================================
let authenticatedRole = null; // null = GUEST
let currentUser = null;
let isBackendLive = false;

let organizations = [];
let currentRoutes = [];
let auditRecords = [];
let currentTenantOrg = null;

// =============================================================================
// 2. Application Entry Point
// =============================================================================
document.addEventListener('DOMContentLoaded', async () => {
    console.log("[SetuX] Initializing Credential-Driven Multi-Tenant Console...");

    updateGreetingAndTime();
    setInterval(updateGreetingAndTime, 60000);

    setupViewNavigation();
    setupTableSearchAndFilter();
    setupModals();
    setupForms();
    setupRefreshButton();

    // Check existing session
    const token = window.ApiClient.getToken();
    const storedUser = window.ApiClient.getUser();
    const validRoles = ['PLATFORM_ADMIN', 'TENANT_ADMIN', 'GATEWAY_SERVICE'];

    if (token && storedUser && storedUser.role && validRoles.includes(storedUser.role)) {
        unlockRoleFromCredentials(storedUser.role, storedUser);
    } else {
        window.ApiClient.clearSession();
        lockToGuestMode();
    }

    // Ping services
    await checkAllServicesHealth();
    await loadLiveRoleData();
});

// =============================================================================
// 3. Credential-Driven RBAC Engine
// =============================================================================
function unlockRoleFromCredentials(role, user) {
    authenticatedRole = role;
    currentUser = user;

    // UI Elements
    const sidebarRoleBadge = document.getElementById('active-role-badge');
    const sidebarRoleDesc = document.getElementById('active-role-desc');
    const verifiedRolePill = document.getElementById('verified-role-pill');
    const verifiedRoleText = document.getElementById('verified-role-text');
    const sidebarAvatar = document.getElementById('sidebar-avatar');
    const sidebarUser = document.getElementById('sidebar-username');
    const sidebarRoleLabel = document.getElementById('sidebar-role-label');
    const authBtnLabel = document.getElementById('auth-btn-label');
    const guestCard = document.getElementById('guest-access-card');
    const heroRoleBadge = document.getElementById('hero-role-badge');
    const heroRoleDesc = document.getElementById('hero-role-desc');

    // Role-specific menus
    const platformNavs = document.querySelectorAll('.role-platform');
    const tenantNavs = document.querySelectorAll('.role-tenant');
    const gatewayNavs = document.querySelectorAll('.role-gateway');

    // Hide all role menus first
    platformNavs.forEach(el => el.classList.add('hidden'));
    tenantNavs.forEach(el => el.classList.add('hidden'));
    gatewayNavs.forEach(el => el.classList.add('hidden'));

    // Hide unauthenticated callout
    if (guestCard) guestCard.classList.add('hidden');

    // Topbar & sidebar user details
    if (authBtnLabel) authBtnLabel.textContent = 'Sign Out';
    if (sidebarUser) sidebarUser.textContent = user.username;
    if (sidebarRoleLabel) sidebarRoleLabel.textContent = role;

    if (role === 'PLATFORM_ADMIN') {
        // ---------------------------------------------------------------------
        // ROLE 1: PLATFORM_ADMIN (Platform Owner)
        // ---------------------------------------------------------------------
        platformNavs.forEach(el => el.classList.remove('hidden'));

        if (sidebarRoleBadge) {
            sidebarRoleBadge.className = 'role-badge purple';
            sidebarRoleBadge.textContent = '👑 PLATFORM_ADMIN';
        }
        if (sidebarRoleDesc) sidebarRoleDesc.textContent = 'Platform Owner • Multi-Tenant Control';

        if (verifiedRolePill) {
            verifiedRolePill.className = 'verified-role-chip purple';
            verifiedRoleText.textContent = `👑 ${user.username} (PLATFORM_ADMIN)`;
        }

        if (sidebarAvatar) sidebarAvatar.textContent = 'PO';
        if (heroRoleBadge) {
            heroRoleBadge.textContent = '👑 PLATFORM OWNER ACCESS ACTIVE';
            heroRoleBadge.style.color = 'var(--purple-accent)';
        }
        if (heroRoleDesc) {
            heroRoleDesc.innerHTML = `Welcome back, <strong>${escapeHtml(user.username)}</strong>! You have access to client company onboarding and tenant admin provisioning.`;
        }

        switchView('organizations');
    } 
    else if (role === 'TENANT_ADMIN') {
        // ---------------------------------------------------------------------
        // ROLE 2: TENANT_ADMIN (Company Service User)
        // ---------------------------------------------------------------------
        tenantNavs.forEach(el => el.classList.remove('hidden'));

        const orgLabel = user.organizationId ? `Org #${user.organizationId}` : 'Tenant User';

        if (sidebarRoleBadge) {
            sidebarRoleBadge.className = 'role-badge cyan';
            sidebarRoleBadge.textContent = '🏢 TENANT_ADMIN';
        }
        if (sidebarRoleDesc) sidebarRoleDesc.textContent = `${orgLabel} • Routes & Policies`;

        if (verifiedRolePill) {
            verifiedRolePill.className = 'verified-role-chip cyan';
            verifiedRoleText.textContent = `🏢 ${user.username} (${orgLabel})`;
        }

        if (sidebarAvatar) sidebarAvatar.textContent = 'TA';
        if (heroRoleBadge) {
            heroRoleBadge.textContent = '🏢 TENANT_ADMIN ACCESS ACTIVE';
            heroRoleBadge.style.color = 'var(--cyan-primary)';
        }
        if (heroRoleDesc) {
            heroRoleDesc.innerHTML = `Signed in as <strong>${escapeHtml(user.username)}</strong> (${orgLabel}). Configure your organization's routing policies and review audit logs.`;
        }

        switchView('routes');
    } 
    else if (role === 'GATEWAY_SERVICE') {
        // ---------------------------------------------------------------------
        // ROLE 3: GATEWAY_SERVICE (Smart Gateway Machine Identity)
        // ---------------------------------------------------------------------
        gatewayNavs.forEach(el => el.classList.remove('hidden'));

        if (sidebarRoleBadge) {
            sidebarRoleBadge.className = 'role-badge emerald';
            sidebarRoleBadge.textContent = '🤖 GATEWAY_SERVICE';
        }
        if (sidebarRoleDesc) sidebarRoleDesc.textContent = 'Smart Gateway Machine Node (Port 8080)';

        if (verifiedRolePill) {
            verifiedRolePill.className = 'verified-role-chip emerald';
            verifiedRoleText.textContent = `🤖 ${user.username} (Gateway Machine)`;
        }

        if (sidebarAvatar) sidebarAvatar.textContent = 'GW';
        if (heroRoleBadge) {
            heroRoleBadge.textContent = '🤖 GATEWAY_SERVICE ACTIVE';
            heroRoleBadge.style.color = 'var(--emerald-success)';
        }
        if (heroRoleDesc) {
            heroRoleDesc.innerHTML = `Authenticated as <strong>Smart Gateway Microservice</strong>. Real-time active routes are synced from <code>/api/gateway/routes</code>.`;
        }

        switchView('gateway-cache');
    }

    console.log(`[SetuX RBAC] Console successfully unlocked for role: ${role}`);
}

function lockToGuestMode() {
    authenticatedRole = null;
    currentUser = null;

    // Hide role sections
    document.querySelectorAll('.role-platform').forEach(el => el.classList.add('hidden'));
    document.querySelectorAll('.role-tenant').forEach(el => el.classList.add('hidden'));
    document.querySelectorAll('.role-gateway').forEach(el => el.classList.add('hidden'));

    // Show unauthenticated callout
    const guestCard = document.getElementById('guest-access-card');
    if (guestCard) guestCard.classList.remove('hidden');

    const sidebarRoleBadge = document.getElementById('active-role-badge');
    const sidebarRoleDesc = document.getElementById('active-role-desc');
    const verifiedRolePill = document.getElementById('verified-role-pill');
    const verifiedRoleText = document.getElementById('verified-role-text');
    const sidebarAvatar = document.getElementById('sidebar-avatar');
    const sidebarUser = document.getElementById('sidebar-username');
    const sidebarRoleLabel = document.getElementById('sidebar-role-label');
    const authBtnLabel = document.getElementById('auth-btn-label');
    const heroRoleBadge = document.getElementById('hero-role-badge');
    const heroRoleDesc = document.getElementById('hero-role-desc');

    if (sidebarRoleBadge) {
        sidebarRoleBadge.className = 'role-badge guest';
        sidebarRoleBadge.textContent = '🔒 GUEST';
    }
    if (sidebarRoleDesc) sidebarRoleDesc.textContent = 'Not authenticated • Sign in to access';

    if (verifiedRolePill) {
        verifiedRolePill.className = 'verified-role-chip guest';
        verifiedRoleText.textContent = 'Not Authenticated';
    }

    if (sidebarAvatar) sidebarAvatar.textContent = 'G';
    if (sidebarUser) sidebarUser.textContent = 'Guest User';
    if (sidebarRoleLabel) sidebarRoleLabel.textContent = 'UNAUTHENTICATED';
    if (authBtnLabel) authBtnLabel.textContent = 'Sign In';

    if (heroRoleBadge) {
        heroRoleBadge.textContent = '🔒 ROLE-BASED ACCESS CONTROL';
        heroRoleBadge.style.color = 'var(--text-muted)';
    }
    if (heroRoleDesc) {
        heroRoleDesc.textContent = 'Welcome to SetuX. Sign in with your database credentials to unlock your authorized control plane.';
    }

    switchView('dashboard');
}

// =============================================================================
// 4. View Switching & Navigation
// =============================================================================
function setupViewNavigation() {
    const navItems = document.querySelectorAll('.nav-menu .nav-item');

    navItems.forEach(item => {
        item.addEventListener('click', (event) => {
            event.preventDefault();
            const targetView = item.getAttribute('data-view');
            if (targetView) switchView(targetView);
        });
    });
}

function switchView(viewName) {
    const navItems = document.querySelectorAll('.nav-menu .nav-item');
    const viewSections = document.querySelectorAll('.view-section');
    const breadcrumbCurrent = document.getElementById('current-view-title');

    navItems.forEach(nav => {
        if (nav.getAttribute('data-view') === viewName) {
            nav.classList.add('active');
            if (breadcrumbCurrent) {
                breadcrumbCurrent.textContent = nav.querySelector('span:nth-child(2)')?.textContent || viewName;
            }
        } else {
            nav.classList.remove('active');
        }
    });

    viewSections.forEach(section => {
        section.classList.remove('active');
    });

    const activeSection = document.getElementById(`view-${viewName}`);
    if (activeSection) {
        activeSection.classList.add('active');
    }
}
window.switchView = switchView;

// =============================================================================
// 5. Backend Health Polling & Live Data Loading
// =============================================================================
async function checkAllServicesHealth() {
    const statusPill = document.getElementById('backend-status-pill');
    const analyticsServiceStatus8082 = document.getElementById('service-status-8082');
    const sidebarMgmtStatus = document.getElementById('sidebar-mgmt-status');
    const serviceStatus8080 = document.getElementById('service-status-8080');
    const sidebarGatewayStatus = document.getElementById('sidebar-gateway-status');
    const serviceStatus8081 = document.getElementById('service-status-8081');
    const sidebarAnalyticsStatus = document.getElementById('sidebar-analytics-status');

    // 1. API Management Service (8081)
    const mgmt = await window.ApiClient.checkManagementHealth();
    if (mgmt.online) {
        isBackendLive = true;
        if (statusPill) {
            statusPill.className = 'backend-pill online';
            statusPill.querySelector('.pill-text').textContent = 'Spring Boot: Online (8081)';
        }
        if (serviceStatus8081) {
            serviceStatus8081.className = 'status-badge online';
            serviceStatus8081.textContent = 'Online (UP)';
        }
        if (sidebarMgmtStatus) {
            sidebarMgmtStatus.className = 'status-chip ready';
            sidebarMgmtStatus.textContent = '8081 • Online';
        }
    } else {
        isBackendLive = false;
        if (statusPill) {
            statusPill.className = 'backend-pill offline';
            statusPill.querySelector('.pill-text').textContent = 'Spring Boot: Offline';
        }
        if (serviceStatus8081) {
            serviceStatus8081.className = 'status-badge offline';
            serviceStatus8081.textContent = 'Offline';
        }
        if (sidebarMgmtStatus) {
            sidebarMgmtStatus.className = 'status-chip offline';
            sidebarMgmtStatus.textContent = '8081 • Offline';
        }
    }

    // 2. Smart Gateway (8080)
    const gw = await window.ApiClient.checkGatewayHealth();
    if (gw.online) {
        if (serviceStatus8080) {
            serviceStatus8080.className = 'status-badge online';
            serviceStatus8080.textContent = 'Online (UP)';
        }
        if (sidebarGatewayStatus) {
            sidebarGatewayStatus.className = 'status-chip ready';
            sidebarGatewayStatus.textContent = '8080 • Online';
        }
    } else {
        if (serviceStatus8080) {
            serviceStatus8080.className = 'status-badge ready';
            serviceStatus8080.textContent = 'Standby';
        }
        if (sidebarGatewayStatus) {
            sidebarGatewayStatus.className = 'status-chip ready';
            sidebarGatewayStatus.textContent = '8080 • Standby';
        }
    }

    // 3. Analytics Service (8082)
    const analytics = await window.ApiClient.checkAnalyticsHealth();
    if (analytics.online) {
        if (analyticsServiceStatus8082) {
            analyticsServiceStatus8082.className = 'status-badge online';
            analyticsServiceStatus8082.textContent = 'Online (UP)';
        }
        if (sidebarAnalyticsStatus) {
            sidebarAnalyticsStatus.className = 'status-chip ready';
            sidebarAnalyticsStatus.textContent = '8082 • Online';
        }
    } else {
        if (analyticsServiceStatus8082) {
            analyticsServiceStatus8082.className = 'status-badge ready';
            analyticsServiceStatus8082.textContent = 'Standby';
        }
        if (sidebarAnalyticsStatus) {
            sidebarAnalyticsStatus.className = 'status-chip ready';
            sidebarAnalyticsStatus.textContent = '8082 • Standby';
        }
    }
}

async function loadLiveRoleData() {
    if (!authenticatedRole) return;

    if (authenticatedRole === 'PLATFORM_ADMIN') {
        try {
            const orgs = await window.ApiClient.getOrganizations();
            organizations = orgs;
            renderOrganizationsTable();
        } catch (err) {
            console.warn("[SetuX] Platform orgs load:", err.message);
            renderOrganizationsTable();
        }
    } else if (authenticatedRole === 'TENANT_ADMIN') {
        try {
            // 1. Profile
            const profile = await window.ApiClient.getOwnOrganization();
            currentTenantOrg = profile;
            populateTenantProfile(profile);

            // 2. Routes
            const routes = await window.ApiClient.getRoutes();
            currentRoutes = routes;
            renderRoutesTable();

            // 3. Audits
            const audits = await window.ApiClient.getAuditRecords();
            auditRecords = audits;
            renderAuditsTable();
        } catch (err) {
            console.warn("[SetuX] Tenant data load:", err.message);
            renderRoutesTable();
        }
    } else if (authenticatedRole === 'GATEWAY_SERVICE') {
        try {
            const gatewayRoutes = await window.ApiClient.getGatewayRoutes();
            currentRoutes = gatewayRoutes;
            renderGatewayCacheTable();
        } catch (err) {
            console.warn("[SetuX] Gateway cache load:", err.message);
            renderGatewayCacheTable();
        }
    }
}

// =============================================================================
// 6. PLATFORM_ADMIN Table & Actions (Organizations)
// =============================================================================
function renderOrganizationsTable() {
    const tbody = document.getElementById('orgs-table-body');
    const orgsCountStat = document.getElementById('stat-orgs-count');
    const navOrgsCount = document.getElementById('nav-orgs-count');

    if (!tbody) return;

    if (orgsCountStat) orgsCountStat.textContent = organizations.length.toString();
    if (navOrgsCount) navOrgsCount.textContent = organizations.length.toString();

    tbody.innerHTML = '';

    if (organizations.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 32px; color: var(--text-muted);">
                    No tenant organizations registered yet. Click <strong>"➕ Register Organization"</strong> to onboard a client.
                </td>
            </tr>
        `;
        return;
    }

    organizations.forEach(org => {
        const tr = document.createElement('tr');
        const formattedDate = new Date(org.createdAt || Date.now()).toLocaleDateString('en-US', {
            year: 'numeric', month: 'short', day: 'numeric'
        });

        const statusClass = org.status || 'PENDING';

        tr.innerHTML = `
            <td>
                <div class="route-name-block">
                    <strong>${escapeHtml(org.name)}</strong>
                    <span class="route-slug-code">ID: ${org.id}</span>
                </div>
            </td>
            <td>
                <span class="uri-badge">${escapeHtml(org.slug)}</span>
            </td>
            <td>
                <button class="status-indicator-pill ${statusClass}" title="Click to change status" onclick="window.openOrgStatusModal(${org.id}, '${escapeHtml(org.name)}', '${statusClass}')" style="cursor: pointer; border: none;">
                    ${statusClass} ⚙️
                </button>
            </td>
            <td>
                <button class="action-icon-btn" onclick="window.openTenantAdminModal(${org.id}, '${escapeHtml(org.name)}')" title="Provision or inspect tenant admin">
                    👤 Manage Admin
                </button>
            </td>
            <td>
                <span class="target-url-text">${formattedDate}</span>
            </td>
            <td class="text-right">
                <div class="table-action-btns">
                    <button class="action-icon-btn" onclick="window.openOrgModal(${JSON.stringify(org).replace(/"/g, '&quot;')})" title="Edit Organization">
                        ✏️ Edit
                    </button>
                    <button class="action-icon-btn danger" onclick="handleDeleteOrganization(${org.id}, '${escapeHtml(org.name)}')" title="Delete Organization">
                        🗑️
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function handleDeleteOrganization(id, name) {
    if (!confirm(`Are you sure you want to soft-delete organization "${name}" (ID: ${id})?`)) return;

    try {
        await window.ApiClient.deleteOrganization(id);
        showToast(`Organization "${name}" soft-deleted.`, "success");
        await loadLiveRoleData();
    } catch (err) {
        showToast(err.message, "error");
    }
}
window.handleDeleteOrganization = handleDeleteOrganization;

// =============================================================================
// 7. TENANT_ADMIN Tables & Actions (Routes, Audits, Profile)
// =============================================================================
function populateTenantProfile(profile) {
    if (!profile) return;
    const nameInput = document.getElementById('profile-org-name');
    const slugInput = document.getElementById('profile-org-slug');
    const statusInput = document.getElementById('profile-org-status');

    if (nameInput) nameInput.value = profile.name || '';
    if (slugInput) slugInput.value = profile.slug || '';
    if (statusInput) statusInput.value = profile.status || 'ACTIVE';
}

function renderRoutesTable() {
    const tbody = document.getElementById('routes-table-body');
    const routeCountStat = document.getElementById('stat-routes-count');
    const navRoutesCount = document.getElementById('nav-routes-count');
    const searchInput = document.getElementById('route-search-input');
    const methodFilter = document.getElementById('method-filter-select');

    if (!tbody) return;

    if (routeCountStat) routeCountStat.textContent = currentRoutes.length.toString();
    if (navRoutesCount) navRoutesCount.textContent = currentRoutes.length.toString();

    const query = (searchInput?.value || '').toLowerCase().trim();
    const selectedMethod = methodFilter?.value || 'ALL';

    const filtered = currentRoutes.filter(r => {
        const matchesQuery = (r.name || '').toLowerCase().includes(query) ||
                             (r.pathPattern || '').toLowerCase().includes(query) ||
                             (r.routeId || '').toLowerCase().includes(query);
        const matchesMethod = selectedMethod === 'ALL' || (r.allowedMethods && r.allowedMethods.includes(selectedMethod));
        return matchesQuery && matchesMethod;
    });

    tbody.innerHTML = '';

    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 32px; color: var(--text-muted);">
                    No API routes configured. Click <strong>"➕ Create Route"</strong> to deploy a new routing policy.
                </td>
            </tr>
        `;
        return;
    }

    filtered.forEach(route => {
        const tr = document.createElement('tr');
        const methodChips = (route.allowedMethods || []).map(m => `<span class="method-chip ${m}">${m}</span>`).join(' ');
        const sampleMethod = (route.allowedMethods && route.allowedMethods[0]) || 'GET';
        const cleanPath = (route.pathPattern || '').replace('/**', '');
        const sampleCurl = `curl -X ${sampleMethod} http://localhost:8080${cleanPath}`;

        const rateLimitLabel = route.requestsPerMinute ? `${route.requestsPerMinute} rpm` : 'Unlimited';
        const timeoutLabel = `${route.responseTimeoutMs || 5000} ms`;

        tr.innerHTML = `
            <td>
                <div class="route-name-block">
                    <strong>${escapeHtml(route.name)}</strong>
                    <span class="route-slug-code">${escapeHtml(route.routeId)}</span>
                </div>
            </td>
            <td>
                <span class="uri-badge">${escapeHtml(route.pathPattern)}</span>
            </td>
            <td>
                <span class="target-url-text">${escapeHtml(route.targetBaseUrl)}</span>
                <span style="display:block; font-size: 0.72rem; color: var(--text-muted);">Prefix: ${escapeHtml(route.targetPathPrefix || '/')}</span>
            </td>
            <td>
                <div class="methods-list">${methodChips}</div>
            </td>
            <td>
                <span style="font-size: 0.8rem; font-weight: 600;">${rateLimitLabel}</span>
                <span style="display:block; font-size: 0.72rem; color: var(--text-muted);">⏱️ ${timeoutLabel}</span>
            </td>
            <td>
                <button class="status-indicator-pill ${route.active ? 'active' : 'inactive'}" onclick="handleToggleRouteStatus(${route.id}, ${!route.active})" title="Click to toggle status" style="cursor: pointer; border: none;">
                    ${route.active ? 'Active' : 'Inactive'}
                </button>
            </td>
            <td class="text-right">
                <div class="table-action-btns">
                    <button class="action-icon-btn" onclick="copyCurlToClipboard('${escapeHtml(sampleCurl)}')" title="Copy test cURL command">
                        📋 cURL
                    </button>
                    <button class="action-icon-btn" onclick="window.openVersionModal('${escapeHtml(route.routeId)}')" title="View Version Snapshots">
                        📜 Versions
                    </button>
                    <button class="action-icon-btn" onclick="window.openRouteModal(${JSON.stringify(route).replace(/"/g, '&quot;')})" title="Edit Route">
                        ✏️
                    </button>
                    <button class="action-icon-btn danger" onclick="handleDeleteRoute(${route.id}, '${escapeHtml(route.name)}')" title="Delete Route">
                        🗑️
                    </button>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function handleToggleRouteStatus(id, newStatus) {
    try {
        await window.ApiClient.changeRouteStatus(id, newStatus);
        showToast(`Route status updated to ${newStatus ? 'Active' : 'Inactive'}`, "success");
        await loadLiveRoleData();
    } catch (err) {
        showToast(err.message, "error");
    }
}
window.handleToggleRouteStatus = handleToggleRouteStatus;

async function handleDeleteRoute(id, name) {
    if (!confirm(`Are you sure you want to delete route "${name}"?`)) return;

    try {
        await window.ApiClient.deleteRoute(id);
        showToast("Route deleted from Spring Boot.", "success");
        await loadLiveRoleData();
    } catch (err) {
        showToast(err.message, "error");
    }
}
window.handleDeleteRoute = handleDeleteRoute;

function copyCurlToClipboard(cmd) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(cmd);
        showToast("cURL command copied to clipboard! 📋", "success");
    } else {
        showToast(cmd, "info");
    }
}
window.copyCurlToClipboard = copyCurlToClipboard;

function renderAuditsTable() {
    const tbody = document.getElementById('audits-table-body');
    if (!tbody) return;

    tbody.innerHTML = '';

    if (auditRecords.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; padding: 30px; color: var(--text-muted);">
                    No audit records logged yet.
                </td>
            </tr>
        `;
        return;
    }

    auditRecords.forEach(audit => {
        const tr = document.createElement('tr');
        const formattedDate = new Date(audit.createdAt || Date.now()).toLocaleString();

        tr.innerHTML = `
            <td><code>${escapeHtml(audit.action)}</code></td>
            <td><span class="uri-badge">${escapeHtml(audit.resourceType)}</span></td>
            <td><code>${escapeHtml(audit.resourceId)}</code></td>
            <td><strong>${escapeHtml(audit.actor)}</strong></td>
            <td>${audit.durationMs || 0} ms</td>
            <td><span class="status-indicator-pill ${audit.result === 'SUCCESS' ? 'active' : 'inactive'}">${escapeHtml(audit.result)}</span></td>
            <td><span class="target-url-text">${formattedDate}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

// =============================================================================
// 8. GATEWAY_SERVICE Table (Proxy Cache)
// =============================================================================
function renderGatewayCacheTable() {
    const tbody = document.getElementById('gateway-cache-table-body');
    const syncStat = document.getElementById('stat-gateway-sync');
    if (!tbody) return;

    if (syncStat) syncStat.textContent = currentRoutes.length.toString();

    tbody.innerHTML = '';

    if (currentRoutes.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 30px; color: var(--text-muted);">
                    No active routes cached in memory.
                </td>
            </tr>
        `;
        return;
    }

    currentRoutes.forEach(route => {
        const tr = document.createElement('tr');
        const methodChips = (route.allowedMethods || []).map(m => `<span class="method-chip ${m}">${m}</span>`).join(' ');

        tr.innerHTML = `
            <td><code>${escapeHtml(route.routeId)}</code></td>
            <td><span class="uri-badge">${escapeHtml(route.pathPattern)}</span></td>
            <td><span class="target-url-text">${escapeHtml(route.targetBaseUrl)}</span></td>
            <td><div class="methods-list">${methodChips}</div></td>
            <td>${route.responseTimeoutMs || 5000} ms</td>
            <td><span class="status-indicator-pill active">CACHED IN MEMORY</span></td>
        `;
        tbody.appendChild(tr);
    });
}

// =============================================================================
// 9. Modals (Global Controls & Setup)
// =============================================================================
function openModalById(id) {
    const m = document.getElementById(id);
    if (m) {
        m.classList.add('open');
        m.style.setProperty('display', 'flex', 'important');
    }
}

function closeModalById(id) {
    const m = document.getElementById(id);
    if (m) {
        m.classList.remove('open');
        m.style.setProperty('display', 'none', 'important');
    }
}

// Modal 1: Sign In
window.openLoginModal = function() {
    const errorAlert = document.getElementById('login-error-alert');
    if (errorAlert) errorAlert.style.display = 'none';
    openModalById('login-modal');
    setTimeout(() => document.getElementById('login-username')?.focus(), 50);
};
window.closeLoginModal = function() { closeModalById('login-modal'); };

// Modal 2: Organization Form
window.openOrgModal = function(editOrg = null) {
    const form = document.getElementById('org-form');
    if (form) form.reset();
    const editIdInput = document.getElementById('org-edit-id');
    const submitBtn = document.getElementById('submit-org-btn');

    if (editOrg && editOrg.id) {
        if (editIdInput) editIdInput.value = editOrg.id;
        document.getElementById('org-name').value = editOrg.name || '';
        document.getElementById('org-slug').value = editOrg.slug || '';
        if (submitBtn) submitBtn.textContent = 'Save Changes';
    } else {
        if (editIdInput) editIdInput.value = '';
        if (submitBtn) submitBtn.textContent = 'Create Organization';
    }

    openModalById('org-modal');
    setTimeout(() => document.getElementById('org-name')?.focus(), 50);
};
window.closeOrgModal = function() { closeModalById('org-modal'); };

// Modal 3: Provision Tenant Admin
window.openTenantAdminModal = async function(orgId, orgName) {
    const form = document.getElementById('tenant-admin-form');
    if (form) form.reset();

    document.getElementById('admin-org-id').value = orgId;
    document.getElementById('admin-org-display').value = `${orgName} (ID: ${orgId})`;

    // Check if an admin already exists for this org
    try {
        const existingAdmin = await window.ApiClient.getTenantAdmin(orgId);
        if (existingAdmin && existingAdmin.username) {
            document.getElementById('admin-username').value = existingAdmin.username;
            document.getElementById('admin-enabled').checked = existingAdmin.enabled;
            document.getElementById('submit-admin-btn').textContent = 'Update Administrator';
        } else {
            document.getElementById('submit-admin-btn').textContent = 'Provision Administrator';
        }
    } catch (e) {
        document.getElementById('submit-admin-btn').textContent = 'Provision Administrator';
    }

    openModalById('tenant-admin-modal');
    setTimeout(() => document.getElementById('admin-username')?.focus(), 50);
};
window.closeTenantAdminModal = function() { closeModalById('tenant-admin-modal'); };

// Modal 4: Organization Status
window.openOrgStatusModal = function(orgId, orgName, currentStatus) {
    document.getElementById('status-org-id').value = orgId;
    document.getElementById('status-org-display').value = `${orgName} (ID: ${orgId})`;
    document.getElementById('status-select').value = currentStatus || 'PENDING';
    openModalById('org-status-modal');
};
window.closeOrgStatusModal = function() { closeModalById('org-status-modal'); };

// Modal 5: Route Configuration
window.openRouteModal = function(editRoute = null) {
    const form = document.getElementById('route-form');
    if (form) form.reset();

    const titleEl = document.getElementById('route-modal-title');
    const editIdInput = document.getElementById('route-edit-id');
    const submitBtn = document.getElementById('submit-route-btn');

    if (editRoute && editRoute.id) {
        if (titleEl) titleEl.textContent = 'Edit API Route';
        if (editIdInput) editIdInput.value = editRoute.id;
        document.getElementById('route-name').value = editRoute.name || '';
        document.getElementById('route-id').value = editRoute.routeId || '';
        document.getElementById('path-pattern').value = editRoute.pathPattern || '';
        document.getElementById('target-url').value = editRoute.targetBaseUrl || '';
        document.getElementById('target-prefix').value = editRoute.targetPathPrefix || '/';
        document.getElementById('route-rate-limit').value = editRoute.requestsPerMinute || '';
        document.getElementById('route-timeout').value = editRoute.responseTimeoutMs || 5000;
        document.getElementById('route-active').checked = Boolean(editRoute.active);
        document.getElementById('route-idempotency').checked = Boolean(editRoute.idempotencyEnabled);

        // Checkboxes
        const methods = editRoute.allowedMethods || ['GET'];
        form.querySelectorAll('input[name="methods"]').forEach(cb => {
            cb.checked = methods.includes(cb.value);
        });

        if (submitBtn) submitBtn.textContent = 'Save Changes';
    } else {
        if (titleEl) titleEl.textContent = 'Configure Tenant API Route';
        if (editIdInput) editIdInput.value = '';
        document.getElementById('target-prefix').value = '/';
        document.getElementById('route-timeout').value = '5000';
        document.getElementById('route-active').checked = true;
        if (submitBtn) submitBtn.textContent = 'Save Route';
    }

    openModalById('route-modal');
    setTimeout(() => document.getElementById('route-name')?.focus(), 50);
};
window.closeRouteModal = function() { closeModalById('route-modal'); };

// Modal 6: Route Versions
window.openVersionModal = async function(routeId) {
    openModalById('version-history-modal');
    const container = document.getElementById('version-history-content');
    if (!container) return;

    container.innerHTML = `<div style="text-align: center; padding: 20px; color: var(--text-muted);">Fetching version snapshots from Spring Boot...</div>`;

    try {
        const versions = await window.ApiClient.getRouteVersions(routeId);
        if (!versions || versions.length === 0) {
            container.innerHTML = `<div style="text-align: center; padding: 20px; color: var(--text-muted);">No version history recorded for route <code>${escapeHtml(routeId)}</code>.</div>`;
            return;
        }

        container.innerHTML = versions.map(v => {
            let formattedSnapshot = v.configurationSnapshot;
            try {
                formattedSnapshot = JSON.stringify(JSON.parse(v.configurationSnapshot), null, 2);
            } catch (e) {}

            return `
                <div style="border: 1px solid var(--border-subtle); border-radius: var(--radius-sm); padding: 14px; margin-bottom: 12px; background: rgba(255,255,255,0.015);">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                        <div>
                            <span class="role-badge purple">Version #${v.versionNumber}</span>
                            <span class="uri-badge" style="margin-left: 8px;">${escapeHtml(v.changeType || 'UPDATE')}</span>
                        </div>
                        <span style="font-size: 0.78rem; color: var(--text-muted);">${new Date(v.createdAt).toLocaleString()}</span>
                    </div>
                    <div class="snapshot-code-block">${escapeHtml(formattedSnapshot)}</div>
                </div>
            `;
        }).join('');
    } catch (err) {
        container.innerHTML = `<div style="color: var(--rose-danger); padding: 14px;">Failed to load versions: ${escapeHtml(err.message)}</div>`;
    }
};
window.closeVersionModal = function() { closeModalById('version-history-modal'); };

// Global Topbar Action
window.handleAuthAction = function() {
    const token = window.ApiClient?.getToken();
    if (token && authenticatedRole) {
        window.ApiClient.clearSession();
        showToast("Signed out successfully. Session ended.", "info");
        lockToGuestMode();
    } else {
        window.openLoginModal();
    }
};

// 1-Click Quick Login
window.quickLogin = async function(username, password) {
    document.getElementById('login-username').value = username;
    document.getElementById('login-password').value = password;
    window.openLoginModal();

    const submitBtn = document.getElementById('submit-login-btn');
    submitBtn.textContent = 'Authenticating...';
    submitBtn.disabled = true;

    const result = await window.ApiClient.login(username, password);
    submitBtn.textContent = 'Authenticate';
    submitBtn.disabled = false;

    if (result.success) {
        showToast(`Signed in as ${username} (${result.data.role})`, "success");
        window.closeLoginModal();
        unlockRoleFromCredentials(result.data.role, result.data);
        await loadLiveRoleData();
    } else {
        const errorAlert = document.getElementById('login-error-alert');
        if (errorAlert) {
            errorAlert.textContent = result.error;
            errorAlert.style.display = 'block';
        }
        showToast(result.error, "error");
    }
};

function setupModals() {
    // Backdrop click listeners
    window.addEventListener('click', (e) => {
        if (e.target.id === 'login-modal') window.closeLoginModal();
        if (e.target.id === 'org-modal') window.closeOrgModal();
        if (e.target.id === 'tenant-admin-modal') window.closeTenantAdminModal();
        if (e.target.id === 'org-status-modal') window.closeOrgStatusModal();
        if (e.target.id === 'route-modal') window.closeRouteModal();
        if (e.target.id === 'version-history-modal') window.closeVersionModal();
    });

    // ESC key listener
    window.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            window.closeLoginModal();
            window.closeOrgModal();
            window.closeTenantAdminModal();
            window.closeOrgStatusModal();
            window.closeRouteModal();
            window.closeVersionModal();
        }
    });
}

// =============================================================================
// 10. Forms Submission
// =============================================================================
function setupForms() {
    // Form 1: Sign In
    const loginForm = document.getElementById('login-form');
    loginForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const submitBtn = document.getElementById('submit-login-btn');
        const errorAlert = document.getElementById('login-error-alert');

        submitBtn.textContent = 'Authenticating...';
        submitBtn.disabled = true;
        if (errorAlert) errorAlert.style.display = 'none';

        const username = document.getElementById('login-username').value.trim();
        const password = document.getElementById('login-password').value;

        const result = await window.ApiClient.login(username, password);
        submitBtn.textContent = 'Authenticate';
        submitBtn.disabled = false;

        if (result.success) {
            showToast(`Welcome ${username}! Role: ${result.data.role}`, "success");
            loginForm.reset();
            window.closeLoginModal();
            unlockRoleFromCredentials(result.data.role, result.data);
            await loadLiveRoleData();
        } else {
            if (errorAlert) {
                errorAlert.textContent = result.error;
                errorAlert.style.display = 'block';
            }
            showToast(result.error, "error");
        }
    });

    // Form 2: Organization Form (Create or Edit)
    const orgForm = document.getElementById('org-form');
    orgForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const editId = document.getElementById('org-edit-id').value;
        const orgData = {
            name: document.getElementById('org-name').value.trim(),
            slug: document.getElementById('org-slug').value.trim().toLowerCase()
        };

        try {
            if (editId) {
                await window.ApiClient.updateOrganization(editId, orgData);
                showToast("Organization updated successfully!", "success");
            } else {
                await window.ApiClient.createOrganization(orgData);
                showToast("Organization registered in PENDING status!", "success");
            }
            orgForm.reset();
            window.closeOrgModal();
            await loadLiveRoleData();
        } catch (err) {
            showToast(err.message, "error");
        }
    });

    // Form 3: Provision Tenant Admin
    const adminForm = document.getElementById('tenant-admin-form');
    adminForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const orgId = document.getElementById('admin-org-id').value;
        const adminData = {
            username: document.getElementById('admin-username').value.trim(),
            password: document.getElementById('admin-password').value,
            enabled: document.getElementById('admin-enabled').checked
        };

        try {
            await window.ApiClient.createTenantAdmin(orgId, adminData);
            showToast(`Tenant Administrator "${adminData.username}" saved!`, "success");
            adminForm.reset();
            window.closeTenantAdminModal();
            await loadLiveRoleData();
        } catch (err) {
            showToast(err.message, "error");
        }
    });

    // Form 4: Organization Status Form
    const statusForm = document.getElementById('org-status-form');
    statusForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const orgId = document.getElementById('status-org-id').value;
        const status = document.getElementById('status-select').value;

        try {
            await window.ApiClient.changeOrganizationStatus(orgId, status);
            showToast(`Organization status changed to ${status}`, "success");
            window.closeOrgStatusModal();
            await loadLiveRoleData();
        } catch (err) {
            showToast(err.message, "error");
        }
    });

    // Form 5: Tenant Profile Form
    const profileForm = document.getElementById('tenant-profile-form');
    profileForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const newName = document.getElementById('profile-org-name').value.trim();

        try {
            await window.ApiClient.updateOwnOrganization({ name: newName });
            showToast("Company profile updated successfully!", "success");
            await loadLiveRoleData();
        } catch (err) {
            showToast(err.message, "error");
        }
    });

    // Form 6: Route Form (Create or Edit)
    const routeForm = document.getElementById('route-form');
    routeForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const editId = document.getElementById('route-edit-id').value;

        const selectedMethods = Array.from(routeForm.querySelectorAll('input[name="methods"]:checked')).map(cb => cb.value);
        if (selectedMethods.length === 0) {
            alert("Please select at least one HTTP method.");
            return;
        }

        const routeData = {
            name: document.getElementById('route-name').value.trim(),
            routeId: document.getElementById('route-id').value.trim(),
            pathPattern: document.getElementById('path-pattern').value.trim(),
            targetBaseUrl: document.getElementById('target-url').value.trim(),
            targetPathPrefix: document.getElementById('target-prefix').value.trim() || '/',
            requestsPerMinute: document.getElementById('route-rate-limit').value || null,
            responseTimeoutMs: Number(document.getElementById('route-timeout').value) || 5000,
            allowedMethods: selectedMethods,
            active: document.getElementById('route-active').checked,
            idempotencyEnabled: document.getElementById('route-idempotency').checked,
            headerRules: []
        };

        try {
            if (editId) {
                await window.ApiClient.updateRoute(editId, routeData);
                showToast("Route updated in Spring Boot!", "success");
            } else {
                await window.ApiClient.createRoute(routeData);
                showToast("Route created in Spring Boot!", "success");
            }
            routeForm.reset();
            window.closeRouteModal();
            await loadLiveRoleData();
        } catch (err) {
            showToast(err.message, "error");
        }
    });
}

function setupTableSearchAndFilter() {
    const searchInput = document.getElementById('route-search-input');
    const methodFilter = document.getElementById('method-filter-select');
    searchInput?.addEventListener('input', () => renderRoutesTable());
    methodFilter?.addEventListener('change', () => renderRoutesTable());
}

function setupRefreshButton() {
    const refreshBtn = document.getElementById('refresh-btn');
    const backendStatusPill = document.getElementById('backend-status-pill');

    const handleRefresh = async () => {
        if (refreshBtn) {
            refreshBtn.innerHTML = '<span>⏳</span><span>Syncing...</span>';
            refreshBtn.disabled = true;
        }

        await checkAllServicesHealth();
        await loadLiveRoleData();

        if (refreshBtn) {
            refreshBtn.innerHTML = '<span>🔄</span><span>Refresh</span>';
            refreshBtn.disabled = false;
        }
        showToast("Overview synchronized", "info");
    };

    refreshBtn?.addEventListener('click', handleRefresh);
    backendStatusPill?.addEventListener('click', handleRefresh);
}

function updateGreetingAndTime() {
    const timeElement = document.getElementById('current-time');
    const greetingElement = document.getElementById('greeting-title');

    const now = new Date();
    const currentHour = now.getHours();

    let greeting = "Good morning";
    if (currentHour >= 12 && currentHour < 17) greeting = "Good afternoon";
    else if (currentHour >= 17) greeting = "Good evening";

    const formattedTime = now.toLocaleDateString('en-US', {
        weekday: 'short', year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });

    if (greetingElement) {
        if (currentUser) {
            greetingElement.textContent = `${greeting}, ${currentUser.username}! 👋`;
        } else {
            greetingElement.textContent = "SetuX Gateway Control Plane";
        }
    }

    if (timeElement) timeElement.textContent = `SetuX Console • ${formattedTime}`;
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
    toast.innerHTML = `<span>${icon}</span><span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        toast.style.transition = 'all 0.3s cubic-bezier(0.16, 1, 0.3, 1)';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function escapeHtml(str) {
    if (!str && str !== 0) return '';
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
