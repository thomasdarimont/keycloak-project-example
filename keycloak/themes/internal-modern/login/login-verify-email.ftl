<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header">
        ${msg("emailVerifyTitle")}
    <#elseif section = "form">
        <p class="instruction">${msg("emailVerifyInstruction1",user.email)}</p>
    <#elseif section = "info">
        <p class="instruction">
            ${msg("emailVerifyInstruction2")}
            <br/>
            <a href="${url.loginAction}">${msg("doClickHere")}</a> ${msg("emailVerifyInstruction3")}
        </p>

        <script defer>
            // periodically checks if the email was already verified on a different device
            // we use the current url in an async fetch request and handle the redirect manually
            function scheduleReload() {
                setTimeout(() => {

                    fetch(location.href, {credentials: 'include', redirect: 'follow'})
                        .then(function (response) {

                            if (response.redirected) {
                                window.location.href = response.url;
                                return;
                            }

                            scheduleReload();
                        });
                }, 5000);
            }

            scheduleReload();
        </script>
    </#if>
</@layout.registrationLayout>
