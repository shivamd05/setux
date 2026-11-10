/**
 * SetuX API Client Module (js/api.js)
 * 
 * 100% Aligned with SetuX Spring Boot Services:
 * - API Management Service: http://localhost:8081
 * - Smart Gateway:          http://localhost:8080
 * - Analytics Service:      http://localhost:8082
 * 
 * Implements Full Backend Endpoints for all 3 Roles:
 * 1. PLATFORM_ADMIN: Multi-Tenant Onboarding & Admin Provisioning (/api/platform/organizations/**)
 * 2. TENANT_ADMIN:   Organization Settings, API Routes CRUD, Status Toggles, Version Snapshots & Audits (/api/tenant/**)
 * 3. GATEWAY_SERVICE: Fast Proxy Routing Cache Sync (/api/gateway/routes)
 */

(function (window) {
    'use strict';

    // Dynamically align host with frontend origin (127.0.0.1 vs localhost) to prevent macOS IPv6/IPv4 mismatches
    const host = (typeof window !== 'undefined' && window.location.hostname === '127.0.0.1') ? '127.0.0.1' : 'localhost';

    const API_CONFIG = {
        MANAGEMENT_SERVICE_URL: `http://${host}:8081`,
        GATEWAY_SERVICE_URL: `http://${host}:8080`,
        ANALYTICS_SERVICE_URL: `http://${host}:8082`
    };

    const TOKEN_STORAGE_KEY = 'setux_jwt_token';
    const USER_STORAGE_KEY = 'setux_user_data';

    const ApiClient = {
        // =========================================================================
        // 1. Session & Identity Storage
        // =========================================================================
        getToken() {
            try {
                return sessionStorage.getItem(TOKEN_STORAGE_KEY);
            } catch (e) {
                return null;
            }
        },

        setSession(token, userData) {
            try {
                sessionStorage.setItem(TOKEN_STORAGE_KEY, token);
                sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(userData));
            } catch (e) {
                console.error("[ApiClient] Failed to store session:", e);
            }
        },

        clearSession() {
            try {
                sessionStorage.removeItem(TOKEN_STORAGE_KEY);
                sessionStorage.removeItem(USER_STORAGE_KEY);
            } catch (e) {
                console.error("[ApiClient] Failed to clear session:", e);
            }
        },

        getUser() {
            try {
                const data = sessionStorage.getItem(USER_STORAGE_KEY);
                return data ? JSON.parse(data) : null;
            } catch (e) {
                return null;
            }
        },

        getRole() {
            const user = this.getUser();
            return user && user.role ? user.role : 'GUEST';
        },

        getAuthHeaders() {
            const headers = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };
            const token = this.getToken();
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
            return headers;
        },

        // =========================================================================
        // 2. Health Monitoring (All 3 Microservices)
        // =========================================================================
        async checkManagementHealth() {
            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 2000);

                const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/health`, {
                    method: 'GET',
                    signal: controller.signal
                });

                clearTimeout(timeoutId);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const data = await response.json();
                return { online: true, data };
            } catch (error) {
                return { online: false, error: error.message };
            }
        },

        async checkGatewayHealth() {
            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 1500);

                const response = await fetch(`${API_CONFIG.GATEWAY_SERVICE_URL}/health`, {
                    method: 'GET',
                    signal: controller.signal
                });

                clearTimeout(timeoutId);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const data = await response.json();
                return { online: true, data };
            } catch (error) {
                return { online: false, error: error.message };
            }
        },

        async checkAnalyticsHealth() {
            try {
                const controller = new AbortController();
                const timeoutId = setTimeout(() => controller.abort(), 1500);

                const response = await fetch(`${API_CONFIG.ANALYTICS_SERVICE_URL}/health`, {
                    method: 'GET',
                    signal: controller.signal
                });

                clearTimeout(timeoutId);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const data = await response.json();
                return { online: true, data };
            } catch (error) {
                return { online: false, error: error.message };
            }
        },

        // =========================================================================
        // 3. Authentication (/api/auth/login)
        // =========================================================================
        /**
         * Authenticates with Spring Boot /api/auth/login
         * Returns: { token, tokenType, role, organizationId, expiresInSeconds }
         */
        async login(username, password) {
            try {
                const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/auth/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username, password })
                });

                let data;
                const contentType = response.headers.get("content-type");
                if (contentType && contentType.includes("application/json")) {
                    data = await response.json();
                } else {
                    const text = await response.text();
                    data = { message: text };
                }

                if (!response.ok) {
                    throw new Error(data.message || `Authentication failed (HTTP ${response.status})`);
                }

                // Normalise role (e.g. ROLE_PLATFORM_ADMIN -> PLATFORM_ADMIN)
                const rawRole = data.role || '';
                const normalizedRole = rawRole.replace(/^ROLE_/, '');

                const userSession = {
                    username: username,
                    role: normalizedRole,
                    organizationId: data.organizationId || null
                };

                this.setSession(data.token, userSession);

                return {
                    success: true,
                    data: {
                        ...data,
                        role: normalizedRole
                    }
                };
            } catch (error) {
                return { success: false, error: error.message };
            }
        },

        // =========================================================================
        // 4. PLATFORM_ADMIN APIs (/api/platform/organizations/**)
        // =========================================================================
        /** List all non-deleted organizations */
        async getOrganizations() {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/platform/organizations`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to fetch organizations (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Create a new tenant organization in PENDING status */
        async createOrganization(orgData) {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/platform/organizations`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({
                    name: orgData.name,
                    slug: orgData.slug
                })
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to create organization (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Update organization name and slug */
        async updateOrganization(id, orgData) {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/platform/organizations/${id}`, {
                method: 'PUT',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({
                    name: orgData.name,
                    slug: orgData.slug
                })
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to update organization (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Change organization status (PENDING, ACTIVE, SUSPENDED) */
        async changeOrganizationStatus(id, status) {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/platform/organizations/${id}/status`, {
                method: 'PATCH',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ status })
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to update status (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Soft delete organization */
        async deleteOrganization(id) {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/platform/organizations/${id}`, {
                method: 'DELETE',
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to delete organization (HTTP ${response.status})`);
            }
            return true;
        },

        /** Create / Provision Tenant Admin account for an organization */
        async createTenantAdmin(organizationId, adminData) {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/platform/organizations/${organizationId}/tenant-admin`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({
                    username: adminData.username,
                    password: adminData.password,
                    enabled: adminData.enabled !== false
                })
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to provision tenant admin (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Fetch the tenant administrator for an organization */
        async getTenantAdmin(organizationId) {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/platform/organizations/${organizationId}/tenant-admin`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                if (response.status === 404) return null;
                const err = await response.text();
                throw new Error(err || `Failed to load tenant admin (HTTP ${response.status})`);
            }
            return await response.json();
        },

        // =========================================================================
        // 5. TENANT_ADMIN APIs (/api/tenant/**)
        // =========================================================================
        /** Get own organization profile */
        async getOwnOrganization() {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/tenant/organization`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to load tenant profile (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Update own organization profile */
        async updateOwnOrganization(data) {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/tenant/organization`, {
                method: 'PUT',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ name: data.name })
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to update tenant profile (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Lists routes owned by the authenticated tenant */
        async getRoutes() {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/tenant/routes`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to load tenant routes (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Creates a route for the authenticated tenant */
        async createRoute(routeData) {
            const payload = {
                routeId: routeData.routeId,
                name: routeData.name,
                pathPattern: routeData.pathPattern,
                targetBaseUrl: routeData.targetBaseUrl,
                targetPathPrefix: routeData.targetPathPrefix || "/",
                requestsPerMinute: routeData.requestsPerMinute ? Number(routeData.requestsPerMinute) : null,
                responseTimeoutMs: Number(routeData.responseTimeoutMs) || 5000,
                idempotencyEnabled: Boolean(routeData.idempotencyEnabled),
                active: Boolean(routeData.active),
                allowedMethods: routeData.allowedMethods || ["GET"],
                headerRules: routeData.headerRules || []
            };

            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/tenant/routes`, {
                method: 'POST',
                headers: this.getAuthHeaders(),
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to create route (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Updates an existing tenant-owned route */
        async updateRoute(id, routeData) {
            const payload = {
                routeId: routeData.routeId,
                name: routeData.name,
                pathPattern: routeData.pathPattern,
                targetBaseUrl: routeData.targetBaseUrl,
                targetPathPrefix: routeData.targetPathPrefix || "/",
                requestsPerMinute: routeData.requestsPerMinute ? Number(routeData.requestsPerMinute) : null,
                responseTimeoutMs: Number(routeData.responseTimeoutMs) || 5000,
                idempotencyEnabled: Boolean(routeData.idempotencyEnabled),
                active: Boolean(routeData.active),
                allowedMethods: routeData.allowedMethods || ["GET"],
                headerRules: routeData.headerRules || []
            };

            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/tenant/routes/${id}`, {
                method: 'PUT',
                headers: this.getAuthHeaders(),
                body: JSON.stringify(payload)
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to update route (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Fast Toggle route active/inactive */
        async changeRouteStatus(id, active) {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/tenant/routes/${id}/status`, {
                method: 'PATCH',
                headers: this.getAuthHeaders(),
                body: JSON.stringify({ active: Boolean(active) })
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to toggle route status (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Deletes a tenant-owned route */
        async deleteRoute(id) {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/tenant/routes/${id}`, {
                method: 'DELETE',
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to delete route (HTTP ${response.status})`);
            }
            return true;
        },

        /** Fetches route version snapshots */
        async getRouteVersions(routeId) {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/tenant/route-versions/${encodeURIComponent(routeId)}`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to load route versions (HTTP ${response.status})`);
            }
            return await response.json();
        },

        /** Fetches tenant audit records */
        async getAuditRecords() {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/tenant/audit-records`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to load audit records (HTTP ${response.status})`);
            }
            return await response.json();
        },

        // =========================================================================
        // 6. GATEWAY_SERVICE APIs (/api/gateway/**)
        // =========================================================================
        /** Returns all active routes loaded by Smart Gateway */
        async getGatewayRoutes() {
            const response = await fetch(`${API_CONFIG.MANAGEMENT_SERVICE_URL}/api/gateway/routes`, {
                method: 'GET',
                headers: this.getAuthHeaders()
            });
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || `Failed to load gateway routes (HTTP ${response.status})`);
            }
            return await response.json();
        }
    };

    window.ApiClient = ApiClient;

})(window);
