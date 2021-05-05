/// <reference types="cypress" />
// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

import installLogsPrinter from "cypress-terminal-report/src/installLogsPrinter";

/**
 * @type {Cypress.PluginConfig}
 */
const pluginConfig: Cypress.PluginConfig = (on, config) => {
    // `on` is used to hook into various events Cypress emits
    // `config` is the resolved Cypress config

    installLogsPrinter(on, {
        includeSuccessfulHookLogs: true,
        printLogsToConsole: 'onFail'
    });

    on('before:browser:launch', (browser, launchOptions) => {
        // if (browser.family === 'chromium' && browser.name !== 'electron') {
        //
        //     // use fixed langauge
        //     // launchOptions.preferences.default['intl.accept_langauges'] = 'en';
        //
        //     // workaround for little memory in ci machines
        //     // launchOptions.args.push(
        //     //     '--disable-dev-shm-usage'
        //     // )
        // }
    });

    return config;
}

module.exports = pluginConfig;
