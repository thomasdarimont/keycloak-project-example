<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmeMfaRemovedBodyHtml",user.username,mfaInfo.label))?no_esc}
</@layout.emailLayout>
