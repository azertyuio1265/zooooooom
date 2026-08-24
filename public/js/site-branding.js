/**
 * ZoomDz Platform Branding & Dynamic Image Sync Engine
 * Ensures custom logos, icons, and site images update instantly across web and app
 */
(function() {
    'use strict';

    function applyBranding(images) {
        if (!images || typeof images !== 'object') return;
        var logoUrl = images.app_logo || images.site_logo;

        if (logoUrl) {
            // Update all brand & app logo image tags
            document.querySelectorAll('.site-app-logo, .brand-logo-img, .navbar-app-logo, .app-brand-logo, #navbarAppLogoImg, #mobileDrawerLogoImg, #studentNavAppLogo, #teacherNavAppLogo').forEach(function(img) {
                if (img.tagName === 'IMG') {
                    img.setAttribute('data-original-url', logoUrl);
                    img.setAttribute('referrerpolicy', 'no-referrer');
                    img.src = logoUrl;
                    img.style.display = 'block';
                    if (img.nextElementSibling && img.nextElementSibling.classList.contains('fallback-icon')) {
                        img.nextElementSibling.style.display = 'none';
                    }
                }
            });

            // Update preloader logo if present
            var preloaderLogo = document.getElementById('preloaderAppLogoImg');
            if (preloaderLogo) {
                preloaderLogo.setAttribute('data-original-url', logoUrl);
                preloaderLogo.setAttribute('referrerpolicy', 'no-referrer');
                preloaderLogo.src = logoUrl;
                preloaderLogo.style.display = 'block';
                var defIcon = document.getElementById('preloaderDefaultIcon');
                if (defIcon) defIcon.style.display = 'none';
            }

            // Update dynamic favicons and icons
            try {
                var icons = document.querySelectorAll('link[rel="shortcut icon"], link[rel="icon"], link[rel="apple-touch-icon"]');
                icons.forEach(function(el) {
                    el.href = logoUrl;
                });
            } catch(e) {}
        }
    }

    // Fast initial apply from local cache
    try {
        var cached = localStorage.getItem('zoomdz_site_images');
        if (cached) {
            applyBranding(JSON.parse(cached));
        }
    } catch(e) {}

    // Live sync from server API
    async function syncBranding() {
        try {
            var res = await fetch('/api/settings/site_images?_t=' + Date.now());
            if (!res.ok) return;
            var data = await res.json();
            if (data) {
                applyBranding(data);
                try {
                    localStorage.setItem('zoomdz_site_images', JSON.stringify(data));
                } catch(e) {}
            }
        } catch(e) {}
    }

    window.ZoomDzBranding = {
        apply: applyBranding,
        sync: syncBranding
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', syncBranding);
    } else {
        syncBranding();
    }
})();
