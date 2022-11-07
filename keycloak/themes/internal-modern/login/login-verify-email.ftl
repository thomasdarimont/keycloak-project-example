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
            // periodically reloads the current page to check if the email was already verified on a different device

            // TODO instead of simple reloading the page we could use the current url in an async fetch request and handle the redirect manually.
            function scheduleReload() {
                setTimeout(() => {
                    location.reload();
                    scheduleReload();
                }, 5000);
            }

            scheduleReload();
        </script>
    </#if>
</@layout.registrationLayout>
