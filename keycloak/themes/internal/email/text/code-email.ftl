<#ftl output_format="plainText">
<#import "template.ftl" as layout>
<@layout.emailLayout>
${msg("emailCodeBody", code)}
</@layout.emailLayout>
