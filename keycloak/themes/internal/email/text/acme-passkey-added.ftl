<#ftl output_format="plainText">
<#import "template.ftl" as layout>
<@layout.emailLayout>
${msg("acmePasskeyAddedBody",user.username,msg(passkeyInfo.label))}
<#if passkeyInfo.label?? && passkeyInfo.label?has_content>
Details: ${kcSanitize(passkeyInfo.label)}
</#if>
</@layout.emailLayout>
