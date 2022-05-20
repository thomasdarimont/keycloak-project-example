import fs from "fs";

const IDP_ISSUER = process.env.IDP_ISSUER || "https://id.acme.test:8443/auth/realms/acme-internal";
const PORT = process.env.PORT || 4723;
const HOSTNAME = process.env.HOSTNAME || "apps.acme.test" + (PORT === 443 ? "" : ":"+ PORT);
const SP_ISSUER =  process.env.SP_ISSUER || "acme-webapp-saml-node-express";
const TLS_CERT_FILE = process.env.TLS_CERT_FILE || '../../config/stage/dev/tls/acme.test+1.pem';
const TLS_KEY_FILE = process.env.TLS_KEY_FILE || '../../config/stage/dev/tls/acme.test+1-key.pem';
const LOG_LEVEL = process.env.LOG_LEVEL || 'info';
const LOG_FORMAT = process.env.LOG_FORMAT || 'json'; // plain / json
// see https://github.com/RisingStack/kubernetes-graceful-shutdown-example/blob/master/src/index.js
const READINESS_PROBE_DELAY = process.env.READINESS_PROBE_DELAY || 1000; // 2 * 2 * 1000; // failureThreshold: 2, periodSeconds: 2 (4s)

const SESSION_SECRET = process.env.SECRET || 'keyboard cat';
let SAML_SP_KEY = process.env.SAML_SP_KEY || fs.readFileSync(TLS_KEY_FILE, "utf-8")
// realm certificate used to sign saml requests from Keycloak
let SAML_IDP_CERT = process.env.SAML_IDP_CERT;

if (!SAML_IDP_CERT) {
    console.log('Missing SAML_IDP_CERT env variable.');
    process.exit(1);
}

export default {
    IDP_ISSUER,
    SP_ISSUER,
    HOSTNAME,
    PORT,
    SAML_SP_KEY,
    SAML_IDP_CERT,
    TLS_CERT_FILE,
    TLS_KEY_FILE,
    LOG_LEVEL,
    LOG_FORMAT,
    READINESS_PROBE_DELAY,
    SESSION_SECRET
};
