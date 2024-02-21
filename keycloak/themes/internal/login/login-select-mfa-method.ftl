<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('code'); section>
    <#if section = "header">
        ${msg("selectMfaMethodTitle",realm.displayName)}
    <#elseif section = "form">
        <form id="kc-select-mfa-method-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="mfaMethods" class="${properties.kcLabelClass!}">${msg("mfaMethods")}</label>
                </div>

                <ul id="kc-form-options" class="pf-c-list">
                <#list mfaMethods as mfaMethod>
                    <li>
                        <div>
                        <button class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               type="submit" name="mfaMethod" value="${mfaMethod}">${msg(mfaMethod)}</button>
                        </div>
                    </li>
                </#list>
                </ul>
            </div>
        </form>
    <#elseif section = "info" >
        ${msg("selectMfaMethodInstruction")}
    </#if>
</@layout.registrationLayout>