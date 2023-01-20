<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmeMfaRemovedBodyHtml",user.username,msg(mfaInfo.type)))?no_esc}
<#if mfaInfo.label?? && mfaInfo.label?has_content>
<p>Details: ${kcSanitize(mfaInfo.label)}</p>
</#if>
</@layout.emailLayout>
