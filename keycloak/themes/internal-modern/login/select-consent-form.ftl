<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "title">
        ${msg('acmeConsentSelectionTitle')}
    <#elseif section = "header">
        ${msg('acmeConsentSelectionTitle')}
    <#elseif section = "form">

<#--        <p>-->
<#--        <div>DEBUG-->
<#--            <div>Granted Scope: ${grantedScope}</div>-->
<#--            <div>Requested Scope: ${requestedScope}</div>-->
<#--        </div>-->
<#--        </p>-->

        <p>${msg('acmeConsentSelection')}</p>
        <form id="acme-dynamic-scope-selection-form" class="${properties.kcFormClass!}" action="${url.loginAction}"
              method="post">

            <div class="${properties.kcFormGroupClass!}">
                <div id="scopes">
                    <#list scopes as scope>
                        <div>
                            <div class="${properties.kcInputWrapperClass!} ${grantedScopeNames?seq_contains(scope.name)?string("hidden", "")}">
                                <#if !scope.optional>
                                    <input type="hidden" name="scopeSelection" value="${scope.name}"/>
                                </#if>

                                <label for="${scope.name}-item">${msg(scope.name)}</label>
                                <#--   <span><#if scope.optional>(optional)</#if></span>-->
                                <input id="${scope.name}-item"
                                       type="checkbox"
                                       name="scopeSelection"
                                       value="${scope.name}"
                                       <#if !scope.optional>disabled</#if>
                                        <#if scope.granted || scope.optional>checked</#if>
                                        <#--  <#if !scope.optional>class="hidden"</#if>-->
                                       class="hidden"
                                />
                                <p>
                                    ${msg(scope.description)}
                                </p>
                            </div>

                            <#-- Field details by scope -->
                            <#-- -->
                            <#if !grantedScopeNames?seq_contains(scope.name) >
                            <div class="${properties.kcFormGroupClass!}">
                                <#list scope.fields as scopeField>
                                    <div class="${properties.kcInputWrapperClass!}">

                                        <div class="${properties.kcLabelWrapperClass!}">
                                            <label for="${scopeField.name}">${msg(scopeField.name)}</label>
                                            <#if scopeField.required>*</#if>
                                        </div>

                                        <div class="${properties.kcInputWrapperClass!}">

                                        <#if scopeField.annotations['inputType']?? && scopeField.annotations['inputType'] == 'select'>
                                            <select name="${scopeField.name}" id="${scopeField.name}">
                                                <#list scopeField.allowedValues as allowedValue>
                                                <option value="${allowedValue}">${msg(allowedValue)}</option>
                                                </#list>
                                            </select>

                                        <#else>

                                            <input id="${scopeField.name}"
                                                   type="${scopeField.type}"
                                                   name="${scopeField.name}"
                                                   value="${(scopeField.value!'')}"
                                                   <#if scopeField.readonly>disabled</#if>
<#--                                                   -->
                                            />
                                        </#if>

                                            <#if messagesPerField.existsError(scopeField.name)>
                                            <span id="input-error-${scopeField.name?replace(".","-")}" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                                ${kcSanitize(messagesPerField.get(scopeField.name))?no_esc}
                                            </span>
                                            </#if>
                                        </div>
                                    </div>
                                </#list>
                            </div>
                            </#if>

                            <#-- -->

                            <div></div>
                        </div>
                    </#list>
                </div>
            </div>

            <div class="${properties.kcFormGroupClass!}">
                <div id="kc-form-buttons" class="${properties.kcFormButtonsClass!}">
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}"
                           name="accept" id="kc-login" type="submit" value="${msg("doContinue")}"/>
                    <input class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}"
                           name="cancel" id="kc-cancel" type="submit" value="${msg("doCancel")}"/>

                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>