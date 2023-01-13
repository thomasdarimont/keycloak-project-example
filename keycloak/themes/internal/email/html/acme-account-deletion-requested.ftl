<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmeAccountDeletionRequestedBodyHtml",user.username,actionTokenUrl))?no_esc}
</@layout.emailLayout>
