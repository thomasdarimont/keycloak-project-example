<#ftl output_format="plainText">
<#import "template.ftl" as layout>
<@layout.emailLayout>
${msg("acmeMagicLinkEmailBody", userDisplayName, link)}
</@layout.emailLayout>