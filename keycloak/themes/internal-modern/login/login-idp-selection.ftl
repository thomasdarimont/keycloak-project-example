<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
<#if section = "header">
${msg("loginAccountTitle")}
<#elseif section = "form">
<!-- customization -->
<div id="kc-form">
    <div id="kc-form-wrapper">
        <div id="kc-social-providers" class="${properties.kcFormSocialAccountSectionClass!}">
            <hr/>
<#--            <h4>${msg("identity-provider-login-label")}</h4>-->

            <ul class="${properties.kcFormSocialAccountListClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountListGridClass!}</#if>" style="display: block">
                <#list customSocial.providers as p>
                    <li>
                        <a id="social-${p.alias}" class="${properties.kcFormSocialAccountListButtonClass!} <#if social.providers?size gt 3>${properties.kcFormSocialAccountGridItem!}</#if>"
                           type="button" href="${p.loginUrl}">
                            <#if p.iconClasses?has_content>
                                <i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i>
                                <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName!}</span>
                            <#else>
                                <span class="${properties.kcFormSocialAccountNameClass!}">${p.displayName!}</span>
                            </#if>
                        </a>
                    </li>
                </#list>
            </ul>
        </div>
    </div>
</div>
</#if>

</@layout.registrationLayout>