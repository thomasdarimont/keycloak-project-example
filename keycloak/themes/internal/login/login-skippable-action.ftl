<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        Skippable Action
    <#elseif section = "header">
        Skippable Action
    <#elseif section = "form">
        <p>Example Skippable Action</p>
        <form id="kc-skippable-action-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">

            <div class="${properties.kcFormGroupClass!}">

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("doSubmit")}"/>

                    <#if canSkip>
                    <input name="skip"
                           class="${properties.kcButtonClass!} ${properties.kcButtonSecondaryClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="Skip"
                           formnovalidate="formnovalidate"/>
                    </#if>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>