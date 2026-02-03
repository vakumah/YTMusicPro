/**
 * YTMusic Pro Core Injection Script
 * Version: 1.0.0
 * Features: Ad-Block, Background Play Control, Tracks Extraction
 */

(function () {
    console.log("YTMusic Pro Script Initialized");

    const CONFIG = {
        checkInterval: 1000,
        adCheckInterval: 500
    };

    let lastTrack = { title: "", artist: "", isPlaying: false, position: 0 };

    // 1. Ad Blocker Logic
    function blockAds() {
        // Remove known ad elements
        const adSelectors = [
            'ytm-promoted-sparkles-web-renderer',
            'ytm-companion-ad-renderer',
            '.video-ads',
            '.ytp-ad-module',
            'ytm-promoted-video-renderer'
        ];

        adSelectors.forEach(selector => {
            document.querySelectorAll(selector).forEach(el => el.remove());
        });

        // Skip video ads if they appear in the player
        const video = document.querySelector('video');
        if (video) {
            const adShowing = document.querySelector('.ad-showing, .ad-interrupting');
            if (adShowing) {
                video.currentTime = video.duration || 999;
                console.log("YTMusic Pro: Ad Skipped");
            }
        }
    }

    function timeStringToSeconds(timeStr) {
        if (!timeStr) return 0;
        const parts = timeStr.split(':').map(Number);
        if (parts.length === 2) return parts[0] * 60 + parts[1];
        if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
        return 0;
    }

    // 2. Track Info Extraction (Using more robust selectors)
    function updateMetadata() {
        const selectors = {
            title: 'ytmusic-player-bar .title, .ytmusic-player-bar .title',
            artist: 'ytmusic-player-bar .byline, .ytmusic-player-bar .byline',
            art: 'ytmusic-player-bar img#img, .ytmusic-player-bar img#img, yt-img-shadow#thumbnail img',
            curTime: '.time-info .current-time, #progress-bar .current-time',
            totalTime: '.time-info .duration, #progress-bar .duration'
        };

        const titleEl = document.querySelector(selectors.title);
        const artistEl = document.querySelector(selectors.artist);
        const artEl = document.querySelector(selectors.art);
        const video = document.querySelector('video');
        const curTimeEl = document.querySelector(selectors.curTime);
        const totalTimeEl = document.querySelector(selectors.totalTime);

        if (titleEl && artistEl) {
            const title = titleEl.textContent.trim();
            const artist = artistEl.textContent.trim();
            let artUrl = artEl ? artEl.src : "";
            if (artUrl.startsWith('//')) artUrl = 'https:' + artUrl;

            const isPlaying = video ? !video.paused : true;
            const position = video ? Math.floor(video.currentTime) : timeStringToSeconds(curTimeEl?.textContent);
            const duration = video ? Math.floor(video.duration) : timeStringToSeconds(totalTimeEl?.textContent);

            if (title !== lastTrack.title || artist !== lastTrack.artist || isPlaying !== lastTrack.isPlaying || Math.abs(position - lastTrack.position) >= 1) {
                lastTrack = { title, artist, isPlaying, position };
                if (window.YTMusicPro) {
                    window.YTMusicPro.updateNotification(title, artist, artUrl, isPlaying, position * 1000, duration * 1000);
                }
            }
        }
    }

    // 3. UI Enhancements (Premium Look)
    function enhanceUI() {
        // Force dark mode styles if not already
        const style = document.createElement('style');
        style.innerHTML = `
            /* Hide annoying prompts */
            ytmusic-mealbar-promo-renderer, 
            ytmusic-upsell-dialog-renderer { 
                display: none !important; 
            }
            
            /* Glassmorphism for player bar */
            ytmusic-player-bar {
                background: rgba(0,0,0,0.7) !important;
                backdrop-filter: blur(15px) !important;
                border-top: 1px solid rgba(255,255,255,0.1) !important;
            }
        `;
        document.head.appendChild(style);
    }


    // Loops
    setInterval(blockAds, CONFIG.adCheckInterval);
    setInterval(updateMetadata, CONFIG.checkInterval);

    // Initial UI Tweaks
    setTimeout(enhanceUI, 3000);

})();
