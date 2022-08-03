<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg('acmeEmailUpdateVerifyTitle')}
    <#elseif section = "header">
        ${msg('acmeEmailUpdateVerifyTitle')}
    <#elseif section = "form">

        <p>${msg('acmeEmailVerifyCta')}</p>
        <form id="kc-email-update-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="code">${msg('emailAuthLabel')}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input id="code" type="text" name="code" value="" required autocomplete="one-time-code"
                           aria-invalid="<#if messagesPerField.existsError('code')>true</#if>"/>

                    <#if messagesPerField.existsError('code')>
                        <span id="input-error-code" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('code'))?no_esc}
                        </span>
                    </#if>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <#if isAppInitiatedAction??>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                               name="verify" type="submit" value="${msg("doSubmit")}"/>
                        <button
                        class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                        type="submit" name="cancel-aia" value="true" formnovalidate>${msg("doCancel")}</button>
                    <#else>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="verify" type="submit" value="${msg("doSubmit")}"/>
                    </#if>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>