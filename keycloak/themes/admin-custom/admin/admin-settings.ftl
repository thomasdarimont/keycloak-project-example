<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Keycloak Admin Settings</title>
    <meta name="robots" content="noindex, nofollow">

    <#if properties.meta?has_content>
        <#list properties.meta?split(' ') as meta>
            <meta name="${meta?split('==')[0]}" content="${meta?split('==')[1]}"/>
        </#list>
    </#if>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico"/>
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet"/>
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet"/>
        </#list>
    </#if>
    <#if properties.scripts?has_content>
        <#list properties.scripts?split(' ') as script>
            <script src="${url.resourcesPath}/${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
</head>

<body>

<h1>Realm Settings</h1>
<h2>Realm: ${realm.displayName}</h2>

<form class="pf-c-form pf-m-horizontal keycloak__form" method="post">

    <#list realmSettings.settings as setting>

        <div class="pf-c-form__group">
            <div class="pf-c-form__group-label">
                <label class="pf-c-form__label" for="${setting.name}">
                    <span class="pf-c-form__label-text">${setting.name}</span>
                </label>
                <button data-testid="help-label-admin-settings:${setting.name}" aria-label="admin-settings:${setting.name}"
                        class="pf-c-form__group-label-help">
                </button>
            </div>
            <div class="pf-c-form__group-control">
                <input id="${setting.name}" data-testid="${setting.name}" name="${setting.name}"
                       class="pf-c-form-control" type="text" aria-invalid="false"
                       data-ouia-component-type="PF4/TextInput" data-ouia-safe="true"
                       data-ouia-component-id="OUIA-Generated-TextInputBase-3" value="${setting.value}">
            </div>
        </div>
    </#list>

    <div class="pf-v5-c-form__group pf-m-action">
        <div class="pf-v5-c-form__group-control">
            <div class="pf-v5-c-form__actions">
                <button class="pf-v5-c-button pf-m-primary" name="action" value="save" type="submit">Save</button>
                <button class="pf-v5-c-button pf-m-link" name="action" value="cancel" type="submit">Cancel</button>
            </div>
        </div>
    </div>
</form>

</body>
</html>