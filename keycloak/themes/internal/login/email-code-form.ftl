<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg('emailCodeFormTitle')}
    <#elseif section = "header">
        ${msg('emailCodeFormTitle')}
    <#elseif section = "form">
        <script>
            function tryCompleteForm() {
                let emailCodeInput = document.querySelector("#emailCode");
                if (emailCodeInput.validity.valid) {
                    document.querySelector("#kc-email-code-login-form").submit();
                }
            }
        </script>
        <p>${msg('emailCodeFormCta')}</p>
        <form id="kc-email-code-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <label for="emailCode">${msg('accessCode')}:</label>
                        <input id="emailCode" name="emailCode" type="text" inputmode="numeric" pattern="${codePattern}" autofocus
                               class="${properties.kcInputClass!}" <#if tryAutoSubmit> oninput="tryCompleteForm()" </#if>
                               required autocomplete="one-time-code"/>
                    </div>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">

                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">
                    <div class="${properties.kcFormOptionsWrapperClass!}">
                        <span><a href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>
                    </div>
                </div>

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("doSubmit")}"/>

                    <input name="resend"
                           class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("resendCode")}"
                           formnovalidate="formnovalidate"/>

                    <input name="cancel"
                           class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("doCancel")}"
                           formnovalidate="formnovalidate"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>