<#ftl output_format="plainText">
<#import "template.ftl" as layout>
<@layout.emailLayout>
${msg("acmeMfaRemovedBody",user.username,msg(mfaInfo.type))}
<#if mfaInfo.label?? && mfaInfo.label?has_content>
Details: ${kcSanitize(mfaInfo.label)}
</#if>
</@layout.emailLayout>
