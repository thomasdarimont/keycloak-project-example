<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmeMfaAddedBodyHtml",user.username,mfaInfo.label))?no_esc}
</@layout.emailLayout>
