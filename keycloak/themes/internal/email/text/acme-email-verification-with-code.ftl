<#ftl output_format="plainText">
<#import "template.ftl" as layout>
<@layout.emailLayout>
${msg("acmeEmailVerificationBodyCode",code)}
</@layout.emailLayout>
