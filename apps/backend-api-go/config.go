package main

type config struct {
	// TODO add support for defaults and end variables
	Addr        string
	TlsCertFile string
	TlsKeyFile  string

	IssuerUri string
	JwksUri   string

	CorsAllowedOrigins string
}

func CreateConfig() *config {

	conf := &config{}
	conf.Addr = ":4843"

	conf.TlsCertFile = "../../config/stage/dev/tls/acme.test+1.pem"
	conf.TlsKeyFile = "../../config/stage/dev/tls/acme.test+1-key.pem"

	conf.IssuerUri = "https://id.acme.test:8443/auth/realms/acme-internal"
	conf.JwksUri = "https://id.acme.test:8443/auth/realms/acme-internal/protocol/openid-connect/certs"

	conf.CorsAllowedOrigins = "https://apps.acme.test:4443"

	return conf
}
