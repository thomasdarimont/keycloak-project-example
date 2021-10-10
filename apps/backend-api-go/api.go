package main

import (
	"encoding/json"
	"fmt"
	"github.com/form3tech-oss/jwt-go"
	"net/http"
	"time"
)

type Api struct {
}

func (a *Api) Init(app *App) {

	app.Router.Handle("/api/users/me", http.HandlerFunc(a.handleMeInfo))
}

const (
	CLAIM_PREFERRED_USERNAME = "preferred_username"
)

type MeInfo struct {
	Message  string `json:"message"`
	Backend  string `json:"backend"`
	Datetime string `json:"datetime"`
}

func (a *Api) handleMeInfo(w http.ResponseWriter, req *http.Request) {

	token := a.getToken(req)
	username := a.getTokenClaim(CLAIM_PREFERRED_USERNAME, token)

	data := &MeInfo{
		Message:  "Hello " + username,
		Backend:  "Golang",
		Datetime: time.Now().String(),
	}

	a.respondWithJSON(w, http.StatusOK, data)
}

func (a *Api) getTokenClaim(claim string, token *jwt.Token) string {
	claims := a.getClaims(token)
	return fmt.Sprintf("%v", claims[claim])
}

func (a *Api) getClaims(token *jwt.Token) jwt.MapClaims {
	return (token.Claims).(jwt.MapClaims)
}

func (a *Api) getToken(req *http.Request) *jwt.Token {
	return (req.Context().Value("user")).(*jwt.Token)
}

func (a *Api) respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {

	response, _ := json.Marshal(payload)

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	_, err := w.Write(response)
	if err != nil {
		return
	}
}
