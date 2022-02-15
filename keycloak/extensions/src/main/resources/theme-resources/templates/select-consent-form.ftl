<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg('acmeConsentSelectionTitle')}
    <#elseif section = "header">
        ${msg('acmeConsentSelectionTitle')}
    <#elseif section = "form">

        <p>${msg('acmeConsentSelection')}</p>
        <form id="acme-dynamic-scope-selection-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
              method="post">

            <div class="${properties.kcFormGroupClass!}">
                <div id="scopes">
                    <#list scopes as scope>
                        <div>
                            <div class="${properties.kcInputWrapperClass!}">
                                <input id="${scope.name}-item"
                                       type="checkbox"
                                       name="scopeSelection"
                                       value="${scope.name}"
                                       <#if !scope.optional>disabled</#if>
                                       <#if scope.granted>checked</#if>
                                />
                                <#if !scope.optional>
                                    <input type="hidden" name="scopeSelection" value="${scope.name}"/>
                                </#if>

                                <label for="${scope.name}-item">${msg(scope.name)}</label>
                                <span><#if scope.optional>(optional)</#if></span>
                                <p>
                                    ${msg(scope.description)}
                                </p>
                            </div>

                            <div class="${properties.kcFormGroupClass!}">
                                <#list scope.fields as scopeField>
                                    <div class="${properties.kcInputWrapperClass!}">

                                        <div class="${properties.kcLabelWrapperClass!}">
                                            <label for="${scopeField.name}">${msg(scopeField.name)}</label>
                                        </div>

                                        <div class="${properties.kcInputWrapperClass!}">
                                            <input id="${scopeField.name}"
                                                   type="${scopeField.type}"
                                                   name="${scopeField.name}"
                                                   value="${scopeField.value}"
                                                   disabled/>
                                        </div>
                                    </div>
                                </#list>
                            </div>

                            <div></div>
                        </div>
                    </#list>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <#if isAppInitiatedAction??>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                               type="submit" name="update" value="${msg("doSubmit")}"/>
                        <button
                        class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                        type="submit" name="cancel-aia" value="true" formnovalidate/>${msg("doCancel")}</button>
                    <#else>
                        <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                               name="update" type="submit" value="${msg("doSubmit")}"/>
                    </#if>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>