<#ftl output_format="plainText">
<#import "template.ftl" as layout>
<@layout.emailLayout>
${msg("acmeWelcomeBodyHtml",realm.displayName, username, userDisplayName)}
</@layout.emailLayout>
