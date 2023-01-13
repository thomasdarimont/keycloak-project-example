<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmeTrustedDeviceAddedBodyHtml",user.username,trustedDeviceInfo.deviceName))?no_esc}
</@layout.emailLayout>
