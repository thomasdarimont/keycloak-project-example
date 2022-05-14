import winston from "winston";

function initLogging(config) {

    const loggingFormat = winston.format.combine(
        winston.format.timestamp(),
        'json' === config.LOG_FORMAT
            ? winston.format.json()
            : winston.format.simple()
    );

    return winston.createLogger({
        level: config.LOG_LEVEL,
        format: loggingFormat,
        defaultMeta: {service: 'acme-webapp-saml-node-express'},
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
}

export default initLogging;
