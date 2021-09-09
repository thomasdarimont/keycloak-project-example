import https from 'https';
import fs from 'fs';

import express from 'express';
import jwt from 'express-jwt';
import jwksRsa from 'jwks-rsa';

import cors from 'cors';

import winston from 'winston';

const ISSUER = process.env.ISSUER || "https://id.acme.test:8443/auth/realms/acme-internal";
const PORT = process.env.PORT || 4743;
const CORS_ALLOWED_ORIGIN = process.env.CORS_ALLOWED_ORIGIN || 'https://apps.acme.test:4443';
const CORS_ALLOWED_METHODS = process.env.CORS_ALLOWED_METHODS || 'GET';
const TLS_CERT = process.env.TLS_CERT || '../../config/stage/dev/tls/acme.test+1.pem';
const TLS_KEY = process.env.TLS_KEY || '../../config/stage/dev/tls/acme.test+1-key.pem';
const LOG_LEVEL = process.env.LOG_LEVEL || 'info';
const LOG_FORMAT = process.env.LOG_FORMAT || 'plain'; // plain / json

const LOG = winston.createLogger({
    level: LOG_LEVEL,
    format: 'json' === LOG_FORMAT ? winston.format.json() : winston.format.simple(),
    // defaultMeta: {service: 'user-service'},
    transports: [
        new winston.transports.Console(),
        //
        // - Write all logs with level `error` and below to `error.log`
        // - Write all logs with level `info` and below to `combined.log`
        //
        // new winston.transports.File({ filename: 'error.log', level: 'error' }),
        // new winston.transports.File({ filename: 'combined.log' }),
    ],
});

const app = express();

const corsOptions = {
    origin: [CORS_ALLOWED_ORIGIN],
    methods: CORS_ALLOWED_METHODS.split(","),
    optionsSuccessStatus: 200 // For legacy browser support
}

app.use(cors(corsOptions));

app.use('/api/*',
    jwt({
        // Dynamically provide a signing key based on the kid in the header and the signing keys provided by the JWKS endpoint.
        secret: jwksRsa.expressJwtSecret({
            cache: true,
            rateLimit: true,

            jwksRequestsPerMinute: 5,
            jwksUri: `${ISSUER}/protocol/openid-connect/certs`,

            handleSigningKeyError: (err, cb) => {
                if (err instanceof jwksRsa.SigningKeyNotFoundError) {
                    return cb(new Error('Could not fetch certs from JWKS endpoint.'));
                }
                return cb(err);
            }
        }),
        // Validate the audience.
        // audience: 'urn:my-resource-server',
        // Validate the issuer.
        issuer: ISSUER,
        algorithms: ['RS256']
    }));

// API routes can then access JWT claims in the request object
app.get('/api/users/me', (req, res) => {

    let username = req.user.preferred_username;

    LOG.info(`### Accessing ${req.path}`);

    const data = {
        message: `Hello ${username}`,
    };

    res.status(200).send(JSON.stringify(data));
});

const httpsServer = https.createServer({
    key: fs.readFileSync(TLS_KEY),
    cert: fs.readFileSync(TLS_CERT),
}, app);

httpsServer.listen(PORT, () => {
    console.log(`API is listening on HTTPS port ${PORT}`);
});
