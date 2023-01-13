<#import "template.ftl" as layout>
<@layout.emailLayout>
${kcSanitize(msg("acmeTrustedDeviceRemovedBodyHtml",user.username,trustedDeviceInfo.deviceName))?no_esc}
</@layout.emailLayout>
