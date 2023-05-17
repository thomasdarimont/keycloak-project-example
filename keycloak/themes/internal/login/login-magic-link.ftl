<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "form">
        <div>
            <h1 id="kc-page-title">
                ${msg("acmeMagicLinkTitle")}
            </h1>
        </div>
        <div id="kc-info-message">
            <p class="instruction">${message.summary}</p>
        </div>
    </#if>
</@layout.registrationLayout>