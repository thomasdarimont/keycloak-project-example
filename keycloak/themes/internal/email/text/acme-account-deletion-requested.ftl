<#ftl output_format="plainText">
<#import "template.ftl" as layout>
<@layout.emailLayout>
${msg("acmeAccountDeletionRequestedBody",user.username,actionTokenUrl)}
</@layout.emailLayout>
