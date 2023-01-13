<#ftl output_format="plainText">
<#import "template.ftl" as layout>
<@layout.emailLayout>
${msg("acmeMfaAddedBody",user.username,mfaInfo.label)}
</@layout.emailLayout>
