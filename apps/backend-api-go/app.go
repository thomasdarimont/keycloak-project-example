package main

import (
	"encoding/json"
	"errors"
	jwtmiddleware "github.com/auth0/go-jwt-middleware"
	"github.com/codegangsta/negroni"
	"github.com/form3tech-oss/jwt-go"
	"github.com/gorilla/mux"
	"github.com/rs/cors"
	"log"
	"net/http"
)

type App struct {
	Config *config
	Router *mux.Router
	Api    *Api
}

func (a *App) Init() {

	router := mux.NewRouter()
	a.Router = router

	a.Api = &Api{}
	a.Api.Init(a)
}

func (a *App) Run() {

	conf := a.Config

	n := negroni.New(
		negroni.NewLogger(),
		negroni.NewRecovery(),

		a.newCors(conf),
		negroni.HandlerFunc(a.createJwtMiddleware().HandlerWithNext),
	)

	n.UseHandler(a.Router)

	log.Printf("Server running %s\n", conf.Addr)
	err := http.ListenAndServeTLS(conf.Addr, conf.TlsCertFile, conf.TlsKeyFile, n)
	if err != nil {
		log.Fatal("ListenAndServe: ", err)
	}
}

func (a *App) newCors(conf *config) *cors.Cors {
	c := cors.New(cors.Options{
		AllowedOrigins:   []string{conf.CorsAllowedOrigins},
		AllowedMethods:   []string{http.MethodGet},
		AllowedHeaders:   []string{"Authorization", "Content-Type"},
		AllowCredentials: true,
		// Enable Debugging for testing, consider disabling in production
		Debug: true,
	})
	return c
}

type Jwks struct {
	Keys []JSONWebKeys `json:"keys"`
}

type JSONWebKeys struct {
	Kty string   `json:"kty"`
	Kid string   `json:"kid"`
	Use string   `json:"use"`
	N   string   `json:"n"`
	E   string   `json:"e"`
	X5c []string `json:"x5c"`
}

func (a *App) getJwkCertForToken(token *jwt.Token) (string, error) {
	kid := token.Header["kid"]
	return a.lookupJwkForKid(kid)
}

func (a *App) lookupJwkForKid(kid interface{}) (string, error) {

	// TODO add support for caching JWKS

	cert := ""
	resp, err := http.Get(a.Config.JwksUri)

	if err != nil {
		return cert, err
	}
	defer resp.Body.Close()

	var jwks = Jwks{}
	err = json.NewDecoder(resp.Body).Decode(&jwks)

	if err != nil {
		return cert, err
	}

	for k, _ := range jwks.Keys {
		if kid == jwks.Keys[k].Kid {
			cert = "-----BEGIN CERTIFICATE-----\n" + jwks.Keys[k].X5c[0] + "\n-----END CERTIFICATE-----"
		}
	}

	if cert == "" {
		err := errors.New("Unable to find appropriate key.")
		return cert, err
	}

	return cert, nil
}

func (a *App) createJwtMiddleware() *jwtmiddleware.JWTMiddleware {
	jwtMiddleware := jwtmiddleware.New(jwtmiddleware.Options{
		ValidationKeyGetter: func(token *jwt.Token) (interface{}, error) {

			// Verify 'aud' claim
			//aud := "${apiIdentifier}"
			//checkAud := token.Claims.(jwt.MapClaims).VerifyAudience(aud, false)
			//if !checkAud {
			//	return token, errors.New("Invalid audience.")
			//}

			// Verify 'iss' claim
			iss := a.Config.IssuerUri
			claims := token.Claims.(jwt.MapClaims)
			checkIss := claims.VerifyIssuer(iss, true)
			if !checkIss {
				return token, errors.New("Invalid issuer.")
			}

			// check iat, exp, nbf
			err := claims.Valid()
			if err != nil {
				return nil, err
			}

			cert, err := a.getJwkCertForToken(token)
			if err != nil {
				return nil, err
			}

			result, _ := jwt.ParseRSAPublicKeyFromPEM([]byte(cert))
			return result, nil
		},
		SigningMethod: jwt.SigningMethodRS256,
	})

	return jwtMiddleware
}
