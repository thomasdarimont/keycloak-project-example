<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmeAccountBlockedBodyHtml",user.username))?no_esc}
</@layout.emailLayout>
