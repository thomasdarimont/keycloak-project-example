// custom-login.js

(function onCustomLogin() {
    console.log("custom login");

    function focusIfFound(selector) {
        let elem = document.querySelector(selector);
        if (elem) {
            elem.focus();
        }
    }

    document.addEventListener("DOMContentLoaded", function() {
        focusIfFound("#kc-form-login #password");
        focusIfFound("#kc-form-login #otp");

        document.querySelector("#kc-form-login #otp")?.setAttribute("auto-complete","one-time-code");
    })

})();

