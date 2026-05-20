// @ts-check
/**
 * @import {Acr, KeycloakAccountOptions, KeycloakAdapter, KeycloakConfig, KeycloakError, KeycloakFlow, KeycloakInitOptions, KeycloakLoginOptions, KeycloakLogoutOptions, KeycloakPkceMethod, KeycloakProfile, KeycloakRegisterOptions, KeycloakResourceAccess, KeycloakResponseMode, KeycloakResponseType, KeycloakRoles, KeycloakTokenParsed, OpenIdProviderMetadata} from "./keycloak.ts"
 */
/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const CONTENT_TYPE_JSON = 'application/json'

/**
 * @typedef {Object} Endpoints
 * @property {() => string} authorize
 * @property {() => string} token
 * @property {() => string} logout
 * @property {() => string} checkSessionIframe
 * @property {() => string=} thirdPartyCookiesIframe
 * @property {() => string} register
 * @property {() => string} userinfo
 */

/**
 * @typedef {Object} LoginIframe
 * @property {boolean} enable
 * @property {((error: Error | null, value?: boolean) => void)[]} callbackList
 * @property {number} interval
 * @property {HTMLIFrameElement=} iframe
 * @property {string=} iframeOrigin
 */

export default class Keycloak {
  /** @type {Pick<PromiseWithResolvers<boolean>, 'resolve' | 'reject'>[]} */
  #refreshQueue = []
  /** @type {KeycloakAdapter} */
  #adapter
  /** @type {boolean} */
  #useNonce = true
  /** @type {CallbackStorage} */
  #callbackStorage
  #logInfo = this.#createLogger(console.info)
  #logWarn = this.#createLogger(console.warn)
  /** @type {LoginIframe} */
  #loginIframe = {
    enable: true,
    callbackList: [],
    interval: 5
  }

  /** @type {KeycloakConfig} config */
  #config
  didInitialize = false
  authenticated = false
  loginRequired = false
  /** @type {KeycloakResponseMode} */
  responseMode = 'fragment'
  /** @type {KeycloakResponseType} */
  responseType = 'code'
  /** @type {KeycloakFlow} */
  flow = 'standard'
  /** @type {number?} */
  timeSkew = null
  /** @type {string=} */
  redirectUri
  /** @type {string=} */
  silentCheckSsoRedirectUri
  /** @type {boolean} */
  silentCheckSsoFallback = true
  /** @type {KeycloakPkceMethod} */
  pkceMethod = 'S256'
  enableLogging = false
  /** @type {'GET' | 'POST'} */
  logoutMethod = 'GET'
  /** @type {string=} */
  scope
  messageReceiveTimeout = 10000
  /** @type {string=} */
  idToken
  /** @type {KeycloakTokenParsed=} */
  idTokenParsed
  /** @type {string=} */
  token
  /** @type {KeycloakTokenParsed=} */
  tokenParsed
  /** @type {string=} */
  refreshToken
  /** @type {KeycloakTokenParsed=} */
  refreshTokenParsed
  /** @type {string=} */
  clientId
  /** @type {string=} */
  sessionId
  /** @type {string=} */
  subject
  /** @type {string=} */
  authServerUrl
  /** @type {string=} */
  realm
  /** @type {KeycloakRoles=} */
  realmAccess
  /** @type {KeycloakResourceAccess=} */
  resourceAccess
  /** @type {KeycloakProfile=} */
  profile
  /** @type {{}=} */
  userInfo
  /** @type {Endpoints} */
  endpoints
  /** @type {number=} */
  tokenTimeoutHandle
  /** @type {() => void=} */
  onAuthSuccess
  /** @type {(errorData?: KeycloakError) => void=} */
  onAuthError
  /** @type {() => void=} */
  onAuthRefreshSuccess
  /** @type {() => void=} */
  onAuthRefreshError
  /** @type {() => void=} */
  onTokenExpired
  /** @type {() => void=} */
  onAuthLogout
  /** @type {(authenticated: boolean) => void=} */
  onReady
  /** @type {(status: 'success' | 'cancelled' | 'error', action: string) => void=} */
  onActionUpdate

  /**
   * @param {KeycloakConfig} config
   */
  constructor (config) {
    if (typeof config !== 'string' && !isObject(config)) {
      throw new Error("The 'Keycloak' constructor must be provided with a configuration object, or a URL to a JSON configuration file.")
    }

    if (isObject(config)) {
      const requiredProperties = 'oidcProvider' in config
        ? ['clientId']
        : ['url', 'realm', 'clientId']

      for (const property of requiredProperties) {
        if (!(property in config)) {
          throw new Error(`The configuration object is missing the required '${property}' property.`)
        }
      }
    }

    if (!globalThis.isSecureContext) {
      this.#logWarn(
        "[KEYCLOAK] Keycloak JS must be used in a 'secure context' to function properly as it relies on browser APIs that are otherwise not available.\n" +
                'Continuing to run your application insecurely will lead to unexpected behavior and breakage.\n\n' +
                'For more information see: https://developer.mozilla.org/en-US/docs/Web/Security/Secure_Contexts'
      )
    }

    this.#config = config
  }

  /**
   * @param {KeycloakInitOptions} initOptions
   * @returns {Promise<boolean>}
   */
  init = async (initOptions = {}) => {
    if (this.didInitialize) {
      throw new Error("A 'Keycloak' instance can only be initialized once.")
    }

    this.didInitialize = true
    this.#callbackStorage = createCallbackStorage()

    const adapters = ['default', 'cordova', 'cordova-native']

    if (typeof initOptions.adapter === 'string' && adapters.includes(initOptions.adapter)) {
      this.#adapter = this.#loadAdapter(initOptions.adapter)
    } else if (typeof initOptions.adapter === 'object') {
      this.#adapter = initOptions.adapter
    } else if ('Cordova' in window || 'cordova' in window) {
      this.#adapter = this.#loadAdapter('cordova')
    } else {
      this.#adapter = this.#loadAdapter('default')
    }

    if (typeof initOptions.useNonce !== 'undefined') {
      this.#useNonce = initOptions.useNonce
    }

    if (typeof initOptions.checkLoginIframe !== 'undefined') {
      this.#loginIframe.enable = initOptions.checkLoginIframe
    }

    if (initOptions.checkLoginIframeInterval) {
      this.#loginIframe.interval = initOptions.checkLoginIframeInterval
    }

    if (initOptions.onLoad === 'login-required') {
      this.loginRequired = true
    }

    if (initOptions.responseMode) {
      if (initOptions.responseMode === 'query' || initOptions.responseMode === 'fragment') {
        this.responseMode = initOptions.responseMode
      } else {
        throw new Error('Invalid value for responseMode')
      }
    }

    if (initOptions.flow) {
      switch (initOptions.flow) {
        case 'standard':
          this.responseType = 'code'
          break
        case 'implicit':
          this.responseType = 'id_token token'
          break
        case 'hybrid':
          this.responseType = 'code id_token token'
          break
        default:
          throw new Error('Invalid value for flow')
      }
      this.flow = initOptions.flow
    }

    if (typeof initOptions.timeSkew === 'number') {
      this.timeSkew = initOptions.timeSkew
    }

    if (initOptions.redirectUri) {
      this.redirectUri = initOptions.redirectUri
    }

    if (initOptions.silentCheckSsoRedirectUri) {
      this.silentCheckSsoRedirectUri = initOptions.silentCheckSsoRedirectUri
    }

    if (typeof initOptions.silentCheckSsoFallback === 'boolean') {
      this.silentCheckSsoFallback = initOptions.silentCheckSsoFallback
    }

    if (typeof initOptions.pkceMethod !== 'undefined') {
      if (initOptions.pkceMethod !== 'S256' && initOptions.pkceMethod !== false) {
        throw new TypeError(`Invalid value for pkceMethod', expected 'S256' or false but got ${initOptions.pkceMethod}.`)
      }

      this.pkceMethod = initOptions.pkceMethod
    }

    if (typeof initOptions.enableLogging === 'boolean') {
      this.enableLogging = initOptions.enableLogging
    }

    if (initOptions.logoutMethod === 'POST') {
      this.logoutMethod = 'POST'
    }

    if (typeof initOptions.scope === 'string') {
      this.scope = initOptions.scope
    }

    if (typeof initOptions.messageReceiveTimeout === 'number' && initOptions.messageReceiveTimeout > 0) {
      this.messageReceiveTimeout = initOptions.messageReceiveTimeout
    }

    await this.#loadConfig()
    await this.#check3pCookiesSupported()
    await this.#processInit(initOptions)

    this.onReady?.(this.authenticated)

    return this.authenticated
  }

  /**
   * @param {"default" | "cordova" | "cordova-native"} type
   * @returns {KeycloakAdapter}
   */
  #loadAdapter (type) {
    if (type === 'default') {
      return this.#loadDefaultAdapter()
    }

    if (type === 'cordova') {
      this.#loginIframe.enable = false
      return this.#loadCordovaAdapter()
    }

    if (type === 'cordova-native') {
      this.#loginIframe.enable = false
      return this.#loadCordovaNativeAdapter()
    }

    throw new Error('invalid adapter type: ' + type)
  }

  /**
   * @returns {KeycloakAdapter}
   */
  #loadDefaultAdapter () {
    /** @type {KeycloakAdapter['redirectUri']}{} */
    const redirectUri = (options) => {
      return options?.redirectUri || this.redirectUri || globalThis.location.href
    }

    return {
      login: async (options) => {
        window.location.assign(await this.createLoginUrl(options))
        return await new Promise(() => {})
      },

      logout: async (options) => {
        const logoutMethod = options?.logoutMethod ?? this.logoutMethod

        if (logoutMethod === 'GET') {
          window.location.replace(this.createLogoutUrl(options))
          return
        }

        // Create form to send POST request.
        const form = document.createElement('form')

        form.setAttribute('method', 'POST')
        form.setAttribute('action', this.createLogoutUrl(options))
        form.style.display = 'none'

        // Add data to form as hidden input fields.
        const data = {
          id_token_hint: this.idToken,
          client_id: this.clientId,
          post_logout_redirect_uri: redirectUri(options)
        }

        for (const [name, value] of Object.entries(data)) {
          const input = document.createElement('input')

          input.setAttribute('type', 'hidden')
          input.setAttribute('name', name)
          input.setAttribute('value', /** @type {string} */ (value))

          form.appendChild(input)
        }

        // Append form to page and submit it to perform logout and redirect.
        document.body.appendChild(form)
        form.submit()
      },

      register: async (options) => {
        window.location.assign(await this.createRegisterUrl(options))
        return await new Promise(() => {})
      },

      accountManagement: async () => {
        const accountUrl = this.createAccountUrl()
        if (typeof accountUrl !== 'undefined') {
          window.location.href = accountUrl
        } else {
          throw new Error('Not supported by the OIDC server')
        }
        return await new Promise(() => {})
      },

      redirectUri
    }
  }

  /**
   * @returns {KeycloakAdapter}
   */
  #loadCordovaAdapter () {
    /**
     * @param {string} loginUrl
     * @param {string} target
     * @param {string} options
     * @returns {WindowProxy | null}
     */
    const cordovaOpenWindowWrapper = (loginUrl, target, options) => {
      if (window.cordova && window.cordova.InAppBrowser) {
        // Use inappbrowser for IOS and Android if available
        return window.cordova.InAppBrowser.open(loginUrl, target, options)
      } else {
        return window.open(loginUrl, target, options)
      }
    }

    const shallowCloneCordovaOptions = (userOptions) => {
      if (userOptions && userOptions.cordovaOptions) {
        return Object.keys(userOptions.cordovaOptions).reduce((options, optionName) => {
          options[optionName] = userOptions.cordovaOptions[optionName]
          return options
        }, {})
      } else {
        return {}
      }
    }

    const formatCordovaOptions = (cordovaOptions) => {
      return Object.keys(cordovaOptions).reduce((options, optionName) => {
        options.push(optionName + '=' + cordovaOptions[optionName])
        return options
      }, []).join(',')
    }

    const createCordovaOptions = (userOptions) => {
      const cordovaOptions = shallowCloneCordovaOptions(userOptions)
      cordovaOptions.location = 'no'
      if (userOptions && userOptions.prompt === 'none') {
        cordovaOptions.hidden = 'yes'
      }
      return formatCordovaOptions(cordovaOptions)
    }

    const getCordovaRedirectUri = () => {
      return this.redirectUri || 'http://localhost'
    }

    return {
      login: async (options) => {
        const cordovaOptions = createCordovaOptions(options)
        const loginUrl = await this.createLoginUrl(options)
        const ref = cordovaOpenWindowWrapper(loginUrl, '_blank', cordovaOptions)
        let completed = false
        let closed = false

        function closeBrowser () {
          closed = true
          ref.close()
        };

        return await new Promise((resolve, reject) => {
          ref.addEventListener('loadstart', async (event) => {
            if (event.url.indexOf(getCordovaRedirectUri()) === 0) {
              const callback = this.#parseCallback(event.url)
              completed = true
              closeBrowser()

              try {
                await this.#processCallback(callback)
                resolve()
              } catch (error) {
                reject(error)
              }
            }
          })

          ref.addEventListener('loaderror', async (event) => {
            if (!completed) {
              if (event.url.indexOf(getCordovaRedirectUri()) === 0) {
                const callback = this.#parseCallback(event.url)
                completed = true
                closeBrowser()

                try {
                  await this.#processCallback(callback)
                  resolve()
                } catch (error) {
                  reject(error)
                }
              } else {
                reject(new Error('Unable to process login.'))
                closeBrowser()
              }
            }
          })

          ref.addEventListener('exit', function (event) {
            if (!closed) {
              reject(new Error('User closed the login window.'))
            }
          })
        })
      },

      logout: async (options) => {
        const logoutUrl = this.createLogoutUrl(options)
        const ref = cordovaOpenWindowWrapper(logoutUrl, '_blank', 'location=no,hidden=yes,clearcache=yes')
        let error = false

        ref.addEventListener('loadstart', (event) => {
          if (event.url.indexOf(getCordovaRedirectUri()) === 0) {
            ref.close()
          }
        })

        ref.addEventListener('loaderror', (event) => {
          if (event.url.indexOf(getCordovaRedirectUri()) === 0) {
            ref.close()
          } else {
            error = true
            ref.close()
          }
        })

        await new Promise((resolve, reject) => {
          ref.addEventListener('exit', () => {
            if (error) {
              reject(new Error('User closed the login window.'))
            } else {
              this.clearToken()
              resolve()
            }
          })
        })
      },

      register: async (options) => {
        const registerUrl = await this.createRegisterUrl()
        const cordovaOptions = createCordovaOptions(options)
        const ref = cordovaOpenWindowWrapper(registerUrl, '_blank', cordovaOptions)

        /** @type {Promise<void>} */
        const promise = new Promise((resolve, reject) => {
          ref.addEventListener('loadstart', async (event) => {
            if (event.url.indexOf(getCordovaRedirectUri()) === 0) {
              ref.close()
              const oauth = this.#parseCallback(event.url)

              try {
                await this.#processCallback(oauth)
                resolve()
              } catch (error) {
                reject(error)
              }
            }
          })
        })

        await promise
      },

      accountManagement: async () => {
        const accountUrl = this.createAccountUrl()
        if (typeof accountUrl !== 'undefined') {
          const ref = cordovaOpenWindowWrapper(accountUrl, '_blank', 'location=no')
          ref.addEventListener('loadstart', function (event) {
            if (event.url.indexOf(getCordovaRedirectUri()) === 0) {
              ref.close()
            }
          })
        } else {
          throw new Error('Not supported by the OIDC server')
        }
      },

      redirectUri: () => {
        return getCordovaRedirectUri()
      }
    }
  }

  /**
   * @returns {KeycloakAdapter}
   */
  #loadCordovaNativeAdapter () {
    /* global universalLinks */
    return {
      login: async (options) => {
        const loginUrl = await this.createLoginUrl(options)

        await new Promise((resolve, reject) => {
          universalLinks.subscribe('keycloak', async (event) => {
            universalLinks.unsubscribe('keycloak')
            window.cordova.plugins.browsertab.close()
            const oauth = this.#parseCallback(event.url)

            try {
              await this.#processCallback(oauth)
              resolve()
            } catch (error) {
              reject(error)
            }
          })

          window.cordova.plugins.browsertab.openUrl(loginUrl)
        })
      },

      logout: async (options) => {
        const logoutUrl = this.createLogoutUrl(options)

        await new Promise((resolve) => {
          universalLinks.subscribe('keycloak', () => {
            universalLinks.unsubscribe('keycloak')
            window.cordova.plugins.browsertab.close()
            this.clearToken()
            resolve()
          })

          window.cordova.plugins.browsertab.openUrl(logoutUrl)
        })
      },

      register: async (options) => {
        const registerUrl = await this.createRegisterUrl(options)

        await new Promise((resolve, reject) => {
          universalLinks.subscribe('keycloak', async (event) => {
            universalLinks.unsubscribe('keycloak')
            window.cordova.plugins.browsertab.close()
            const oauth = this.#parseCallback(event.url)
            try {
              await this.#processCallback(oauth)
              resolve()
            } catch (error) {
              reject(error)
            }
          })

          window.cordova.plugins.browsertab.openUrl(registerUrl)
        })
      },

      accountManagement: async () => {
        const accountUrl = this.createAccountUrl()
        if (typeof accountUrl !== 'undefined') {
          window.cordova.plugins.browsertab.openUrl(accountUrl)
        } else {
          throw new Error('Not supported by the OIDC server')
        }
      },

      redirectUri: (options) => {
        if (options && options.redirectUri) {
          return options.redirectUri
        } else if (this.redirectUri) {
          return this.redirectUri
        } else {
          return 'http://localhost'
        }
      }
    }
  }

  /**
   * @returns {Promise<void>}
   */
  async #loadConfig () {
    if (typeof this.#config === 'string') {
      const jsonConfig = await fetchJsonConfig(this.#config)
      this.authServerUrl = jsonConfig['auth-server-url']
      this.realm = jsonConfig.realm
      this.clientId = jsonConfig.resource
      this.#setupEndpoints()
    } else {
      this.clientId = this.#config.clientId

      if ('oidcProvider' in this.#config) {
        await this.#loadOidcConfig(this.#config.oidcProvider)
      } else {
        this.authServerUrl = this.#config.url
        this.realm = this.#config.realm
        this.#setupEndpoints()
      }
    }
  }

  /**
   * @returns {void}
   */
  #setupEndpoints () {
    this.endpoints = {
      authorize: () => {
        return this.#getRealmUrl() + '/protocol/openid-connect/auth'
      },
      token: () => {
        return this.#getRealmUrl() + '/protocol/openid-connect/token'
      },
      logout: () => {
        return this.#getRealmUrl() + '/protocol/openid-connect/logout'
      },
      checkSessionIframe: () => {
        return this.#getRealmUrl() + '/protocol/openid-connect/login-status-iframe.html'
      },
      thirdPartyCookiesIframe: () => {
        return this.#getRealmUrl() + '/protocol/openid-connect/3p-cookies/step1.html'
      },
      register: () => {
        return this.#getRealmUrl() + '/protocol/openid-connect/registrations'
      },
      userinfo: () => {
        return this.#getRealmUrl() + '/protocol/openid-connect/userinfo'
      }
    }
  }

  /**
   * @param {string | OpenIdProviderMetadata} oidcProvider
   * @returns {Promise<void>}
   */
  async #loadOidcConfig (oidcProvider) {
    if (typeof oidcProvider === 'string') {
      const url = `${stripTrailingSlash(oidcProvider)}/.well-known/openid-configuration`
      const openIdConfig = await fetchOpenIdConfig(url)
      this.#setupOidcEndpoints(openIdConfig)
    } else {
      this.#setupOidcEndpoints(oidcProvider)
    }
  }

  /**
   * @param {OpenIdProviderMetadata} config
   * @returns {void}
   */
  #setupOidcEndpoints (config) {
    this.endpoints = {
      authorize () {
        return config.authorization_endpoint
      },
      token () {
        return config.token_endpoint
      },
      logout () {
        if (!config.end_session_endpoint) {
          throw new Error('Not supported by the OIDC server')
        }
        return config.end_session_endpoint
      },
      checkSessionIframe () {
        if (!config.check_session_iframe) {
          throw new Error('Not supported by the OIDC server')
        }
        return config.check_session_iframe
      },
      register () {
        throw new Error('Redirection to "Register user" page not supported in standard OIDC mode')
      },
      userinfo () {
        if (!config.userinfo_endpoint) {
          throw new Error('Not supported by the OIDC server')
        }
        return config.userinfo_endpoint
      }
    }
  }

  /**
   * @returns {Promise<void>}
   */
  async #check3pCookiesSupported () {
    if ((!this.#loginIframe.enable && !this.silentCheckSsoRedirectUri) || typeof this.endpoints.thirdPartyCookiesIframe !== 'function') {
      return
    }

    const iframe = document.createElement('iframe')
    iframe.setAttribute('src', this.endpoints.thirdPartyCookiesIframe())
    iframe.setAttribute('sandbox', 'allow-storage-access-by-user-activation allow-scripts allow-same-origin')
    iframe.setAttribute('title', 'keycloak-3p-check-iframe')
    iframe.style.display = 'none'
    document.body.appendChild(iframe)

    /** @type {Promise<void>} */
    const promise = new Promise((resolve) => {
      /**
       * @param {MessageEvent} event
       */
      const messageCallback = (event) => {
        if (iframe.contentWindow !== event.source) {
          return
        }

        if (event.data !== 'supported' && event.data !== 'unsupported') {
          return
        } else if (event.data === 'unsupported') {
          this.#logWarn(
            '[KEYCLOAK] Your browser is blocking access to 3rd-party cookies, this means:\n\n' +
                        ' - It is not possible to retrieve tokens without redirecting to the Keycloak server (a.k.a. no support for silent authentication).\n' +
                        ' - It is not possible to automatically detect changes to the session status (such as the user logging out in another tab).\n\n' +
                        'For more information see: https://www.keycloak.org/securing-apps/javascript-adapter#_modern_browsers'
          )

          this.#loginIframe.enable = false
          if (this.silentCheckSsoFallback) {
            this.silentCheckSsoRedirectUri = undefined
          }
        }

        document.body.removeChild(iframe)
        window.removeEventListener('message', messageCallback)
        resolve()
      }

      window.addEventListener('message', messageCallback, false)
    })

    return await applyTimeoutToPromise(promise, this.messageReceiveTimeout, 'Timeout when waiting for 3rd party check iframe message.')
  }

  /**
   * @param {KeycloakInitOptions} initOptions
   * @returns {Promise<void>}
   */
  async #processInit (initOptions) {
    const callback = this.#parseCallback(window.location.href)

    if (callback?.newUrl) {
      window.history.replaceState(window.history.state, '', callback.newUrl)
    }

    if (callback && callback.valid) {
      await this.#setupCheckLoginIframe()
      await this.#processCallback(callback)
      return
    }

    /** @param {boolean} prompt */
    const doLogin = async (prompt) => {
      /** @type {KeycloakLoginOptions} */
      const options = {}

      if (!prompt) {
        options.prompt = 'none'
      }

      if (initOptions.locale) {
        options.locale = initOptions.locale
      }

      await this.login(options)
    }

    const onLoad = async () => {
      switch (initOptions.onLoad) {
        case 'check-sso':
          if (this.#loginIframe.enable) {
            await this.#setupCheckLoginIframe()
            const unchanged = await this.#checkLoginIframe()

            if (!unchanged) {
              this.silentCheckSsoRedirectUri ? await this.#checkSsoSilently() : await doLogin(false)
            }
          } else {
            this.silentCheckSsoRedirectUri ? await this.#checkSsoSilently() : await doLogin(false)
          }
          break
        case 'login-required':
          await doLogin(true)
          break
        default:
          throw new Error('Invalid value for onLoad')
      }
    }

    if (initOptions.token && initOptions.refreshToken) {
      this.#setToken(initOptions.token, initOptions.refreshToken, initOptions.idToken)

      if (this.#loginIframe.enable) {
        await this.#setupCheckLoginIframe()
        const unchanged = await this.#checkLoginIframe()

        if (unchanged) {
          this.onAuthSuccess?.()
          this.#scheduleCheckIframe()
        }
      } else {
        try {
          await this.updateToken(-1)
          this.onAuthSuccess?.()
        } catch (error) {
          this.onAuthError?.()
          if (initOptions.onLoad) {
            await onLoad()
          } else {
            throw error
          }
        }
      }
    } else if (initOptions.onLoad) {
      await onLoad()
    }
  }

  /**
   * @returns {Promise<void>}
   */
  async #setupCheckLoginIframe () {
    if (!this.#loginIframe.enable || this.#loginIframe.iframe) {
      return
    }

    const iframe = document.createElement('iframe')
    this.#loginIframe.iframe = iframe
    iframe.setAttribute('src', this.endpoints.checkSessionIframe())
    iframe.setAttribute('sandbox', 'allow-storage-access-by-user-activation allow-scripts allow-same-origin')
    iframe.setAttribute('title', 'keycloak-session-iframe')
    iframe.style.display = 'none'
    document.body.appendChild(iframe)

    /**
     * @param {MessageEvent} event
     */
    const messageCallback = (event) => {
      if (event.origin !== this.#loginIframe.iframeOrigin || this.#loginIframe.iframe?.contentWindow !== event.source) {
        return
      }

      if (!(event.data === 'unchanged' || event.data === 'changed' || event.data === 'error')) {
        return
      }

      if (event.data !== 'unchanged') {
        this.clearToken()
      }

      const callbacks = this.#loginIframe.callbackList
      this.#loginIframe.callbackList = []

      for (const callback of callbacks.reverse()) {
        if (event.data === 'error') {
          callback(new Error('Error while checking login iframe'))
        } else {
          callback(null, event.data === 'unchanged')
        }
      }
    }

    window.addEventListener('message', messageCallback, false)

    /** @type {Promise<void>} */
    const promise = new Promise((resolve) => {
      iframe.addEventListener('load', () => {
        const authUrl = this.endpoints.authorize()
        if (authUrl.startsWith('/')) {
          this.#loginIframe.iframeOrigin = globalThis.location.origin
        } else {
          this.#loginIframe.iframeOrigin = new URL(authUrl).origin
        }
        resolve()
      })
    })

    await promise
  }

  /**
   * @returns {Promise<boolean | undefined>}
   */
  async #checkLoginIframe () {
    if (!this.#loginIframe.iframe || !this.#loginIframe.iframeOrigin) {
      return
    }

    const message = `${this.clientId} ${(this.sessionId ? this.sessionId : '')}`
    const origin = this.#loginIframe.iframeOrigin

    /** @type {Promise<boolean>} */
    const promise = new Promise((resolve, reject) => {
      /** @type {(error: Error | null, value?: boolean) => void} */
      const callback = (error, result) => error ? reject(error) : resolve(/** @type {boolean} */ (result))

      this.#loginIframe.callbackList.push(callback)

      if (this.#loginIframe.callbackList.length === 1) {
        this.#loginIframe.iframe?.contentWindow?.postMessage(message, origin)
      }
    })

    return await promise
  }

  /**
   * @returns {Promise<void>}
   */
  async #checkSsoSilently () {
    const iframe = document.createElement('iframe')
    const src = await this.createLoginUrl({ prompt: 'none', redirectUri: this.silentCheckSsoRedirectUri })
    iframe.setAttribute('src', src)
    iframe.setAttribute('sandbox', 'allow-storage-access-by-user-activation allow-scripts allow-same-origin')
    iframe.setAttribute('title', 'keycloak-silent-check-sso')
    iframe.style.display = 'none'
    document.body.appendChild(iframe)

    return await new Promise((resolve, reject) => {
      /**
       * @param {MessageEvent} event
       */
      const messageCallback = async (event) => {
        if (event.origin !== window.location.origin || iframe.contentWindow !== event.source) {
          return
        }

        const oauth = this.#parseCallback(event.data)

        try {
          await this.#processCallback(oauth)
          resolve()
        } catch (error) {
          reject(error)
        }

        document.body.removeChild(iframe)
        window.removeEventListener('message', messageCallback)
      }

      window.addEventListener('message', messageCallback)
    })
  };

  /**
   * @param {string} url
   */
  #parseCallback (url) {
    const oauth = this.#parseCallbackUrl(url)
    if (!oauth) {
      return
    }

    const oauthState = this.#callbackStorage.get(oauth.state)

    if (oauthState) {
      oauth.valid = true
      oauth.redirectUri = oauthState.redirectUri
      oauth.storedNonce = oauthState.nonce
      oauth.prompt = oauthState.prompt
      oauth.pkceCodeVerifier = oauthState.pkceCodeVerifier
      oauth.loginOptions = oauthState.loginOptions
    }

    return oauth
  }

  /**
   * @param {string} urlString
   */
  #parseCallbackUrl (urlString) {
    let supportedParams = []
    switch (this.flow) {
      case 'standard':
        supportedParams = ['code', 'state', 'session_state', 'kc_action_status', 'kc_action', 'iss']
        break
      case 'implicit':
        supportedParams = ['access_token', 'token_type', 'id_token', 'state', 'session_state', 'expires_in', 'kc_action_status', 'kc_action', 'iss']
        break
      case 'hybrid':
        supportedParams = ['access_token', 'token_type', 'id_token', 'code', 'state', 'session_state', 'expires_in', 'kc_action_status', 'kc_action', 'iss']
        break
    }

    supportedParams.push('error')
    supportedParams.push('error_description')
    supportedParams.push('error_uri')

    const url = new URL(urlString)
    let newUrl = ''
    let parsed

    if (this.responseMode === 'query' && url.searchParams.size > 0) {
      parsed = this.#parseCallbackParams(url.search, supportedParams)
      url.search = parsed.paramsString
      newUrl = url.toString()
    } else if (this.responseMode === 'fragment' && url.hash.length > 0) {
      parsed = this.#parseCallbackParams(url.hash.substring(1), supportedParams)
      url.hash = parsed.paramsString
      newUrl = url.toString()
    }

    if (parsed?.oauthParams) {
      if (this.flow === 'standard' || this.flow === 'hybrid') {
        if ((parsed.oauthParams.code || parsed.oauthParams.error) && parsed.oauthParams.state) {
          parsed.oauthParams.newUrl = newUrl
          return parsed.oauthParams
        }
      } else if (this.flow === 'implicit') {
        if ((parsed.oauthParams.access_token || parsed.oauthParams.error) && parsed.oauthParams.state) {
          parsed.oauthParams.newUrl = newUrl
          return parsed.oauthParams
        }
      }
    }
  }

  /**
   * @typedef {Object} ParsedCallbackParams
   * @property {string} paramsString
   * @property {Record<string, string | undefined>} oauthParams
   */

  /**
   * @param {string} paramsString
   * @param {string[]} supportedParams
   * @returns {ParsedCallbackParams}
   */
  #parseCallbackParams (paramsString, supportedParams) {
    const params = paramsString.split('&')
    /** @type {Record<string, string>} */
    const oauthParams = {}
    let result = ''

    for (const param of params.reverse()) {
      const entry = new URLSearchParams(param).entries().next().value

      if (!entry) {
        result = '&' + result
        continue
      }

      const [key, value] = entry

      if (supportedParams.includes(key) && !(key in oauthParams)) {
        oauthParams[key] = value
      } else {
        result = result.length === 0 ? param : param + '&' + result
      }
    }

    return {
      paramsString: result,
      oauthParams
    }
  }

  async #processCallback (oauth) {
    const { code, error, prompt } = oauth
    let timeLocal = new Date().getTime()

    /**
     * @param {string} accessToken
     * @param {string=} refreshToken
     * @param {string=} idToken
     */
    const authSuccess = (accessToken, refreshToken, idToken) => {
      timeLocal = (timeLocal + new Date().getTime()) / 2

      this.#setToken(accessToken, refreshToken, idToken, timeLocal)

      if (this.#useNonce && (this.idTokenParsed && this.idTokenParsed.nonce !== oauth.storedNonce)) {
        this.#logInfo('[KEYCLOAK] Invalid nonce, clearing token')
        this.clearToken()
        throw new Error('Invalid nonce.')
      }
    }

    if (oauth.kc_action_status) {
      this.onActionUpdate && this.onActionUpdate(oauth.kc_action_status, oauth.kc_action)
    }

    if (error) {
      if (prompt !== 'none') {
        if (oauth.error_description && oauth.error_description === 'authentication_expired') {
          await this.login(oauth.loginOptions)
        } else {
          const errorData = { error, error_description: oauth.error_description }
          this.onAuthError?.(errorData)
          throw errorData
        }
      }
      return
    } else if ((this.flow !== 'standard') && (oauth.access_token || oauth.id_token)) {
      authSuccess(oauth.access_token, undefined, oauth.id_token)
      this.onAuthSuccess?.()
    }

    if ((this.flow !== 'implicit') && code) {
      try {
        const response = await fetchAccessToken(this.endpoints.token(), code, /** @type {string} */ (this.clientId), oauth.redirectUri, oauth.pkceCodeVerifier)
        authSuccess(response.access_token, response.refresh_token, response.id_token)

        if (this.flow === 'standard') {
          this.onAuthSuccess?.()
        }

        this.#scheduleCheckIframe()
      } catch (error) {
        this.onAuthError?.()
        throw error
      }
    }
  }

  async #scheduleCheckIframe () {
    if (this.#loginIframe.enable && this.token) {
      await waitForTimeout(this.#loginIframe.interval * 1000)
      const unchanged = await this.#checkLoginIframe()

      if (unchanged) {
        await this.#scheduleCheckIframe()
      }
    }
  }

  /**
   * @param {KeycloakLoginOptions} [options]
   * @returns {Promise<void>}
   */
  login = (options) => {
    return this.#adapter.login(options)
  }

  /**
   * @param {KeycloakLoginOptions} [options]
   * @returns {Promise<string>}
   */
  createLoginUrl = async (options) => {
    const state = createUUID()
    const nonce = createUUID()
    const redirectUri = this.#adapter.redirectUri(options)
    /** @type {CallbackState} */
    const callbackState = {
      state,
      nonce,
      redirectUri,
      loginOptions: options
    }

    if (options?.prompt) {
      callbackState.prompt = options.prompt
    }

    const url = options?.action === 'register'
      ? this.endpoints.register()
      : this.endpoints.authorize()

    let scope = options?.scope || this.scope
    const scopeValues = scope ? scope.split(' ') : []

    // Ensure the 'openid' scope is always included.
    if (!scopeValues.includes('openid')) {
      scopeValues.unshift('openid')
    }

    scope = scopeValues.join(' ')

    const params = new URLSearchParams([
      ['client_id', /** @type {string} */ (this.clientId)],
      ['redirect_uri', redirectUri],
      ['state', state],
      ['response_mode', this.responseMode],
      ['response_type', this.responseType],
      ['scope', scope]
    ])

    if (this.#useNonce) {
      params.append('nonce', nonce)
    }

    if (options?.prompt) {
      params.append('prompt', options.prompt)
    }

    if (typeof options?.maxAge === 'number') {
      params.append('max_age', options.maxAge.toString())
    }

    if (options?.loginHint) {
      params.append('login_hint', options.loginHint)
    }

    if (options?.idpHint) {
      params.append('kc_idp_hint', options.idpHint)
    }

    if (options?.action && options.action !== 'register') {
      params.append('kc_action', options.action)
    }

    if (options?.locale) {
      params.append('ui_locales', options.locale)
    }

    if (options?.acr) {
      params.append('claims', buildClaimsParameter(options.acr))
    }

    if (options?.acrValues) {
      params.append('acr_values', options.acrValues)
    }

    if (this.pkceMethod) {
      try {
        const codeVerifier = generateCodeVerifier(96)
        const pkceChallenge = await generatePkceChallenge(this.pkceMethod, codeVerifier)

        callbackState.pkceCodeVerifier = codeVerifier

        params.append('code_challenge', pkceChallenge)
        params.append('code_challenge_method', this.pkceMethod)
      } catch (error) {
        throw new Error('Failed to generate PKCE challenge.', { cause: error })
      }
    }

    this.#callbackStorage.add(callbackState)

    return `${url}?${params.toString()}`
  }

  /**
   * @param {KeycloakLogoutOptions} [options]
   * @returns {Promise<void>}
   */
  logout = (options) => {
    return this.#adapter.logout(options)
  }

  /**
   * @param {KeycloakLogoutOptions} [options]
   * @returns {string}
   */
  createLogoutUrl = (options) => {
    const logoutMethod = options?.logoutMethod ?? this.logoutMethod
    const url = this.endpoints.logout()

    if (logoutMethod === 'POST') {
      return url
    }

    const params = new URLSearchParams([
      ['client_id', /** @type {string} */ (this.clientId)],
      ['post_logout_redirect_uri', this.#adapter.redirectUri(options)]
    ])

    if (this.idToken) {
      params.append('id_token_hint', this.idToken)
    }

    return `${url}?${params.toString()}`
  }

  /**
   * @param {KeycloakRegisterOptions} [options]
   * @returns {Promise<void>}
   */
  register = (options) => {
    return this.#adapter.register(options)
  }

  /**
   * @param {KeycloakRegisterOptions} [options]
   * @returns {Promise<string>}
   */
  createRegisterUrl = (options) => {
    return this.createLoginUrl({ ...options, action: 'register' })
  }

  /**
   * @param {KeycloakAccountOptions} [options]
   * @returns {string}
   */
  createAccountUrl = (options) => {
    const url = this.#getRealmUrl()

    if (!url) {
      throw new Error('Unable to create account URL, make sure the adapter is not configured using a generic OIDC provider.')
    }

    const params = new URLSearchParams([
      ['referrer', /** @type {string} */ (this.clientId)],
      ['referrer_uri', this.#adapter.redirectUri(options)]
    ])

    return `${url}/account?${params.toString()}`
  }

  /**
   * @returns {Promise<void>}
   */
  accountManagement = () => {
    return this.#adapter.accountManagement()
  }

  /**
   * @param {string} role
   * @returns {boolean}
   */
  hasRealmRole = (role) => {
    const access = this.realmAccess
    return !!access && access.roles.indexOf(role) >= 0
  }

  /**
   * @param {string} role
   * @param {string} [resource]
   * @returns {boolean}
   */
  hasResourceRole = (role, resource) => {
    if (!this.resourceAccess) {
      return false
    }

    const access = this.resourceAccess[resource || /** @type {string} */ (this.clientId)]
    return !!access && access.roles.indexOf(role) >= 0
  }

  /**
   * @returns {Promise<KeycloakProfile>}
   */
  loadUserProfile = async () => {
    const realmUrl = this.#getRealmUrl()

    if (!realmUrl) {
      throw new Error('Unable to load user profile, make sure the adapter is not configured using a generic OIDC provider.')
    }

    const url = `${realmUrl}/account`
    /** @type {KeycloakProfile} */
    const profile = await fetchJSON(url, {
      headers: [buildAuthorizationHeader(this.token)]
    })

    return (this.profile = profile)
  }

  /**
   * @returns {Promise<{}>}
   */
  loadUserInfo = async () => {
    const url = this.endpoints.userinfo()
    /** @type {{}} */
    const userInfo = await fetchJSON(url, {
      headers: [buildAuthorizationHeader(this.token)]
    })

    return (this.userInfo = userInfo)
  }

  /**
   * @param {number} [minValidity]
   * @returns {boolean}
   */
  isTokenExpired = (minValidity) => {
    if (!this.tokenParsed || (!this.refreshToken && this.flow !== 'implicit')) {
      throw new Error('Not authenticated')
    }

    if (this.timeSkew == null) {
      this.#logInfo('[KEYCLOAK] Unable to determine if token is expired as timeskew is not set')
      return true
    }

    if (typeof this.tokenParsed.exp !== 'number') {
      return false
    }

    let expiresIn = this.tokenParsed.exp - Math.ceil(new Date().getTime() / 1000) + this.timeSkew
    if (minValidity) {
      if (isNaN(minValidity)) {
        throw new Error('Invalid minValidity')
      }
      expiresIn -= minValidity
    }
    return expiresIn < 0
  }

  /**
   * @param {number} minValidity
   * @returns {Promise<boolean>}
   */
  updateToken = async (minValidity) => {
    if (!this.refreshToken) {
      throw new Error('Unable to update token, no refresh token available.')
    }

    minValidity = minValidity || 5

    if (this.#loginIframe.enable) {
      await this.#checkLoginIframe()
    }

    let refreshToken = false

    if (minValidity === -1) {
      refreshToken = true
      this.#logInfo('[KEYCLOAK] Refreshing token: forced refresh')
    } else if (!this.tokenParsed || this.isTokenExpired(minValidity)) {
      refreshToken = true
      this.#logInfo('[KEYCLOAK] Refreshing token: token expired')
    }

    if (!refreshToken) {
      return false
    }

    /** @type {PromiseWithResolvers<boolean>} */
    const { promise, resolve, reject } = Promise.withResolvers()

    this.#refreshQueue.push({ resolve, reject })

    if (this.#refreshQueue.length === 1) {
      const url = this.endpoints.token()
      let timeLocal = new Date().getTime()

      try {
        const response = await fetchRefreshToken(url, this.refreshToken, /** @type {string} */ (this.clientId))
        this.#logInfo('[KEYCLOAK] Token refreshed')

        timeLocal = (timeLocal + new Date().getTime()) / 2

        this.#setToken(response.access_token, response.refresh_token, response.id_token, timeLocal)

        this.onAuthRefreshSuccess?.()
        for (let p = this.#refreshQueue.pop(); p != null; p = this.#refreshQueue.pop()) {
          p.resolve(true)
        }
      } catch (error) {
        this.#logWarn('[KEYCLOAK] Failed to refresh token')

        if (error instanceof NetworkError && error.response.status === 400) {
          this.clearToken()
        }

        this.onAuthRefreshError?.()
        for (let p = this.#refreshQueue.pop(); p != null; p = this.#refreshQueue.pop()) {
          p.reject(error)
        }
      }
    }

    return await promise
  }

  clearToken = () => {
    if (this.token) {
      this.#setToken()
      this.onAuthLogout?.()
      if (this.loginRequired) {
        this.login()
      }
    }
  }

  /**
   * @param {string} [token]
   * @param {string} [refreshToken]
   * @param {string} [idToken]
   * @param {number} [timeLocal]
   */
  #setToken (token, refreshToken, idToken, timeLocal) {
    if (this.tokenTimeoutHandle) {
      clearTimeout(this.tokenTimeoutHandle)
      this.tokenTimeoutHandle = undefined
    }

    if (refreshToken) {
      this.refreshToken = refreshToken
      this.refreshTokenParsed = decodeToken(refreshToken)
    } else {
      delete this.refreshToken
      delete this.refreshTokenParsed
    }

    if (idToken) {
      this.idToken = idToken
      this.idTokenParsed = decodeToken(idToken)
    } else {
      delete this.idToken
      delete this.idTokenParsed
    }

    if (token) {
      this.token = token
      this.tokenParsed = decodeToken(token)
      this.sessionId = this.tokenParsed.sid
      this.authenticated = true
      this.subject = this.tokenParsed.sub
      this.realmAccess = this.tokenParsed.realm_access
      this.resourceAccess = this.tokenParsed.resource_access

      if (timeLocal) {
        this.timeSkew = Math.floor(timeLocal / 1000) - this.tokenParsed.iat
      }

      if (this.timeSkew !== null) {
        this.#logInfo('[KEYCLOAK] Estimated time difference between browser and server is ' + this.timeSkew + ' seconds')

        if (this.onTokenExpired) {
          const expiresIn = (this.tokenParsed.exp - (new Date().getTime() / 1000) + this.timeSkew) * 1000
          this.#logInfo('[KEYCLOAK] Token expires in ' + Math.round(expiresIn / 1000) + ' s')
          if (expiresIn <= 0) {
            this.onTokenExpired()
          } else {
            this.tokenTimeoutHandle = window.setTimeout(this.onTokenExpired, expiresIn)
          }
        }
      }
    } else {
      delete this.token
      delete this.tokenParsed
      delete this.subject
      delete this.realmAccess
      delete this.resourceAccess

      this.authenticated = false
    }
  }

  /**
   * @returns {string=}
   */
  #getRealmUrl () {
    if (typeof this.authServerUrl === 'undefined') {
      return
    }

    return `${stripTrailingSlash(this.authServerUrl)}/realms/${encodeURIComponent(/** @type {string} */ (this.realm))}`
  }

  /**
   * @param {Function} fn
   * @returns {(message: string) => void}
   */
  #createLogger (fn) {
    return (message) => {
      if (this.enableLogging) {
        fn.call(console, message)
      }
    }
  }
}

/**
 * @returns {string}
 */
function createUUID () {
  if (typeof crypto === 'undefined' || typeof crypto.randomUUID === 'undefined') {
    throw new Error('Web Crypto API is not available.')
  }

  return crypto.randomUUID()
}

/**
 * @param {Acr} requestedAcr
 * @returns {string}
 */
function buildClaimsParameter (requestedAcr) {
  return JSON.stringify({
    id_token: {
      acr: requestedAcr
    }
  })
}

/**
 * @param {number} len
 * @returns {string}
 */
function generateCodeVerifier (len) {
  return generateRandomString(len, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789')
}

/**
 * @param {string} pkceMethod
 * @param {string} codeVerifier
 * @returns {Promise<string>}
 */
async function generatePkceChallenge (pkceMethod, codeVerifier) {
  if (pkceMethod !== 'S256') {
    throw new TypeError(`Invalid value for 'pkceMethod', expected 'S256' but got '${pkceMethod}'.`)
  }

  // hash codeVerifier, then encode as url-safe base64 without padding
  const hashBytes = new Uint8Array(await sha256Digest(codeVerifier))
  const encodedHash = bytesToBase64(hashBytes)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=/g, '')

  return encodedHash
}

/**
 * @param {number} len
 * @param {string} alphabet
 * @returns {string}
 */
function generateRandomString (len, alphabet) {
  const randomData = generateRandomData(len)
  const chars = new Array(len)
  for (let i = 0; i < len; i++) {
    chars[i] = alphabet.charCodeAt(randomData[i] % alphabet.length)
  }
  return String.fromCharCode.apply(null, chars)
}

/**
 * @param {number} len
 * @returns {Uint8Array<ArrayBuffer>}
 */
function generateRandomData (len) {
  if (typeof crypto === 'undefined' || typeof crypto.getRandomValues === 'undefined') {
    throw new Error('Web Crypto API is not available.')
  }

  return crypto.getRandomValues(new Uint8Array(len))
}

/**
 * Function to extend existing native Promise with timeout
 *
 * @template T
 * @param {Promise<T>} promise
 * @param {number} timeout
 * @param {string} errorMessage
 * @returns {Promise<T>}
 */
function applyTimeoutToPromise (promise, timeout, errorMessage) {
  /** @type {number} */
  let timeoutHandle
  const timeoutPromise = new Promise(function (resolve, reject) {
    timeoutHandle = window.setTimeout(function () {
      reject(new Error(errorMessage || 'Promise is not settled within timeout of ' + timeout + 'ms'))
    }, timeout)
  })

  return Promise.race([promise, timeoutPromise]).finally(function () {
    clearTimeout(timeoutHandle)
  })
}

/**
 * @returns {CallbackStorage}
 */
function createCallbackStorage () {
  try {
    return new LocalStorage()
  } catch (err) {
    return new CookieStorage()
  }
}

const STORAGE_KEY_PREFIX = 'kc-callback-'

/**
 * @typedef {Object} CallbackState
 * @property {string} state
 * @property {string} nonce
 * @property {string} redirectUri
 * @property {KeycloakLoginOptions} [loginOptions]
 * @property {KeycloakLoginOptions['prompt']} [prompt]
 * @property {string} [pkceCodeVerifier]
 */

/**
 * @typedef {Object} CallbackStorage
 * @property {(state?: string) => CallbackState | null} get
 * @property {(state: CallbackState) => void} add
 */

/**
 * @implements {CallbackStorage}
 */
class LocalStorage {
  constructor () {
    globalThis.localStorage.setItem('kc-test', 'test')
    globalThis.localStorage.removeItem('kc-test')
  }

  /**
   * @param {string} [state]
   * @returns {CallbackState | null}
   */
  get (state) {
    if (!state) {
      return null
    }

    this.#clearInvalidValues()

    const key = STORAGE_KEY_PREFIX + state
    const value = globalThis.localStorage.getItem(key)

    if (value) {
      globalThis.localStorage.removeItem(key)
      return JSON.parse(value)
    }

    return null
  };

  /**
   * @param {CallbackState} state
   */
  add (state) {
    this.#clearInvalidValues()

    const key = STORAGE_KEY_PREFIX + state.state
    const value = JSON.stringify({
      ...state,
      // Set the expiry time to 1 hour from now.
      expires: Date.now() + (60 * 60 * 1000)
    })

    try {
      globalThis.localStorage.setItem(key, value)
    } catch (error) {
      // If the storage is full, clear all known values and try again.
      this.#clearAllValues()
      globalThis.localStorage.setItem(key, value)
    }
  };

  /**
   * Clears all values from local storage that are no longer valid.
   */
  #clearInvalidValues () {
    const currentTime = Date.now()

    for (const [key, value] of this.#getStoredEntries()) {
      // Attempt to parse the expiry time from the value.
      const expiry = this.#parseExpiry(value)

      // Discard the value if it is malformed or expired.
      if (expiry === null || expiry < currentTime) {
        globalThis.localStorage.removeItem(key)
      }
    }
  }

  /**
   * Clears all known values from local storage.
   */
  #clearAllValues () {
    for (const [key] of this.#getStoredEntries()) {
      globalThis.localStorage.removeItem(key)
    }
  }

  /**
   * Gets all entries stored in local storage that are known to be managed by this class.
   * @returns {[string, string][]} An array of key-value pairs.
   */
  #getStoredEntries () {
    return Object.entries(globalThis.localStorage).filter(([key]) => key.startsWith(STORAGE_KEY_PREFIX))
  }

  /**
   * Parses the expiry time from a value stored in local storage.
   * @param {string} value
   * @returns {number | null} The expiry time in milliseconds, or `null` if the value is malformed.
   */
  #parseExpiry (value) {
    let parsedValue

    // Attempt to parse the value as JSON.
    try {
      parsedValue = JSON.parse(value)
    } catch (error) {
      return null
    }

    // Attempt to extract the 'expires' property.
    if (isObject(parsedValue) && 'expires' in parsedValue && typeof parsedValue.expires === 'number') {
      return parsedValue.expires
    }

    return null
  }
}

/**
 * @implements {CallbackStorage}
 */
class CookieStorage {
  /**
   * @param {string} [state]
   * @returns {CallbackState | null}
   */
  get (state) {
    if (!state) {
      return null
    }

    const value = this.#getCookie(STORAGE_KEY_PREFIX + state)
    this.#setCookie(STORAGE_KEY_PREFIX + state, '', this.#cookieExpiration(-100))
    if (value) {
      return JSON.parse(value)
    }

    return null
  }

  /**
   * @param {CallbackState} state
   */
  add (state) {
    this.#setCookie(STORAGE_KEY_PREFIX + state.state, JSON.stringify(state), this.#cookieExpiration(60))
  }

  /**
   * @param {string} key
   * @returns
   */
  #getCookie (key) {
    const name = key + '='
    const ca = document.cookie.split(';')
    for (let i = 0; i < ca.length; i++) {
      let c = ca[i]
      while (c.charAt(0) === ' ') {
        c = c.substring(1)
      }
      if (c.indexOf(name) === 0) {
        return c.substring(name.length, c.length)
      }
    }
    return ''
  }

  /**
   * @param {string} key
   * @param {string} value
   * @param {Date} expirationDate
   */
  #setCookie (key, value, expirationDate) {
    const cookie = key + '=' + value + '; ' +
            'expires=' + expirationDate.toUTCString() + '; '
    document.cookie = cookie
  }

  /**
   * @param {number} minutes
   * @returns {Date}
   */
  #cookieExpiration (minutes) {
    const exp = new Date()
    exp.setTime(exp.getTime() + (minutes * 60 * 1000))
    return exp
  }
}

/**
 * @param {Uint8Array<ArrayBuffer>} bytes
 * @see https://developer.mozilla.org/en-US/docs/Glossary/Base64#the_unicode_problem
 */
function bytesToBase64 (bytes) {
  const binString = String.fromCodePoint(...bytes)
  return btoa(binString)
}

/**
 * @param {string} message
 * @see https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/digest#basic_example
 */
async function sha256Digest (message) {
  const encoder = new TextEncoder()
  const data = encoder.encode(message)

  if (typeof crypto === 'undefined' || typeof crypto.subtle === 'undefined') {
    throw new Error('Web Crypto API is not available.')
  }

  return await crypto.subtle.digest('SHA-256', data)
}

/**
 * @param {string} token
 * @returns {KeycloakTokenParsed}
 */
function decodeToken (token) {
  const [, payload] = token.split('.')

  if (typeof payload !== 'string') {
    throw new Error('Unable to decode token, payload not found.')
  }

  let decoded

  try {
    decoded = base64UrlDecode(payload)
  } catch (error) {
    throw new Error('Unable to decode token, payload is not a valid Base64URL value.', { cause: error })
  }

  try {
    return JSON.parse(decoded)
  } catch (error) {
    throw new Error('Unable to decode token, payload is not a valid JSON value.', { cause: error })
  }
}

/**
 * @param {string} input
 */
function base64UrlDecode (input) {
  let output = input
    .replaceAll('-', '+')
    .replaceAll('_', '/')

  switch (output.length % 4) {
    case 0:
      break
    case 2:
      output += '=='
      break
    case 3:
      output += '='
      break
    default:
      throw new Error('Input is not of the correct length.')
  }

  try {
    return b64DecodeUnicode(output)
  } catch (error) {
    return atob(output)
  }
}

/**
 * @param {string} input
 */
function b64DecodeUnicode (input) {
  return decodeURIComponent(atob(input).replace(/(.)/g, (m, p) => {
    let code = p.charCodeAt(0).toString(16).toUpperCase()

    if (code.length < 2) {
      code = '0' + code
    }

    return '%' + code
  }))
}

/**
 * Check if the input is an object that can be operated on.
 * @param {unknown} input
 */
function isObject (input) {
  return typeof input === 'object' && input !== null
}

/**
 * @typedef {Object} JsonConfig The JSON version of the adapter configuration.
 * @property {string} auth-server-url The URL of the authentication server.
 * @property {string} realm The name of the realm.
 * @property {string} resource The name of the resource, usually the client ID.
 */

/**
 * Fetch the adapter configuration from the given URL.
 * @param {string} url
 * @returns {Promise<JsonConfig>}
 */
async function fetchJsonConfig (url) {
  return await fetchJSON(url)
}

/**
 * Fetch the OpenID configuration from the given URL.
 * @param {string} url
 * @returns {Promise<OpenIdProviderMetadata>}
 */
async function fetchOpenIdConfig (url) {
  return await fetchJSON(url)
}

/**
 * @typedef {Object} AccessTokenResponse The successful token response from the authorization server, based on the {@link https://datatracker.ietf.org/doc/html/rfc6749#section-5.1 OAuth 2.0 Authorization Framework specification}.
 * @property {string} access_token The access token issued by the authorization server.
 * @property {string} token_type The type of the token issued by the authorization server.
 * @property {number} [expires_in] The lifetime in seconds of the access token.
 * @property {string} [refresh_token] The refresh token issued by the authorization server.
 * @property {string} [id_token] The ID token issued by the authorization server, if requested.
 * @property {string} [scope] The scope of the access token.
 */

/**
 * Fetch the access token from the given URL.
 * @param {string} url
 * @param {string} code
 * @param {string} clientId
 * @param {string} redirectUri
 * @param {string} [pkceCodeVerifier]
 * @returns {Promise<AccessTokenResponse>}
 */
async function fetchAccessToken (url, code, clientId, redirectUri, pkceCodeVerifier) {
  const body = new URLSearchParams([
    ['code', code],
    ['grant_type', 'authorization_code'],
    ['client_id', clientId],
    ['redirect_uri', redirectUri]
  ])

  if (pkceCodeVerifier) {
    body.append('code_verifier', pkceCodeVerifier)
  }

  return await fetchJSON(url, {
    method: 'POST',
    credentials: 'include',
    body
  })
}

/**
 * Fetch the refresh token from the given URL.
 * @param {string} url
 * @param {string} refreshToken
 * @param {string} clientId
 * @returns {Promise<AccessTokenResponse>}
 */
async function fetchRefreshToken (url, refreshToken, clientId) {
  const body = new URLSearchParams([
    ['grant_type', 'refresh_token'],
    ['refresh_token', refreshToken],
    ['client_id', clientId]
  ])

  return await fetchJSON(url, {
    method: 'POST',
    credentials: 'include',
    body
  })
}

/**
 * @template [T=unknown]
 * @param {string} url
 * @param {RequestInit} init
 * @returns {Promise<T>}
 */
async function fetchJSON (url, init = {}) {
  const headers = new Headers(init.headers)
  headers.set('Accept', CONTENT_TYPE_JSON)

  const response = await fetchWithErrorHandling(url, {
    ...init,
    headers
  })

  return await response.json()
}

/**
 * @param {string} url
 * @param {RequestInit} [init]
 * @returns {Promise<Response>}
 */
async function fetchWithErrorHandling (url, init) {
  const response = await fetch(url, init)

  if (!response.ok) {
    throw new NetworkError('Server responded with an invalid status.', { response })
  }

  return response
}

/**
 * @param {string} [token]
 * @returns {[string, string]}
 */
function buildAuthorizationHeader (token) {
  if (!token) {
    throw new Error('Unable to build authorization header, token is not set, make sure the user is authenticated.')
  }

  return ['Authorization', `bearer ${token}`]
}

/**
 * @param {string} url
 * @returns {string}
 */
function stripTrailingSlash (url) {
  return url.endsWith('/') ? url.slice(0, -1) : url
}

/**
 * @typedef {Object} NetworkErrorOptionsProperties
 * @property {Response} response
 * @typedef {ErrorOptions & NetworkErrorOptionsProperties} NetworkErrorOptions
 */

export class NetworkError extends Error {
  /** @type {Response} */
  response

  /**
   * @param {string} message
   * @param {NetworkErrorOptions} options
   */
  constructor (message, options) {
    super(message, options)
    this.response = options.response
  }
}

/**
 * @param {number} delay
 * @returns {Promise<void>}
 */
const waitForTimeout = (delay) => new Promise((resolve) => setTimeout(resolve, delay))
