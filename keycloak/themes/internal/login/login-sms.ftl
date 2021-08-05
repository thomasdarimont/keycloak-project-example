<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('code'); section>
    <#if section = "header">
        ${msg("smsAuthTitle",realm.displayName)}
    <#elseif section = "form">
        <form id="kc-sms-code-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div class="${properties.kcLabelWrapperClass!}">
                    <label for="code" class="${properties.kcLabelClass!}">${msg("smsAuthLabel")}</label>
                </div>
                <div class="${properties.kcInputWrapperClass!}">
                    <input type="text" id="code" name="code" class="${properties.kcInputClass!}" autofocus
                           inputmode="numeric" pattern="\d{6,8}" autocomplete="one-time-code"
                           aria-invalid="<#if messagesPerField.existsError('code')>true</#if>"/>

                    <#if messagesPerField.existsError('code')>
                        <span id="input-error-sms-code" class="${properties.kcInputErrorMessageClass!}"
                              aria-live="polite">
                        ${kcSanitize(messagesPerField.get('code'))?no_esc}
                    </span>
                    </#if>
                </div>
            </div>

            <div class="checkbox">
                <label for="registerTrustedDevice" class="${properties.kcLabelClass!}">
                    <input type="checkbox" id="registerTrustedDevice" name="register-trusted-device" class="${properties.kcCheckboxInputClass!}"
                           value=""/>
                    ${msg("trustThisDevice")}
                </label>
            </div>

            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <span><a href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               type="submit" value="${msg("doSubmit")}"/>

                        <input name="resend"
                               class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!} <#if showResend??><#else>hidden</#if>"
                               type="submit" value="${msg("smsResendCode")}"/>
                    </div>
                </div>
            </div>
        </form>
    <#elseif section = "info" >
        ${msg("smsAuthInstruction")}
    </#if>
</@layout.registrationLayout>