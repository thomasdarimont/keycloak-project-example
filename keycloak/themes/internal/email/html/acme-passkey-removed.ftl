<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmePasskeyRemovedBodyHtml",user.username,msg(passkeyInfo.label)))?no_esc}
<#if passkeyInfo.label?? && passkeyInfo.label?has_content>
<p>Details: ${kcSanitize(passkeyInfo.label)}</p>
</#if>
</@layout.emailLayout>
