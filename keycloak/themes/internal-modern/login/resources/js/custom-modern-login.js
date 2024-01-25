// custom-login.js

(function initTheme() {
    console.log("internal modern theme");

    // hack to add mobile icon for sms authenticator, needs to be called after dom ready
    function updateAuthenticatorIconsInAuthenticationSelector() {
        {
            let elements = [...document.querySelectorAll('div.pf-c-title')].filter(elem => elem.textContent.includes('SMS'));
            if (elements && elements.length > 0) {
                console.log("patch mobile icon");
                elements[0].parentElement.parentElement.querySelector("i").classList.add("fa-mobile");
            }
        }

        {
            let emailCodeAuthElements = [...document.querySelectorAll('div.pf-c-title')].filter(elem => elem.textContent.toLowerCase().replace("-","").includes('email code'));
            if (emailCodeAuthElements && emailCodeAuthElements.length > 0) {
                console.log("patch email-code icon");
                emailCodeAuthElements[0].parentElement.parentElement.querySelector("i").classList.add("fa-envelope");
            }
        }
    }

    function enableInactivityMonitoring() {

        let idleSinceTimestamp = Date.now();

        const maxIdleMinutesBeforeAutoReload = 29;
        const autoReloadInactivityThresholdMillis = maxIdleMinutesBeforeAutoReload * 60 * 1000

        var hidden, visibilityChange;
        if (typeof document.hidden !== "undefined") { // Opera 12.10 and Firefox 18 and later support
            hidden = "hidden";
            visibilityChange = "visibilitychange";
        } else if (typeof document.msHidden !== "undefined") {
            hidden = "msHidden";
            visibilityChange = "msvisibilitychange";
        } else if (typeof document.webkitHidden !== "undefined") {
            hidden = "webkitHidden";
            visibilityChange = "webkitvisibilitychange";
        }

        function handleVisibilityChange() {
            const now = Date.now();
            if (document[hidden]) {
                idleSinceTimestamp = now;
            } else {
                if (now > idleSinceTimestamp + autoReloadInactivityThresholdMillis) {
                    location.reload();
                }
            }
        }

        if (typeof document.addEventListener === "undefined" || hidden === undefined) {
            console.log("This demo requires a browser, such as Google Chrome or Firefox, that supports the Page Visibility API.");
        } else {
            // Handle page visibility change
            document.addEventListener(visibilityChange, handleVisibilityChange, false);
        }
    }

    function autoSubmitLoginHintForUsernameFormForCompanyApps() {

        if (window.location.href.includes("/company-users/") && new URLSearchParams(window.location.search).get("login_hint")) {
            // only for company-users realm if login hint is present
            if (document.querySelector("input[name=username]") && !document.querySelector("input[name=password]")) {
                log.info("autoSubmitLoginHintForUsernameFormForCompanyApps");
                // we are in username name form
                document.querySelector("#kc-form-login").submit();
            }
        }
    }

    function onDomContentLoaded() {
        updateAuthenticatorIconsInAuthenticationSelector();

        autoSubmitLoginHintForUsernameFormForCompanyApps();

        enableInactivityMonitoring();
    }

    document.addEventListener('DOMContentLoaded', evt => onDomContentLoaded());
})();
