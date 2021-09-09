'use strict'

import config from './config.js';
import initLogging from './logging.js';

import createExpressApp from './express.js';
import createApiEndpoints from './api.js';
import createServer from "./server.js";

const LOG = initLogging(config);

const app = createExpressApp(config, LOG);
createApiEndpoints(app, config, LOG);
createServer(app, config, LOG);
