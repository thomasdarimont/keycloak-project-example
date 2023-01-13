<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("emailCodeBody", code))?no_esc}
</@layout.emailLayout>
