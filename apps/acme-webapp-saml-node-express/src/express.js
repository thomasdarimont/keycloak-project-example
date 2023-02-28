import express from "express";
import session from "express-session";
import passport from "passport";
import {Strategy as SamlStrategy} from "passport-saml";
import {default as bodyParser} from "body-parser";

function createExpressApp(config, LOG) {

    LOG.info("Create express app");

    const app = express();

    app.use(bodyParser.urlencoded({extended: true}));

    configureSession(app, config);
    configureSaml(app, config);
    configureTemplateEngine(app, config);
    configureRoutes(app, config);

    return app;
}

function configureSession(app, config) {
    app.use(session({
        secret: config.SESSION_SECRET,
        resave: false,
        saveUninitialized: true,
        cookie: {secure: true}
    }));
}

let samlStrategy;

function configureSaml(app, config) {

    app.use(passport.initialize());
    app.use(passport.session());

    passport.serializeUser(function (user, done) {
        done(null, user);
    });
    passport.deserializeUser(function (user, done) {
        done(null, user);
    });

    samlStrategy = new SamlStrategy(
        // See Config parameter details: https://www.npmjs.com/package/passport-saml
        // See also https://github.com/node-saml/passport-saml
        {
            entryPoint: config.IDP_ISSUER + "/protocol/saml",
            issuer: config.SP_ISSUER,
            host: config.HOSTNAME,
            protocol: "https://",
            signatureAlgorithm: "sha256",
            privateKey: config.SAML_SP_KEY,
            cert: config.SAML_IDP_CERT,
            passReqToCallback: true,
            logoutUrl: config.IDP_ISSUER + "/protocol/saml",
        },
        function (request, profile, done) {
            let user = {
                username: profile["nameID"],
                firstname: profile["http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"],
                lastname: profile["http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"],
                email: profile["http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress"],
                // e.g. if you added a Group claim
                group: profile["http://schemas.xmlsoap.org/claims/Group"],
            };
            return done(null, user);
        }
    );
    passport.use(samlStrategy);
}

function configureTemplateEngine(app, config) {

    // set the view engine to ejs
    app.set('view engine', 'ejs');
}

function configureRoutes(app, config) {

    let ensureAuthenticated = function (req, res, next) {
        if (!req.isAuthenticated()) {
            res.redirect('/login')
            return;
        }
        return next();
    }

    app.get('/login',
        passport.authenticate('saml', {failureRedirect: '/', failureFlash: true}),
        function (req, res) {
            res.redirect('/app');
        }
    );

    app.post('/saml',
        passport.authenticate('saml', {
            failureRedirect: '/error',
            failureFlash: true
        }),
        (req, res) => {

            // success redirection to index
            return res.redirect('/');
        }
    );

    app.post('/saml/consume',
        passport.authenticate('saml', {
            failureRedirect: '/error',
            failureFlash: true
        }),
        (req, res) => {

            // success redirection to /app
            return res.redirect('/app');
        }
    );

    app.get('/logout',
        ensureAuthenticated,
        (req, res, next) => {

            if (req.user != null) {
                return samlStrategy.logout(req, (err, uri) => {
                    req.logout(err => {
                        if (err) {
                            LOG.warn("Could not logout: " + err);
                            return next(err);
                        }
                        res.redirect('/');
                    });
                });
            }

            return res.redirect('/');
        });

    app.get('/error',
        function (req, res) {
            res.render('pages/error');
        }
    );

    app.get('/',
        function (req, res) {
            res.render('pages/index');
        }
    );

    app.get('/app',
        ensureAuthenticated,
        function (req, res) {
            let user = req.user;
            res.render('pages/app', {
                user
            });
        }
    );
}

export default createExpressApp;
