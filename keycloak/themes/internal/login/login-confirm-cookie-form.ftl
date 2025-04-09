<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('code') showAnotherWayIfPresent=false; section>
    <#if section = "header">
        Confirm Cookie: ${realm.displayName}
    <#elseif section = "form">

        <h1>Confirm Cookie</h1>

        <form id="kc-confirm-cookie-login-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">

            <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
<#--                <div id="kc-form-options" class="${properties.kcFormOptionsClass!}">-->
<#--                    <div class="${properties.kcFormOptionsWrapperClass!}">-->
<#--                        <span><a href="${url.loginUrl}">${kcSanitize(msg("backToLogin"))?no_esc}</a></span>-->
<#--                    </div>-->
<#--                </div>-->

                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <div class="${properties.kcFormButtonsWrapperClass!}">
                        <input name="proceed"
                                class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               type="submit" value="Continue"/>

<#--                        <input name="switchUser"-->
<#--                           class="${properties.kcButtonClass!} ${properties.kcButtonSecondaryClass!} ${properties.kcButtonBlockClass!}  ${properties.kcButtonLargeClass!}"-->
<#--                           type="submit" value="Switch User"-->
<#--                           formnovalidate="formnovalidate"/>-->

                        <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg("restartLoginTooltip")}" class="${properties.kcButtonClass!} ${properties.kcButtonSecondaryClass!} ${properties.kcButtonBlockClass!}  ${properties.kcButtonLargeClass!}">
                            Switch User
                            <div class="kc-login-tooltip">
                                <span class="kc-tooltip-text">Switch User</span>
                            </div>
                        </a>
                    </div>
                </div>
            </div>
        </form>
    <#elseif section = "info" >
        Confirm Cookie Instruction
    </#if>
</@layout.registrationLayout>