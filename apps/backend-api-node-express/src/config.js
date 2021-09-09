const ISSUER = process.env.ISSUER || "https://id.acme.test:8443/auth/realms/acme-internal";
const PORT = process.env.PORT || 4743;
const CORS_ALLOWED_ORIGIN = process.env.CORS_ALLOWED_ORIGIN || 'https://apps.acme.test:4443';
const CORS_ALLOWED_METHODS = process.env.CORS_ALLOWED_METHODS || 'GET';
const TLS_CERT = process.env.TLS_CERT || '../../config/stage/dev/tls/acme.test+1.pem';
const TLS_KEY = process.env.TLS_KEY || '../../config/stage/dev/tls/acme.test+1-key.pem';
const LOG_LEVEL = process.env.LOG_LEVEL || 'info';
const LOG_FORMAT = process.env.LOG_FORMAT || 'json'; // plain / json
// see https://github.com/RisingStack/kubernetes-graceful-shutdown-example/blob/master/src/index.js
const READINESS_PROBE_DELAY = process.env.READINESS_PROBE_DELAY || 1000; // 2 * 2 * 1000; // failureThreshold: 2, periodSeconds: 2 (4s)


export default {
    ISSUER,
    PORT,
    CORS_ALLOWED_METHODS,
    CORS_ALLOWED_ORIGIN,
    TLS_CERT,
    TLS_KEY,
    LOG_LEVEL,
    LOG_FORMAT,
    READINESS_PROBE_DELAY,
};
