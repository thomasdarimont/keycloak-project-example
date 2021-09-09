import express from "express";
import cors from "cors";
import jwksRsa from "jwks-rsa";
import jwt from "express-jwt";

function createExpressApp(config, LOG) {

    const app = express();

    const corsOptions = {
        origin: [config.CORS_ALLOWED_ORIGIN],
        methods: config.CORS_ALLOWED_METHODS.split(","),
        optionsSuccessStatus: 200 // For legacy browser support
    }

    app.use(
        // CORS handling
        cors(corsOptions)
    );

// JWT Bearer Authorization
    let jwtOptions = {
        // Dynamically provide a signing key based on the kid in the header and the signing keys provided by the JWKS endpoint.
        secret: jwksRsa.expressJwtSecret({
            cache: true,
            rateLimit: true,

            jwksRequestsPerMinute: 5,
            jwksUri: `${config.ISSUER}/protocol/openid-connect/certs`,

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
        issuer: config.ISSUER,
        algorithms: ['RS256']
    };

    app.use('/api/*',
        // API Authorization
        jwt(jwtOptions)
    );

    return app;
}

export default createExpressApp;
