<#macro passwordPolicyCheck>
    <#if acmeLogin.passwordPolicy??>
        <h1>Password Policy Check</h1>
        <div>${(acmeLogin.passwordPolicy)!'#'}</div>
        <script>
            console.log("validate password policy");
        </script>
    </#if>
</#macro>

<#macro requiredActionInfo>
    <#if acmeLogin.lastProcessedAction??>
        <h1>Required Action Info</h1>
        <div>${(acmeLogin.lastProcessedAction)!'#'}</div>
    </#if>
</#macro>
