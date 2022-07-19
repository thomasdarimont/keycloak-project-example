let spa = {};

function qs(selector) {
    return document.querySelector(selector);
}

function qsa(selector) {
    return [...document.querySelectorAll(selector)];
}

function callApi(url, requestOptions, onError) {
    let csrfToken = qs("meta[name=_csrf]").content;
    let csrfTokenHeader = qs("meta[name=_csrf_header]").content;
    let requestData = {
        timeout: 2000,
        method: "GET",
        credentials: "include",
        headers: {
            "Accept": "application/json",
            'Content-Type': 'application/json',
            [`${csrfTokenHeader}`]: csrfToken
        }
        , ...requestOptions
    }
    return fetch(url, requestData).catch(onError);
}


(async function onInit() {
    try {
        let userInfoResponse = await callApi("/bff/api/users/me", {});
        if (userInfoResponse.ok) {
            let userInfo = await userInfoResponse.json();
            console.log(userInfo);
            spa.userInfo = userInfo;
        }
    } catch {
        console.log("failed to fetch userinfo");
    }

    if (spa.userInfo) {
        qs("#userInfo").innerText = JSON.stringify(spa.userInfo.claims, null, "  ");
        qs("#login").remove()
    } else {
        qs("#logout").remove()
    }
}());