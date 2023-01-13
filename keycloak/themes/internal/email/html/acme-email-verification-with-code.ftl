<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmeEmailVerificationBodyCodeHtml",code))?no_esc}
</@layout.emailLayout>
