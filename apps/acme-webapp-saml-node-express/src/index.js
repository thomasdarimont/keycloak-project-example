'use strict'

import config from './config.js';
import initLogging from './logging.js';

import createExpressApp from './express.js';
import createServer from "./server.js";

const LOG = initLogging(config);

const app = createExpressApp(config, LOG);
createServer(app, config, LOG);
