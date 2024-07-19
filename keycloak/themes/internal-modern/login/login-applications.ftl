<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "header">
        ${msg("loginApplicationsTitle")}
    <#elseif section = "form">

        <!-- dump all available attribute keys -->
    <#--        <#list .data_model?keys as key>-->
    <#--            ${key}<br>-->
    <#--        </#list>-->

        <div class="pf-l-bullseye">
            <div class="pf-c-page__main-section pf-m-light">
                <div class="pf-l-flex pf-m-column">
                    <div class="pf-l-flex__item">
                        <h1>${msg('loginApplicationsGreeting', user.username)}</h1>
                        <p>${msg('loginApplicationsInfo')}</p>
                    </div>

                    <div class="pf-l-gallery pf-m-gutter">
                        <#list application.applications as app>
                            <div class="pf-l-gallery__item">
                                <div class="pf-c-card">
                                    <div class="pf-c-card__body pf-l-flex">
                                        <div class="pf-l-flex__item pf-m-fixed">
                                            <a href="${app.url}">
                                                <img src="${app.icon?default(url.resourcesPath + '/img/generic_app_icon.png')}"
                                                     alt="${msg(app.name)}" class="app-icon">
                                            </a>
                                        </div>
                                        <div class="pf-l-flex__item">
                                            <a href="${app.url}">
                                                <h3>${msg(app.name)}</h3>
                                            </a>
                                            <p>${msg(app.description)}</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </#list>
                    </div>
                </div>
            </div>
        </div>

    <#elseif section = "info" >

    </#if>

</@layout.registrationLayout>
