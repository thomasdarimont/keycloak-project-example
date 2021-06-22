// custom-login.js

(function initTheme() {
    console.log("internal theme");

    // hack to add mobile icon for sms authenticator, needs to be called after dom ready
    function updateMobileIconOnSmsAuthenticatorInAuthenticationSelector() {
        let elements = [...document.querySelectorAll('div.pf-c-title')].filter(elem => elem.textContent.includes('SMS'));
        if (elements && elements.length > 0) {
            console.log("patch mobile icon");
            elements[0].parentElement.parentElement.querySelector("i").classList.add("fa-mobile");
        }
    }

    function onDomContentLoaded() {
        updateMobileIconOnSmsAuthenticatorInAuthenticationSelector();
    }

    document.addEventListener('DOMContentLoaded', evt => onDomContentLoaded());
})();

