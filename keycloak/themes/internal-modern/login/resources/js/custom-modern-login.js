// custom-login.js

(function initTheme() {
    console.log("internal modern theme");

    // hack to add mobile icon for sms authenticator, needs to be called after dom ready
    function updateMobileIconOnSmsAuthenticatorInAuthenticationSelector() {
        let elements = [...document.querySelectorAll('div.pf-c-title')].filter(elem => elem.textContent.includes('SMS'));
        if (elements && elements.length > 0) {
            console.log("patch mobile icon");
            elements[0].parentElement.parentElement.querySelector("i").classList.add("fa-mobile");
        }
    }

    // hack to add key icon for backup-code authenticator, needs to be called after dom ready
    function updateBackupCodeIconOnBackupCodeAuthenticatorInAuthenticationSelector() {
        let elements = [...document.querySelectorAll('div.pf-c-title')].filter(elem => elem.textContent.includes('Backup'));
        if (elements && elements.length > 0) {
            console.log("patch backup-code icon");
            elements[0].parentElement.parentElement.querySelector("i").classList.add("pficon-key");
        }
    }

    function enableInactivityMonitoring() {

        went_inactive = new Date().getTime();

        auto_reload_diff = 29*60*1000

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
            if (document[hidden]) {
                went_inactive = new Date().getTime();
            } else {
                if(new Date().getTime() >  auto_reload_diff + went_inactive)
                {
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
    };

    function onDomContentLoaded() {
        updateMobileIconOnSmsAuthenticatorInAuthenticationSelector();
        updateBackupCodeIconOnBackupCodeAuthenticatorInAuthenticationSelector();

        enableInactivityMonitoring();
    }

    document.addEventListener('DOMContentLoaded', evt => onDomContentLoaded());
})();
