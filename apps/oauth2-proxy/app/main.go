package main

import (
	"fmt"
	"html/template"
	"net/http"
	"strings"
)

func greet(w http.ResponseWriter, r *http.Request) {

	headers := make(map[string]string)
	for name, values := range r.Header {
		if len(values) > 1 {
			headers[name] = strings.Join(values, ", ")
		} else {
			headers[name] = values[0]
		}
	}

	authorization := r.Header.Get("Authorization")
	idToken := strings.Split(authorization, "Bearer ")[1]

	data := &struct {
		Username  string
		Email     string
		Roles     string
		Headers   map[string]string
		LogoutUri string
	}{
		Username: r.Header.Get("X-Forwarded-Preferred-Username"),
		Email:    r.Header.Get("X-Forwarded-Email"),
		Roles:    r.Header.Get("X-Forwarded-Groups"),
		Headers:  headers,
		// oauth2-proxy logout URL uses Keycloaks end_session endpoint
		LogoutUri: "/oauth2/sign_out?rd=https%3A%2F%2Fid.acme.test%3A8443%2Fauth%2Frealms%2Facme-internal%2Fprotocol%2Fopenid-connect%2Flogout%3Fclient_id%3Dapp-oauth2-proxy%26post_logout_redirect_uri%3Dhttps%3A%2F%2Fapps.acme.test%3A6443%2F%26id_token_hint%3D" + idToken,
	}

	htmlTemplate := `
	<h1>app-oauth2-proxy</h1>
	<h2>Greeting</h2>
	<div>
	Hello {{.Username}} <a href="{{.LogoutUri}}">Logout</a>
	</div>
    <div>
		<ul>
        <li>Email: {{.Email}}</li>
        <li>Roles: {{.Roles}}</li>
		</ul>
    </div>
	<h2>Headers</h2>
    <ul>
        {{range $name, $value := .Headers}}
            <li><strong>{{$name}}</strong>: {{$value}}</li>
        {{end}}
    </ul>
	`

	t, _ := template.New("greet").Parse(htmlTemplate)
	t.Execute(w, data)
}

func main() {
	http.HandleFunc("/", greet)
	addr := ":6080"
	fmt.Printf("Listening on http://%s/\n", addr)
	fmt.Printf("External address https://apps.acme.test:6443/\n", addr)
	http.ListenAndServe(addr, nil)
}
