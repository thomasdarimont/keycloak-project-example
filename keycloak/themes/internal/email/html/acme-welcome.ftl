<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmeWelcomeBodyHtml",realm.displayName, username, userDisplayName))?no_esc}
</@layout.emailLayout>
