<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmeAccountUpdatedBodyHtml",user.username,update.changedAttribute,update.changedValue))?no_esc}
</@layout.emailLayout>
