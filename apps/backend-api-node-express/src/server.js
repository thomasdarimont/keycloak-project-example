import fs from "fs";
import stoppable from "stoppable";
import {promisify} from "es6-promisify";

import spdy from "spdy";

function createServer(app, config, LOG) {

    LOG.info("Create server");

    const httpsServer = spdy.createServer({
        key: fs.readFileSync(config.TLS_KEY),
        cert: fs.readFileSync(config.TLS_CERT),
    }, app);

    // for Graceful shutdown see https://github.com/RisingStack/kubernetes-graceful-shutdown-example
    configureGracefulShutdown(httpsServer, config, LOG);

    // Start server
    httpsServer.listen(config.PORT, () => {
        LOG.info(`Listening on HTTPS port ${config.PORT}`);
    });
}


function configureGracefulShutdown(httpsServer, config, LOG) {
// Keep-alive connections doesn't let the server to close in time
// Destroy extension helps to force close connections
// Because we wait READINESS_PROBE_DELAY, we expect that all requests are fulfilled
// https://en.wikipedia.org/wiki/HTTP_persistent_connection
    stoppable(httpsServer);

    const serverDestroy = promisify(httpsServer.stop.bind(httpsServer));

// Graceful stop
    async function gracefulStop() {
        LOG.info('Server is shutting down...')

        try {
            await serverDestroy(); // close server first (ongoing requests)
            LOG.info('Successful graceful shutdown');
            process.exit(0); // exit with ok code
        } catch (err) {
            LOG.error('Error happened during graceful shutdown', err)
            process.exit(1) // exit with not ok code
        }
    }

// Support graceful shutdown
// do not accept more request and release resources
    process.on('SIGTERM', () => {
        LOG.info('Got SIGTERM. Graceful shutdown start');

        // Wait a little bit to give enough time for Kubernetes readiness probe to fail (we don't want more traffic)
        // Don't worry livenessProbe won't kill it until (failureThreshold: 3) => 30s
        // http://www.bite-code.com/2015/07/27/implementing-graceful-shutdown-for-docker-containers-in-go-part-2/
        setTimeout(gracefulStop, config.READINESS_PROBE_DELAY);
    });
}

export default createServer;
