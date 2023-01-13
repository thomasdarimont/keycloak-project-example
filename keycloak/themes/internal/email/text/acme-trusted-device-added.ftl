<#ftl output_format="plainText">
<#import "template.ftl" as layout>
<@layout.emailLayout>
${msg("acmeTrustedDeviceAddedBody",user.username,trustedDeviceInfo.deviceName)}
</@layout.emailLayout>
