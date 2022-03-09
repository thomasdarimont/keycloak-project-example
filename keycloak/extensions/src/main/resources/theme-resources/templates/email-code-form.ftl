<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg('emailCodeFormTitle')}
    <#elseif section = "header">
        ${msg('emailCodeFormTitle')}
    <#elseif section = "form">
        <p>${msg('emailCodeFormCta')}</p>
        <form class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <div>
                    <div class="${properties.kcInputWrapperClass!}">
                        <label for="emailCode">${msg('accessCode')}:</label>
                        <input id="emailCode" name="emailCode" type="text" inputmode="numeric" pattern="[\w\d-]*" autofocus
                               required autocomplete="one-time-code"/>
                    </div>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("doSubmit")}"/>

                    <input name="resend"
                           class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("resendCode")}"/>

                    <input name="cancel"
                           class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                           type="submit" value="${msg("doCancel")}"/>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>