<#import "template.ftl" as layout>
<@layout.emailLayout>
    ${kcSanitize(msg("acmeMagicLinkEmailBodyHtml", userDisplayName, link))?no_esc}
</@layout.emailLayout>