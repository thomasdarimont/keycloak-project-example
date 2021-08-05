<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg("acmeRegisterTrustedDeviceTitle")}
    <#elseif section = "header">
        ${msg("acmeRegisterTrustedDeviceTitle")}
    <#elseif section = "form">
        <p>${msg("acmeRegisterTrustedDeviceCta")}</p>
        <form action="${url.loginAction}" class="${properties.kcFormClass!}" id="kc-u2f-login-form" method="post">
            <label for="device">${msg("device")}</label>
            <input id="device" type="text" name="device" value="${(device!'')}"/>

            <div class="checkbox">
                <label for="removeOtherTrustedDevices" class="${properties.kcLabelClass!}">
                    <input type="checkbox" id="removeOtherTrustedDevices" name="remove-other-trusted-devices" class="${properties.kcCheckboxInputClass!}"
                           value=""/>
                    ${msg("removeAllTrustedDevices")}
                </label>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <#if isAppInitiatedAction??>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                               type="submit" name="trust-device" value="${msg("yes")}"/>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonSecondaryClass!} ${properties.kcButtonLargeClass!}"
                               type="submit" name="dont-trust-device" value="${msg("no")}"/>
                        <button class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                                type="submit" name="cancel-aia" value="true" formnovalidate/>${msg("doCancel")}</button>
                    <#else>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                               type="submit" name="trust-device" value="${msg("yes")}"/>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonSecondaryClass!} ${properties.kcButtonLargeClass!}"
                               type="submit" name="dont-trust-device" value="${msg("no")}"/>
                    </#if>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>