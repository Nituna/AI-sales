/**
 * AI-POWERED MOBILE INVENTORY & SALES ANALYTICS SYSTEM
 * Full-Stack Frontend Client Application
 */

// State Management
let currentUser = null;
let currentBrand = 'Samsung';
let selectedProduct = null;
window.selectedProduct = null;
let currentProduct = null;
let catalogProducts = [];
let selectedCsvFile = null;

// Chart Instances
let chartInstances = {};

// Theme Management & Currency Formatting
function formatINR(amount, includeDecimals = true) {
    if (amount === undefined || amount === null || isNaN(amount)) return '₹0.00';
    const num = Number(amount);
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: includeDecimals ? 2 : 0,
        maximumFractionDigits: includeDecimals ? 2 : 0
    }).format(num);
}

function applyTheme(theme) {
    const isLight = theme === 'light';
    if (isLight) {
        document.documentElement.setAttribute('data-theme', 'light');
        document.body.classList.add('theme-light');
        document.body.classList.remove('theme-dark');
        const text = document.getElementById('themeToggleText');
        if (text) text.textContent = 'Dark Mode';
        document.querySelectorAll('.theme-icon-light').forEach(el => el.classList.remove('hidden'));
        document.querySelectorAll('.theme-icon-dark').forEach(el => el.classList.add('hidden'));
    } else {
        document.documentElement.setAttribute('data-theme', 'dark');
        document.body.classList.add('theme-dark');
        document.body.classList.remove('theme-light');
        const text = document.getElementById('themeToggleText');
        if (text) text.textContent = 'Light Mode';
        document.querySelectorAll('.theme-icon-light').forEach(el => el.classList.add('hidden'));
        document.querySelectorAll('.theme-icon-dark').forEach(el => el.classList.remove('hidden'));
    }
    localStorage.setItem('mobilesales_theme', theme);

    // Re-render open charts with updated theme colors
    if (selectedProduct && selectedProduct.sales) {
        renderWeeklySalesChart(selectedProduct.sales.weeklyHistory || [25, 30, 34, 38]);
        renderMonthlySalesChart(selectedProduct.sales.monthlyHistory || [90, 105, 115, 145]);
        renderProjectedDemandChart(selectedProduct.demand?.projectedDemandCurve || [115, 145, 155, 168, 175, 182]);
    }
}

function toggleTheme() {
    const currentTheme = localStorage.getItem('mobilesales_theme') || 'dark';
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    applyTheme(newTheme);
}

function getChartThemeColors() {
    const isLight = document.documentElement.getAttribute('data-theme') === 'light';
    return {
        textColor: isLight ? '#475569' : '#94a3b8',
        gridColor: isLight ? 'rgba(148, 163, 184, 0.22)' : 'rgba(148, 163, 184, 0.1)',
        xGridColor: isLight ? 'rgba(148, 163, 184, 0.14)' : 'rgba(148, 163, 184, 0.05)'
    };
}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', () => {
    // 1. Initialize Theme from localStorage
    const savedTheme = localStorage.getItem('mobilesales_theme') || 'dark';
    applyTheme(savedTheme);

    // 2. Check if previously logged in session in sessionStorage
    const savedUser = sessionStorage.getItem('mobilesales_user');
    if (savedUser) {
        try {
            currentUser = JSON.parse(savedUser);
            showMainApp();
        } catch (e) {
            sessionStorage.removeItem('mobilesales_user');
        }
    }
});

// ==========================================
// 1. AUTHENTICATION & ROUTING
// ==========================================

async function handleLogin(e) {
    e.preventDefault();
    const alertBox = document.getElementById('loginAlert');
    const alertText = document.getElementById('loginAlertText');
    alertBox.classList.add('hidden');

    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value.trim();

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();

        if (data.success && data.user) {
            currentUser = data.user;
            sessionStorage.setItem('mobilesales_user', JSON.stringify(currentUser));
            showMainApp();
        } else {
            alertText.textContent = data.error || 'Invalid credentials. Please retry.';
            alertBox.classList.remove('hidden');
        }
    } catch (err) {
        alertText.textContent = 'Connection error: ' + err.message;
        alertBox.classList.remove('hidden');
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const alertBox = document.getElementById('registerAlert');
    const alertText = document.getElementById('registerAlertText');
    alertBox.classList.add('hidden');

    const name = document.getElementById('regFullName').value.trim();
    const username = document.getElementById('regUsername').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;
    const confirmPassword = document.getElementById('regConfirmPassword').value;

    if (password !== confirmPassword) {
        alertText.textContent = 'Password and Confirm Password do not match.';
        alertBox.classList.remove('hidden');
        return;
    }

    try {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, username, email, password, confirmPassword })
        });
        const data = await res.json();

        if (data.success) {
            alert('Registration successful! Please login with your credentials.');
            showLoginScreen();
            document.getElementById('loginUsername').value = username;
            document.getElementById('loginPassword').value = password;
        } else {
            alertText.textContent = data.error || 'Registration failed.';
            alertBox.classList.remove('hidden');
        }
    } catch (err) {
        alertText.textContent = 'Server error: ' + err.message;
        alertBox.classList.remove('hidden');
    }
}

function handleLogout() {
    currentUser = null;
    sessionStorage.removeItem('mobilesales_user');
    document.getElementById('appShell').classList.add('hidden');
    showLoginScreen();
}

function showLoginScreen() {
    document.getElementById('registerScreen').classList.add('hidden');
    document.getElementById('loginScreen').classList.remove('hidden');
    document.getElementById('loginAlert').classList.add('hidden');
}

function showRegistrationScreen() {
    document.getElementById('loginScreen').classList.add('hidden');
    document.getElementById('registerScreen').classList.remove('hidden');
    document.getElementById('registerAlert').classList.add('hidden');
}

function showMainApp() {
    document.getElementById('loginScreen').classList.add('hidden');
    document.getElementById('registerScreen').classList.add('hidden');
    document.getElementById('appShell').classList.remove('hidden');

    // Update profile
    document.getElementById('userNameLabel').textContent = currentUser.name || currentUser.username;
    document.getElementById('userAvatar').textContent = (currentUser.name || currentUser.username).charAt(0).toUpperCase();
    document.getElementById('userRoleLabel').textContent = currentUser.role || 'ANALYST';

    // Load initial Brand Selection
    onBrandSelected('Samsung');
}

function showForgotPasswordModal(e) {
    if (e) e.preventDefault();
    document.getElementById('forgotPasswordModal').classList.remove('hidden');
}

function closeForgotPasswordModal() {
    document.getElementById('forgotPasswordModal').classList.add('hidden');
}

// ==========================================
// 2. MAIN VIEW SWITCHER (SIDEBAR)
// ==========================================

function switchMainView(viewId, navElement = null) {
    // Hide all views
    document.querySelectorAll('.view-panel').forEach(el => el.classList.add('hidden'));
    const target = document.getElementById(viewId);
    if (target) {
        target.classList.remove('hidden');
    }

    // Update sidebar active link
    if (navElement) {
        document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
        navElement.classList.add('active');
    }

    if (viewId === 'brandSelectionView') {
        document.getElementById('topNavTitle').textContent = 'Mobile Inventory & Sales Analytics — Brand Selection';
    } else if (viewId === 'productDashboardView') {
        document.getElementById('topNavTitle').textContent = 'Product Analytics Dashboard';
    } else if (viewId === 'executiveDashboardView') {
        document.getElementById('topNavTitle').textContent = 'Executive Supply Chain & Sales Intelligence';
        loadExecutiveDashboard();
    } else if (viewId === 'dataEntryView') {
        document.getElementById('topNavTitle').textContent = 'Data Management & CSV Ingestion';
    }
}

// ==========================================
// 3. BRAND SELECTION, MODELS GRID & PRODUCT SEARCH
// ==========================================

let searchDebounceTimer = null;
let isSearchActive = false;

function handleProductSearchInput(query) {
    const clearBtn = document.getElementById('clearSearchBtn');
    const trimmed = (query || '').trim();

    if (trimmed.length > 0) {
        clearBtn?.classList.remove('hidden');
    } else {
        clearBtn?.classList.add('hidden');
    }

    if (searchDebounceTimer) {
        clearTimeout(searchDebounceTimer);
    }

    if (!trimmed) {
        if (isSearchActive) {
            clearProductSearch(false);
        }
        return;
    }

    searchDebounceTimer = setTimeout(() => {
        executeProductSearch(trimmed);
    }, 280);
}

async function executeProductSearch(query) {
    if (!query || !query.trim()) return;
    const cleanQuery = query.trim();
    isSearchActive = true;

    // Switch view to brand selection view where search results are presented
    switchMainView('brandSelectionView', document.getElementById('navBrandSelection'));

    const heading = document.getElementById('modelsSectionHeading');
    const subheading = document.getElementById('modelsSectionSubheading');
    const exitBtn = document.getElementById('exitSearchBtn');
    const badge = document.getElementById('modelCountBadge');
    const container = document.getElementById('modelsGridContainer');

    if (heading) heading.textContent = `Search Results for "${cleanQuery}"`;
    if (subheading) subheading.textContent = 'Displaying matching smartphone records from database';
    if (exitBtn) exitBtn.classList.remove('hidden');
    if (badge) badge.textContent = 'Searching database...';

    // Deselect brand chips to indicate global search
    document.querySelectorAll('.brand-chip').forEach(c => c.classList.remove('active'));

    try {
        const res = await fetch(`/api/products/search?q=${encodeURIComponent(cleanQuery)}`);
        const data = await res.json();

        if (data.success && Array.isArray(data.products)) {
            renderModelsGrid(data.products, true);
        } else {
            renderModelsGrid([], true);
        }

        // Smooth scroll to search results
        setTimeout(() => {
            const modelsSec = document.getElementById('availableModelsSection') || container;
            if (modelsSec) {
                modelsSec.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }, 50);
    } catch (err) {
        console.error('Error during product search:', err);
        renderModelsGrid([], true);
    }
}

function clearProductSearch(reloadCurrentBrand = true) {
    isSearchActive = false;
    const searchInput = document.getElementById('globalProductSearchInput');
    const clearBtn = document.getElementById('clearSearchBtn');
    const exitBtn = document.getElementById('exitSearchBtn');
    const heading = document.getElementById('modelsSectionHeading');
    const subheading = document.getElementById('modelsSectionSubheading');

    if (searchInput) searchInput.value = '';
    if (clearBtn) clearBtn.classList.add('hidden');
    if (exitBtn) exitBtn.classList.add('hidden');
    if (heading) heading.textContent = 'Available Smartphone Models';
    if (subheading) subheading.textContent = 'Select any smartphone to view sales metrics, inventory status, and AI insights';

    if (reloadCurrentBrand) {
        onBrandSelected(currentBrand);
    }
}

async function onBrandSelected(brand, chipElement = null) {
    currentBrand = brand;
    document.getElementById('currentBrandLabel').textContent = brand;
    document.getElementById('brandDropdownSelect').value = brand;

    // If search was active, reset search input and headings
    if (isSearchActive) {
        isSearchActive = false;
        const searchInput = document.getElementById('globalProductSearchInput');
        const clearBtn = document.getElementById('clearSearchBtn');
        const exitBtn = document.getElementById('exitSearchBtn');
        if (searchInput) searchInput.value = '';
        if (clearBtn) clearBtn.classList.add('hidden');
        if (exitBtn) exitBtn.classList.add('hidden');
        document.getElementById('modelsSectionHeading').textContent = 'Available Smartphone Models';
        document.getElementById('modelsSectionSubheading').textContent = 'Select any smartphone to view sales metrics, inventory status, and AI insights';
    }

    // Highlight chip
    if (chipElement) {
        document.querySelectorAll('.brand-chip').forEach(c => c.classList.remove('active'));
        chipElement.classList.add('active');
    } else {
        document.querySelectorAll('.brand-chip').forEach(c => {
            if (c.querySelector('.brand-chip-name').textContent.trim().toLowerCase() === brand.toLowerCase()) {
                c.classList.add('active');
            } else {
                c.classList.remove('active');
            }
        });
    }

    // Fetch models for brand
    try {
        const res = await fetch(`/api/products?brand=${encodeURIComponent(brand)}`);
        const data = await res.json();
        if (data.success && data.products) {
            renderModelsGrid(data.products, false);
            setTimeout(() => {
                const modelsSec = document.getElementById('availableModelsSection') || document.getElementById('modelsGridContainer');
                if (modelsSec) {
                    modelsSec.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }
            }, 60);
        }
    } catch (err) {
        console.error('Error fetching brand models:', err);
    }
}

function renderModelsGrid(products, isSearchResult = false) {
    const container = document.getElementById('modelsGridContainer');
    container.innerHTML = '';

    if (!products || products.length === 0) {
        if (isSearchResult) {
            document.getElementById('modelCountBadge').textContent = '0 Models Found';
            container.innerHTML = `
                <div class="no-results-card" style="grid-column: 1 / -1;">
                    <i class="fas fa-search-minus" style="font-size: 42px; color: var(--rose-500); margin-bottom: 14px;"></i>
                    <h3 style="margin-bottom: 8px; color: var(--text-primary); font-size: 18px; font-weight: 700;">No matching product found.</h3>
                    <p style="color: var(--text-muted); font-size: 13px; max-width: 480px; margin: 0 auto 18px auto; line-height: 1.6;">
                        No smartphone records match your search criteria. Check your spelling, try searching with fewer keywords (e.g. brand name or series), or restore the product listing.
                    </p>
                    <button class="btn btn-secondary" onclick="clearProductSearch()">
                        <i class="fas fa-rotate-left"></i> Restore Normal Product Listing
                    </button>
                </div>
            `;
        } else {
            document.getElementById('modelCountBadge').textContent = '0 Models Available';
            container.innerHTML = `
                <div class="no-results-card" style="grid-column: 1 / -1;">
                    <i class="fas fa-box-open" style="font-size: 38px; color: var(--text-muted); margin-bottom: 12px;"></i>
                    <h3 style="margin-bottom: 6px; color: var(--text-primary); font-size: 16px;">No models currently available for this brand</h3>
                    <p style="color: var(--text-muted); font-size: 13px;">Import products using the CSV Data Ingestion tool or add new models manually.</p>
                </div>
            `;
        }
        return;
    }

    document.getElementById('modelCountBadge').textContent = isSearchResult
        ? `${products.length} Model${products.length === 1 ? '' : 's'} Found`
        : `${products.length} Models Available`;

    products.forEach(p => {
        const card = document.createElement('div');
        card.className = 'glass-card model-card fade-in';
        card.onclick = () => openProductDashboard(p.productId);

        const stockUnits = p.currentStock !== undefined && p.currentStock !== null ? p.currentStock : null;
        const stockBadge = stockUnits !== null
            ? `<span class="badge ${stockUnits <= 15 ? 'badge-rose' : stockUnits <= 40 ? 'badge-amber' : 'badge-green'}" style="font-size: 11px; font-weight: 600;"><i class="fas fa-boxes-stacked"></i> Stock: ${stockUnits} units</span>`
            : '';
        const salesBadge = p.monthlySales
            ? `<span class="badge badge-cyan" style="font-size: 11px; font-weight: 600;"><i class="fas fa-chart-line"></i> Sales: ${p.monthlySales} units</span>`
            : '';

        card.innerHTML = `
            <div class="model-header">
                <div>
                    <div style="display: flex; gap: 6px; align-items: center; margin-bottom: 4px; flex-wrap: wrap;">
                        <span style="font-size: 11px; text-transform: uppercase; color: var(--cyan-400); font-weight: 700;">${escapeHtml(p.brand)}</span>
                        <span class="badge badge-cyan" style="font-size: 10px; padding: 2px 6px;">ID: #${p.productId}</span>
                        ${p.category ? `<span style="font-size: 11px; color: var(--text-muted);">• ${escapeHtml(p.category)}</span>` : ''}
                    </div>
                    <h3 class="model-name">${escapeHtml(p.modelName)}</h3>
                </div>
                <div class="model-price">${formatINR(p.price)}</div>
            </div>
            <div class="model-specs-line">
                <i class="fas fa-microchip"></i> ${escapeHtml(p.processor || 'Standard Octa-core')}<br>
                <i class="fas fa-memory"></i> ${escapeHtml(p.ram || '8GB')} / ${escapeHtml(p.storage || '128GB')} • <i class="fas fa-battery-full"></i> ${escapeHtml(p.battery || '5000 mAh')}
                ${p.display ? `<br><i class="fas fa-mobile-screen"></i> ${escapeHtml(p.display)}` : ''}
                ${p.camera ? ` • <i class="fas fa-camera"></i> ${escapeHtml(p.camera)}` : ''}
            </div>
            ${(stockBadge || salesBadge) ? `<div style="display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 12px;">${stockBadge}${salesBadge}</div>` : ''}
            <div class="model-indicators">
                <span style="color: var(--text-muted); font-size: 12px;"><i class="fas fa-layer-group"></i> ${escapeHtml(p.category || 'Smartphone')}</span>
                <span style="margin-left: auto; color: var(--cyan-400); font-weight: 600; font-size: 13px;">
                    View Dashboard <i class="fas fa-chevron-right"></i>
                </span>
            </div>
        `;
        container.appendChild(card);
    });
}

// ==========================================
// 4. MOBILE PRODUCT DASHBOARD
// ==========================================

async function openProductDashboard(productId) {
    try {
        const res = await fetch(`/api/product/dashboard?productId=${productId}`);
        const data = await res.json();
        if (!data.success || !data.dashboard) {
            alert('Failed to load product dashboard: ' + (data.error || 'Unknown error'));
            return;
        }

        // Single Source of Truth for Currently Selected Product
        const d = data.dashboard;
        selectedProduct = d;
        window.selectedProduct = d;
        currentProduct = d;

        // Temporary debug logging requested for verification
        console.log("Selected Brand:", currentBrand);
        console.log("Selected Product:", selectedProduct?.product?.modelName);
        console.log("Product ID:", selectedProduct?.product?.productId);
        console.log("Product Sales:", selectedProduct?.sales?.currentMonthlySales);
        console.log("Product Stock:", selectedProduct?.inventory?.currentStock);
        console.log("Product Visibility:", selectedProduct?.aiSearch?.visibilityScore);

        // Switch view to product dashboard
        switchMainView('productDashboardView', document.getElementById('navProductDashboard'));

        // BUG 2 FIX: Automatically navigate/scroll to the product analytics dashboard without manual scrolling
        setTimeout(() => {
            const dashView = document.getElementById('productDashboardView');
            if (dashView) {
                dashView.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }, 60);

        // Populate Header from selectedProduct
        document.getElementById('prodBrandTag').textContent = selectedProduct.product.brand;
        document.getElementById('prodCategoryTag').textContent = selectedProduct.product.category;
        document.getElementById('prodModelTitle').textContent = selectedProduct.product.modelName;
        document.getElementById('prodPriceTag').textContent = formatINR(selectedProduct.product.price);

        // Classification badge
        const badge = document.getElementById('prodClassBadge');
        badge.innerHTML = `<i class="${selectedProduct.classification.icon}"></i> ${escapeHtml(selectedProduct.classification.name)}`;
        badge.style.background = selectedProduct.classification.color + '25';
        badge.style.color = selectedProduct.classification.color;
        badge.style.borderColor = selectedProduct.classification.color + '60';

        // 1. Specs Tab from selectedProduct
        document.getElementById('specProcessor').textContent = selectedProduct.product.processor || 'N/A';
        document.getElementById('specMemory').textContent = `${selectedProduct.product.ram || '8GB'} RAM / ${selectedProduct.product.storage || '128GB'} Storage`;
        document.getElementById('specDisplay').textContent = selectedProduct.product.display || 'N/A';
        document.getElementById('specCamera').textContent = selectedProduct.product.camera || 'N/A';
        document.getElementById('specBattery').textContent = selectedProduct.product.battery || 'N/A';
        document.getElementById('specOS').textContent = selectedProduct.product.os || 'N/A';
        document.getElementById('specNetwork').textContent = selectedProduct.product.network || '5G';
        document.getElementById('specLaunch').textContent = selectedProduct.product.launchDate ? `${selectedProduct.product.launchDate.year}-${selectedProduct.product.launchDate.month}-${selectedProduct.product.launchDate.day}` : 'Recent';

        // 2. Sales Tab from selectedProduct
        document.getElementById('salesTotalUnits').textContent = (selectedProduct.sales.totalUnitsSold || 0).toLocaleString();
        document.getElementById('salesWeekly').textContent = (selectedProduct.sales.currentWeeklySales || 0) + ' units';
        document.getElementById('salesMonthly').textContent = (selectedProduct.sales.currentMonthlySales || 0) + ' units';
        document.getElementById('salesPrevMonth').textContent = (selectedProduct.sales.previousMonthSales || 0) + ' units';

        const growth = selectedProduct.sales.salesGrowthPct || 0;
        document.getElementById('salesGrowthPct').textContent = `${growth >= 0 ? '+' : ''}${growth.toFixed(1)}%`;
        const growthBadge = document.getElementById('salesGrowthBadge');
        growthBadge.textContent = growth >= 0 ? 'Upward Trend' : 'Downward Trend';
        growthBadge.className = `metric-badge ${growth >= 0 ? 'badge-green' : 'badge-rose'}`;

        document.getElementById('salesPredictedFuture').textContent = (selectedProduct.sales.predictedFutureSales || 0) + ' units';

        // Render Sales Charts
        renderWeeklySalesChart(selectedProduct.sales.weeklyHistory || [25, 30, 34, 38]);
        renderMonthlySalesChart(selectedProduct.sales.monthlyHistory || [90, 105, 115, 145]);

        // 3. Inventory Tab from selectedProduct
        document.getElementById('invCurrentStock').textContent = (selectedProduct.inventory.currentStock || 0) + ' units';
        document.getElementById('invSafetyStock').textContent = (selectedProduct.inventory.safetyStock || 0) + ' units';
        document.getElementById('invReorderLevel').textContent = (selectedProduct.inventory.reorderLevel || 0) + ' units';
        document.getElementById('invLeadTime').textContent = (selectedProduct.inventory.supplierLeadTimeDays || 7) + ' days';
        document.getElementById('invAvailableStock').textContent = (selectedProduct.inventory.availableStock || 0) + ' units';

        const invStatus = document.getElementById('invStatusLabel');
        invStatus.textContent = selectedProduct.inventory.inventoryStatus || 'In Stock';
        invStatus.style.color = selectedProduct.inventory.statusColor || 'var(--emerald-500)';

        // 4. AI Search Tab from selectedProduct
        updateAiSearchTab(selectedProduct.aiSearch);

        // 5. Demand Tab from selectedProduct
        document.getElementById('demandCurrent').textContent = `${selectedProduct.demand.currentDemand} units`;
        document.getElementById('demandPredicted').textContent = `${selectedProduct.demand.predictedDemand} units`;
        const demGrowth = selectedProduct.demand.demandGrowthPct || 0;
        document.getElementById('demandGrowth').textContent = `${demGrowth >= 0 ? '+' : ''}${demGrowth.toFixed(1)}%`;
        document.getElementById('demandTrendLabel').textContent = selectedProduct.demand.demandTrend || 'Stable';
        document.getElementById('demandInsightsText').textContent = selectedProduct.demand.analyticalInsights || '';

        renderProjectedDemandChart(selectedProduct.demand.projectedDemandCurve || [115, 145, 155, 168, 175, 182]);

        // 6. Reorder Tab from selectedProduct
        const rec = selectedProduct.recommendation;
        const recCard = document.getElementById('reorderActionCard');
        const recBadge = document.getElementById('reorderStatusBadge');

        const isReorder = rec.status === 'REORDER RECOMMENDED';
        recBadge.textContent = isReorder ? '⚠️ REORDER RECOMMENDED' : '✓ STOCK ADEQUATE';
        recBadge.className = `reorder-status-badge ${isReorder ? 'recommended' : 'adequate'}`;
        recCard.className = `reorder-action-card ${rec.urgent ? 'urgent' : ''}`;

        document.getElementById('recCurrentStock').textContent = `${rec.currentStock} units`;
        document.getElementById('recPredictedDemand').textContent = `${rec.predictedDemand} units`;
        document.getElementById('recReorderLevel').textContent = `${rec.reorderLevel} units`;
        document.getElementById('recRecommendedQty').textContent = `${rec.recommendedReorderQty} units`;
        document.getElementById('recRecommendedQty').style.color = isReorder ? 'var(--rose-500)' : 'var(--emerald-500)';

        // Explanatory checklist
        const checklist = document.getElementById('reorderChecklist');
        checklist.innerHTML = '';
        (rec.reasonsList || []).forEach(r => {
            const li = document.createElement('li');
            li.className = 'reorder-checklist-item';
            li.innerHTML = `<i class="fas fa-check-circle" style="color: ${isReorder ? 'var(--rose-500)' : 'var(--emerald-500)'};"></i> ${escapeHtml(r)}`;
            checklist.appendChild(li);
        });

    } catch (err) {
        console.error('Error opening product dashboard:', err);
    }
}

function switchProductTab(tabId, btn) {
    document.querySelectorAll('.prod-tab-content').forEach(el => el.classList.add('hidden'));
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));

    const target = document.getElementById(tabId);
    if (target) target.classList.remove('hidden');
    if (btn) btn.classList.add('active');
}

function updateAiSearchTab(ai) {
    if (!ai) ai = {};
    const visScore = ai.visibilityScore;
    const accScore = ai.accuracyScore;
    const engScore = ai.engagementScore;
    const isUnavailable = ai.analysisStatus === 'UNAVAILABLE';

    // Timestamp
    const timeEl = document.getElementById('aiLastAnalyzed');
    if (timeEl) {
        timeEl.textContent = ai.analyzedAt || (isUnavailable ? 'Analysis Unavailable' : 'Not yet analyzed');
    }

    // Status alert banner
    const alertBanner = document.getElementById('aiStatusAlert');
    if (alertBanner) {
        if (isUnavailable || (ai.statusMessage && ai.statusMessage.toLowerCase().includes('unavailable'))) {
            alertBanner.style.display = 'block';
            alertBanner.style.background = 'rgba(244, 63, 94, 0.15)';
            alertBanner.style.border = '1px solid var(--rose-500)';
            alertBanner.style.color = '#fda4af';
            alertBanner.innerHTML = `<i class="fas fa-triangle-exclamation"></i> <strong>Notice:</strong> ${escapeHtml(ai.statusMessage || 'AI analysis temporarily unavailable.')}`;
        } else {
            alertBanner.style.display = 'none';
        }
    }

    // Core Metrics
    if (visScore !== undefined && visScore !== null && !isUnavailable) {
        document.getElementById('aiVisibilityScore').textContent = `${visScore.toFixed(1)}%`;
        document.getElementById('aiVisibilityBar').style.width = `${Math.min(100, Math.max(0, visScore))}%`;
    } else {
        document.getElementById('aiVisibilityScore').textContent = 'Not Available';
        document.getElementById('aiVisibilityBar').style.width = '0%';
    }

    if (accScore !== undefined && accScore !== null && !isUnavailable) {
        document.getElementById('aiAccuracyScore').textContent = `${accScore.toFixed(1)}%`;
        document.getElementById('aiAccuracyBar').style.width = `${Math.min(100, Math.max(0, accScore))}%`;
    } else {
        document.getElementById('aiAccuracyScore').textContent = 'Not Available';
        document.getElementById('aiAccuracyBar').style.width = '0%';
    }

    if (engScore !== undefined && engScore !== null && !isUnavailable) {
        document.getElementById('aiEngagementScore').textContent = `${engScore.toFixed(1)}%`;
        document.getElementById('aiEngagementBar').style.width = `${Math.min(100, Math.max(0, engScore))}%`;
    } else {
        document.getElementById('aiEngagementScore').textContent = 'Not Available';
        document.getElementById('aiEngagementBar').style.width = '0%';
    }

    document.getElementById('aiTotalQueries').textContent = (ai.totalQueries || 0).toLocaleString();
    document.getElementById('aiAppearances').textContent = `${(ai.appearances || 0).toLocaleString()} (${(visScore || 0).toFixed(1)}%)`;
    document.getElementById('aiBrandMentions').textContent = (ai.brandMentions || 0).toLocaleString();
    document.getElementById('aiCompQueries').textContent = (ai.competitorComparisons || 0).toLocaleString();

    document.getElementById('engSearches').textContent = ai.productSearches || 0;
    document.getElementById('engComparisons').textContent = ai.comparisonQueries || 0;
    document.getElementById('engRecommendations').textContent = ai.recommendationQueries || 0;
    document.getElementById('engReviews').textContent = ai.positiveReviews || 0;
    document.getElementById('engRepeat').textContent = ai.repeatSearches || 0;

    renderClaimsBreakdown(ai.claimsList || ai.claimComparisons || []);
}

function renderClaimsBreakdown(claims) {
    const container = document.getElementById('claimsListContainer');
    if (!container) return;
    container.innerHTML = '';

    if (!claims || claims.length === 0) {
        container.innerHTML = '<div style="color: var(--text-muted); font-size: 13px;">No spec claims available for current product.</div>';
        return;
    }

    claims.forEach(c => {
        const item = document.createElement('div');
        item.style.cssText = 'padding: 10px 14px; background: rgba(15, 23, 42, 0.4); border-radius: var(--radius-sm); font-size: 13px; display: flex; flex-direction: column; gap: 4px; border-left: 3px solid ' + (c.isCorrect ? 'var(--emerald-500)' : 'var(--rose-500)') + ';';
        item.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <strong style="color: var(--text-primary); font-size: 13px;">${escapeHtml(c.feature)}</strong>
                ${c.isCorrect ?
                    '<span class="metric-badge badge-green" style="font-size: 11px;"><i class="fas fa-check"></i> Correct</span>' :
                    '<span class="metric-badge badge-rose" style="font-size: 11px;"><i class="fas fa-times"></i> Discrepancy</span>'}
            </div>
            <div style="display: flex; flex-wrap: wrap; gap: 14px; font-size: 12px; margin-top: 2px;">
                <span style="color: var(--text-secondary);"><strong style="color: var(--text-muted);">Verified:</strong> ${escapeHtml(c.verifiedValue || 'N/A')}</span>
                <span style="color: var(--cyan-400);"><strong style="color: var(--text-muted);">AI/Search Claim:</strong> ${escapeHtml(c.aiClaimedValue || 'N/A')}</span>
            </div>
            ${c.sourceSnippet ? `<div style="font-size: 11px; color: var(--text-muted); font-style: italic;">${escapeHtml(c.sourceSnippet)}</div>` : ''}
        `;
        container.appendChild(item);
    });
}

async function refreshAiAnalysis() {
    if (!currentProduct || !currentProduct.product) return;
    const pid = currentProduct.product.productId;
    const btn = document.getElementById('btnRefreshAi');
    const icon = document.getElementById('aiRefreshIcon');

    if (btn) btn.disabled = true;
    if (icon) icon.classList.add('fa-spin');

    try {
        const res = await fetch(`/api/ai-search/refresh?productId=${pid}`, { method: 'POST' });
        const json = await res.json();
        if (json.success && json.aiSearch) {
            currentProduct.aiSearch = json.aiSearch;
            if (json.demand) currentProduct.demand = json.demand;
            if (json.recommendation) currentProduct.recommendation = json.recommendation;

            updateAiSearchTab(currentProduct.aiSearch);
        } else {
            alert('AI Analysis notice: ' + (json.error || json.message || 'AI analysis temporarily unavailable.'));
        }
    } catch (err) {
        console.error('Refresh AI Analysis error:', err);
        alert('Could not refresh AI analysis: ' + err.message);
    } finally {
        if (btn) btn.disabled = false;
        if (icon) icon.classList.remove('fa-spin');
    }
}

// ==========================================
// 5. CHART.JS VISUALIZATION ENGINE
// ==========================================

function renderWeeklySalesChart(data) {
    const ctx = document.getElementById('weeklySalesChart');
    if (!ctx) return;
    if (chartInstances['weeklySales']) chartInstances['weeklySales'].destroy();

    const cols = getChartThemeColors();
    chartInstances['weeklySales'] = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4 (Current)'],
            datasets: [{
                label: 'Units Sold',
                data: data,
                backgroundColor: 'rgba(6, 182, 212, 0.65)',
                borderColor: '#06b6d4',
                borderWidth: 1.5,
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                y: { grid: { color: cols.gridColor }, ticks: { color: cols.textColor } },
                x: { grid: { display: false }, ticks: { color: cols.textColor } }
            }
        }
    });
}

function renderMonthlySalesChart(data) {
    const ctx = document.getElementById('monthlySalesChart');
    if (!ctx) return;
    if (chartInstances['monthlySales']) chartInstances['monthlySales'].destroy();

    const cols = getChartThemeColors();
    chartInstances['monthlySales'] = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['Month -3', 'Month -2', 'Month -1', 'Current Month'],
            datasets: [{
                label: 'Monthly Units Sold',
                data: data,
                borderColor: '#10b981',
                backgroundColor: 'rgba(16, 185, 129, 0.15)',
                fill: true,
                tension: 0.35,
                borderWidth: 2.5,
                pointBackgroundColor: '#10b981',
                pointRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                y: { grid: { color: cols.gridColor }, ticks: { color: cols.textColor } },
                x: { grid: { display: false }, ticks: { color: cols.textColor } }
            }
        }
    });
}

function renderProjectedDemandChart(data) {
    const ctx = document.getElementById('projectedDemandChart');
    if (!ctx) return;
    if (chartInstances['projectedDemand']) chartInstances['projectedDemand'].destroy();

    const cols = getChartThemeColors();
    chartInstances['projectedDemand'] = new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['M-1 (Actual)', 'Month 0 (Current)', 'Month +1 (Pred)', 'Month +2 (Pred)', 'Month +3 (Pred)', 'Month +4 (Pred)'],
            datasets: [{
                label: 'Projected Demand Curve',
                data: data,
                borderColor: '#38bdf8',
                backgroundColor: 'rgba(56, 189, 248, 0.15)',
                fill: true,
                borderDash: [0, 0, 5, 5, 5, 5],
                tension: 0.35,
                borderWidth: 3,
                pointBackgroundColor: '#38bdf8',
                pointRadius: 5
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                y: { grid: { color: cols.gridColor }, ticks: { color: cols.textColor } },
                x: { grid: { display: false }, ticks: { color: cols.textColor } }
            }
        }
    });
}

// ==========================================
// 6. EXECUTIVE DASHBOARD & 6 AGGREGATE CHARTS
// ==========================================

async function loadExecutiveDashboard() {
    try {
        const res = await fetch('/api/main-dashboard');
        const data = await res.json();
        if (!data.success || !data.mainDashboard) return;

        const m = data.mainDashboard;
        catalogProducts = m.products || [];

        // Top 7 Cards
        document.getElementById('execTotalModels').textContent = m.totalModels;
        document.getElementById('execTotalSold').textContent = m.totalUnitsSold.toLocaleString();
        document.getElementById('execTotalStock').textContent = m.totalStock.toLocaleString();
        document.getElementById('execReorderNeeded').textContent = m.reordersRequired;
        document.getElementById('execAvgVis').textContent = `${m.avgVisibility.toFixed(1)}%`;
        document.getElementById('execAvgAcc').textContent = `${m.avgAccuracy.toFixed(1)}%`;
        document.getElementById('execAvgEng').textContent = `${m.avgEngagement.toFixed(1)}%`;

        // Render Alerts
        renderAlerts(m.alerts || []);

        // Populate Table
        renderCatalogTable(catalogProducts);

        // Render 6 Charts
        renderExecutiveCharts(m);

    } catch (err) {
        console.error('Error loading executive dashboard:', err);
    }
}

function renderAlerts(alerts) {
    const container = document.getElementById('alertsContainer');
    container.innerHTML = '';

    if (!alerts || alerts.length === 0) {
        container.innerHTML = '<div style="color: var(--text-muted); font-size: 13px;">No critical supply chain alerts triggered at this time.</div>';
        return;
    }

    alerts.slice(0, 6).forEach(a => {
        const item = document.createElement('div');
        const colorClass = a.type === 'LOW_STOCK' ? 'danger' :
                           a.type === 'REORDER_REQUIRED' ? 'warning' :
                           a.type === 'POTENTIAL_DEMAND_INCREASE' ? 'warning' :
                           a.type === 'OVERSTOCK' ? 'purple' : 'info';

        item.className = `alert-item-card ${colorClass} fade-in`;
        item.innerHTML = `
            <div style="font-size: 18px; margin-top: 2px;"><i class="fas fa-triangle-exclamation"></i></div>
            <div>
                <div style="font-size: 12px; font-weight: 700; color: ${a.color || 'var(--cyan-400)'}; margin-bottom: 2px;">
                    ${escapeHtml(a.badge)}
                </div>
                <div style="font-size: 13px; color: var(--text-primary); line-height: 1.4;">
                    ${escapeHtml(a.message)}
                </div>
            </div>
        `;
        container.appendChild(item);
    });
}

function renderCatalogTable(products) {
    const tbody = document.getElementById('catalogTableBody');
    tbody.innerHTML = '';

    products.forEach(p => {
        const tr = document.createElement('tr');
        const isReorder = p.reorderStatus === 'REORDER RECOMMENDED';
        const classColor = p.classification.includes('Potential') ? 'badge-amber' :
                           p.classification.includes('Fast') ? 'badge-green' :
                           p.classification.includes('Medium') ? 'badge-cyan' : 'badge-rose';

        tr.innerHTML = `
            <td><strong>${escapeHtml(p.brand)}</strong></td>
            <td>${escapeHtml(p.modelName)}</td>
            <td>${formatINR(p.price)}</td>
            <td>${p.currentStock}</td>
            <td>${p.monthlySales}</td>
            <td>${p.predictedDemand}</td>
            <td>${p.visibilityScore.toFixed(0)}%</td>
            <td>${p.accuracyScore.toFixed(0)}%</td>
            <td>${p.engagementScore.toFixed(0)}%</td>
            <td><span class="metric-badge ${classColor}">${escapeHtml(p.classification)}</span></td>
            <td><span class="metric-badge ${isReorder ? 'badge-rose' : 'badge-green'}">${escapeHtml(p.reorderStatus)}</span></td>
            <td>
                <button class="btn btn-secondary" style="padding: 4px 10px; font-size: 11px;" onclick="openProductDashboard(${p.productId})">
                    Analyze
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function filterCatalogTable(query) {
    const q = (query || '').toLowerCase().trim();
    const tbody = document.getElementById('catalogTableBody');
    if (!q) {
        renderCatalogTable(catalogProducts);
        return;
    }
    const tokens = q.split(/\s+/).filter(Boolean);
    const filtered = catalogProducts.filter(p => {
        const text = `${p.brand} ${p.modelName} ${p.classification || ''} ${p.reorderStatus || ''}`.toLowerCase();
        return tokens.every(tok => text.includes(tok));
    });

    if (filtered.length === 0) {
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="12" style="text-align: center; color: var(--text-muted); padding: 36px 16px; font-size: 14px;">
                        <i class="fas fa-search" style="margin-right: 8px; color: var(--rose-400);"></i>
                        No matching product found for "<strong>${escapeHtml(query)}</strong>".
                    </td>
                </tr>
            `;
        }
    } else {
        renderCatalogTable(filtered);
    }
}

function renderExecutiveCharts(m) {
    const products = m.products || [];

    // Chart 1: Sales Trend
    renderChart('execSalesTrendChart', 'line', {
        labels: ['Month -3', 'Month -2', 'Month -1', 'Current Month'],
        datasets: [{
            label: 'Total Units Sold Across Brands',
            data: [1240, 1380, 1520, 1680],
            borderColor: '#38bdf8',
            backgroundColor: 'rgba(56, 189, 248, 0.12)',
            fill: true,
            tension: 0.35,
            borderWidth: 2.5
        }]
    });

    // Chart 2: Sales vs Visibility
    const sampleP = products.slice(0, 8);
    renderChart('execSalesVsVisChart', 'scatter', {
        datasets: [{
            label: 'Product Sales vs Search Visibility',
            data: sampleP.map(p => ({ x: p.visibilityScore, y: p.monthlySales })),
            backgroundColor: '#06b6d4',
            pointRadius: 6
        }]
    }, {
        scales: {
            x: { title: { display: true, text: 'Visibility Score (%)', color: '#94a3b8' } },
            y: { title: { display: true, text: 'Monthly Sales (Units)', color: '#94a3b8' } }
        }
    });

    // Chart 3: Engagement vs Demand
    renderChart('execEngVsDemandChart', 'scatter', {
        datasets: [{
            label: 'Engagement vs Predicted Demand',
            data: sampleP.map(p => ({ x: p.engagementScore, y: p.predictedDemand })),
            backgroundColor: '#a855f7',
            pointRadius: 6
        }]
    }, {
        scales: {
            x: { title: { display: true, text: 'Customer Engagement Score (%)', color: '#94a3b8' } },
            y: { title: { display: true, text: 'Predicted Demand (Units)', color: '#94a3b8' } }
        }
    });

    // Chart 4: Stock vs Predicted Demand
    renderChart('execStockVsDemandChart', 'bar', {
        labels: sampleP.map(p => p.modelName.split(' ').slice(0, 2).join(' ')),
        datasets: [
            { label: 'Current Stock', data: sampleP.map(p => p.currentStock), backgroundColor: 'rgba(6, 182, 212, 0.7)' },
            { label: 'Predicted Demand', data: sampleP.map(p => p.predictedDemand), backgroundColor: 'rgba(244, 63, 94, 0.7)' }
        ]
    });

    // Chart 5: Brand Comparison
    const brandEntries = Object.entries(m.brandSales || {});
    renderChart('execBrandComparisonChart', 'pie', {
        labels: brandEntries.map(e => e[0]),
        datasets: [{
            data: brandEntries.map(e => e[1]),
            backgroundColor: ['#06b6d4', '#3b82f6', '#10b981', '#f59e0b', '#a855f7', '#f43f5e', '#64748b']
        }]
    }, { plugins: { legend: { position: 'bottom', labels: { color: '#94a3b8' } } } });

    // Chart 6: Product Classification Breakdown
    const classEntries = Object.entries(m.classificationCounts || {});
    renderChart('execClassificationChart', 'doughnut', {
        labels: classEntries.map(e => e[0]),
        datasets: [{
            data: classEntries.map(e => e[1]),
            backgroundColor: ['#10b981', '#3b82f6', '#ef4444', '#f59e0b']
        }]
    }, { plugins: { legend: { position: 'bottom', labels: { color: '#94a3b8' } } } });
}

function renderChart(canvasId, type, data, extraOptions = {}) {
    const ctx = document.getElementById(canvasId);
    if (!ctx) return;
    if (chartInstances[canvasId]) chartInstances[canvasId].destroy();

    const cols = getChartThemeColors();
    const baseOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { labels: { color: cols.textColor } } },
        scales: (type === 'pie' || type === 'doughnut') ? {} : {
            y: { grid: { color: cols.gridColor }, ticks: { color: cols.textColor } },
            x: { grid: { color: cols.xGridColor }, ticks: { color: cols.textColor } }
        }
    };

    chartInstances[canvasId] = new Chart(ctx, {
        type: type,
        data: data,
        options: { ...baseOptions, ...extraOptions }
    });
}

// ==========================================
// 7. DATA ENTRY & CSV UPLOADER
// ==========================================

function handleCsvFileSelected(file) {
    if (!file) return;
    selectedCsvFile = file;
    document.getElementById('dropZone').innerHTML = `
        <i class="fas fa-file-csv" style="font-size: 38px; color: var(--emerald-500); margin-bottom: 12px;"></i>
        <div style="font-size: 15px; font-weight: 700; color: var(--text-primary);">${escapeHtml(file.name)}</div>
        <div style="font-size: 12px; color: var(--text-muted);">${(file.size / 1024).toFixed(1)} KB — Ready to upload</div>
    `;
}

async function uploadSelectedCsv() {
    if (!selectedCsvFile) {
        alert('Please choose or drop a CSV file first.');
        return;
    }

    const btn = document.getElementById('uploadCsvBtn');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Processing & Analyzing...';

    const alertBox = document.getElementById('uploadResultAlert');
    const alertText = document.getElementById('uploadResultText');
    const alertIcon = document.getElementById('uploadResultIcon');
    const viewErrorsBtn = document.getElementById('viewErrorsBtn');
    const errorsBadge = document.getElementById('errorsCountBadge');
    const errorSection = document.getElementById('errorDetailsSection');
    const errorTableBody = document.getElementById('errorDetailsTableBody');
    const statsGrid = document.getElementById('uploadStatsGrid');
    const downloadErrorsBtn = document.getElementById('downloadErrorsBtn');

    errorSection?.classList.add('hidden');
    viewErrorsBtn?.classList.add('hidden');
    downloadErrorsBtn?.classList.add('hidden');
    if (errorTableBody) errorTableBody.innerHTML = '';

    const text = await selectedCsvFile.text();
    try {
        const res = await fetch('/api/csv/upload', {
            method: 'POST',
            headers: { 'Content-Type': 'text/csv' },
            body: text
        });
        const data = await res.json();

        if (data.success) {
            const imported = data.productsImported || 0;
            const skipped = data.rowsSkipped || 0;
            const errors = data.errorsEncountered || 0;
            const totalRows = data.totalRowsDetected !== undefined ? data.totalRowsDetected : (imported + skipped + errors);
            const errorList = data.errorDetails || [];

            alertBox?.classList.remove('hidden');

            // Populate Statistics Cards
            if (statsGrid) {
                statsGrid.classList.remove('hidden');
                document.getElementById('statTotalRows').textContent = totalRows;
                document.getElementById('statImportedRows').textContent = imported;
                document.getElementById('statSkippedRows').textContent = skipped;
                document.getElementById('statErrorRows').textContent = errors;
            }

            if (imported > 0 && errors === 0) {
                alertBox.className = 'alert-box alert-success';
                alertIcon.className = 'fas fa-check-circle';
                alertText.innerHTML = `✓ CSV uploaded and validated successfully! Imported: <strong>${imported}</strong> products across <strong>${totalRows}</strong> total rows detected. 0 errors.<br><span style="font-size: 12px; font-weight: normal; color: var(--emerald-400);">Inventory stock and AI sales intelligence generated.</span>`;
            } else if (imported > 0 && errors > 0) {
                alertBox.className = 'alert-box alert-success';
                alertIcon.className = 'fas fa-check-circle';
                alertText.innerHTML = `✓ CSV processed: <strong>${imported}</strong> new smartphone models successfully imported. <strong>${errors}</strong> validation error(s) flagged for correction.`;
                viewErrorsBtn?.classList.remove('hidden');
                downloadErrorsBtn?.classList.remove('hidden');
                if (errorsBadge) errorsBadge.textContent = errors;
            } else {
                alertBox.className = 'alert-box alert-danger';
                alertIcon.className = 'fas fa-triangle-exclamation';
                alertText.innerHTML = `⚠️ CSV validation completed with 0 products imported, <strong>${errors}</strong> validation error(s) detected.`;
                if (errors > 0) {
                    viewErrorsBtn?.classList.remove('hidden');
                    downloadErrorsBtn?.classList.remove('hidden');
                    if (errorsBadge) errorsBadge.textContent = errors;
                }
            }

            // Populate Error Details Table if any
            if (errorList.length > 0 && errorTableBody) {
                errorList.forEach(err => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td><strong>Row ${err.rowNumber}</strong></td>
                        <td><span style="color: var(--cyan-400); font-weight: 600;">${escapeHtml(err.column)}</span></td>
                        <td><code style="word-break: break-all; font-size: 11px;">${escapeHtml(err.value || '(empty)')}</code></td>
                        <td><span style="color: var(--rose-400); font-weight: 500;">${escapeHtml(err.problem)}</span></td>
                        <td><span style="color: var(--emerald-400); font-size: 12px;">${escapeHtml(err.suggestedFix)}</span></td>
                    `;
                    errorTableBody.appendChild(row);
                });
            }

            // Automatically refresh Dashboard & Brands
            if (imported > 0) {
                await refreshBrandOptions();
                onBrandSelected(currentBrand);
                loadExecutiveDashboard();
            }

        } else {
            alertBox.className = 'alert-box alert-danger';
            alertIcon.className = 'fas fa-triangle-exclamation';
            alertText.textContent = 'Upload error: ' + (data.error || 'Failed to process CSV');
            alertBox?.classList.remove('hidden');
        }
    } catch (err) {
        alertBox.className = 'alert-box alert-danger';
        alertIcon.className = 'fas fa-triangle-exclamation';
        alertText.textContent = 'Connection error: ' + err.message;
        alertBox?.classList.remove('hidden');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fas fa-upload"></i> Upload &amp; Analyze';
    }
}

function toggleErrorsSection() {
    const errorSection = document.getElementById('errorDetailsSection');
    if (errorSection) {
        errorSection.classList.toggle('hidden');
    }
}

function downloadValidationErrorsCsv() {
    window.location.href = '/api/csv/errors/download';
}

async function refreshBrandOptions() {
    try {
        const res = await fetch('/api/brands');
        const data = await res.json();
        if (data.success && data.brands) {
            const select = document.getElementById('brandDropdownSelect');
            const chipsGrid = document.querySelector('.brand-chips-grid');

            if (select) {
                select.innerHTML = '';
                data.brands.forEach(b => {
                    const opt = document.createElement('option');
                    opt.value = b;
                    opt.textContent = b;
                    if (b.toLowerCase() === currentBrand.toLowerCase()) opt.selected = true;
                    select.appendChild(opt);
                });
            }

            if (chipsGrid) {
                chipsGrid.innerHTML = '';
                data.brands.forEach(b => {
                    const chip = document.createElement('div');
                    chip.className = `brand-chip ${b.toLowerCase() === currentBrand.toLowerCase() ? 'active' : ''}`;
                    chip.onclick = () => onBrandSelected(b, chip);
                    chip.innerHTML = `
                        <i class="fas fa-mobile-screen"></i>
                        <span class="brand-chip-name">${escapeHtml(b)}</span>
                    `;
                    chipsGrid.appendChild(chip);
                });
            }
        }
    } catch (e) {
        console.error('Error refreshing brands:', e);
    }
}

async function handleDirectProductSubmit(e) {
    e.preventDefault();
    const brand = document.getElementById('manualBrand').value;
    const modelName = document.getElementById('manualModel').value.trim();
    const price = parseFloat(document.getElementById('manualPrice').value);
    const currentStock = parseInt(document.getElementById('manualStock').value);
    const ram = document.getElementById('manualRAM').value.trim();
    const storage = document.getElementById('manualStorage').value.trim();
    const processor = document.getElementById('manualProcessor').value.trim();

    try {
        const res = await fetch('/api/products/add', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                brand, modelName, price, currentStock, ram, storage, processor,
                category: 'Smartphone', battery: '5000 mAh', display: '6.7" OLED 120Hz', camera: '50MP Main'
            })
        });
        const data = await res.json();
        if (data.success) {
            alert(`Product '${brand} ${modelName}' successfully added and evaluated!`);
            document.getElementById('addProductDirectForm').reset();
            onBrandSelected(brand);
            openProductDashboard(data.productId);
        } else {
            alert('Error adding product: ' + (data.error || 'Server error'));
        }
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

function openAddProductModal() {
    switchMainView('dataEntryView');
}

function downloadSampleCsv() {
    window.location.href = '/api/csv/sample';
}

function exportCatalogCSV() {
    window.location.href = '/api/csv/export';
}

function downloadAuditReport() {
    window.location.href = '/api/report/download';
}

function simulateOrderPlacement() {
    if (!currentProduct) return;
    const rec = currentProduct.recommendation;
    alert(`Purchase Order Confirmed!\n\nSupplier Order Placed for: ${currentProduct.product.brand} ${currentProduct.product.modelName}\nQuantity: ${rec.recommendedReorderQty} units\nLead Time Window: ${rec.supplierLeadTimeDays} days.\nInventory levels will be updated upon shipment arrival.`);
}

// Utility
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
