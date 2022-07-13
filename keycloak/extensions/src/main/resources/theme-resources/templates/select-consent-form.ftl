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
                                        <#if scope.granted || !scope.optional>checked</#if>
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

                            <#-- Field details by scope -->

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
                                                   value="${(scopeField.value!'')}"
                                                   disabled/>
                                        </div>
                                    </div>
                                </#list>
                            </div>
                            <#-- -->

                            <div></div>
                        </div>
                    </#list>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                           name="accept" id="kc-login" type="submit" value="${msg("doYes")}"/>
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                           name="cancel" id="kc-cancel" type="submit" value="${msg("doNo")}"/>

                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>