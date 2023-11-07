<#ftl output_format="plainText">
<#import "template.ftl" as layout>
<@layout.emailLayout>
${msg("acmeAccountUpdatedBodyHtml",user.username,update.changedAttribute,update.changedValue)}
</@layout.emailLayout>
