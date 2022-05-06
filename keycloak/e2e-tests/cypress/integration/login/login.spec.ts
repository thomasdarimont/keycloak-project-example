const {keycloak_host, test_realm} = Cypress.env();

import users from '../../fixtures/users.json'
import i18nMsg from '../../fixtures/messages.json'
import {loginUser, visitClient} from '../../utils/keycloakUtils'

let browserLang = (navigator.language || 'en-EN').split("-")[0];
let msg = (i18nMsg as any)[browserLang];

context('Login...', () => {

    it('with known Username and Password, then Logout should pass', () => {

        visitClient('account')
        cy.get('#landingSignInButton').click()

        loginUser(users.tester)

        cy.get('#landingSignOutButton').invoke("text").should('eq', msg.signOut)
        cy.get('#landingSignOutButton').click()

        // confirm logout
        cy.get('#kc-logout').click()

        cy.get('#landingSignInButton').invoke("text").should('eq', msg.signIn)
    });

    it('with unknown Username should fail', () => {

        visitClient('account')

        cy.get('#landingSignInButton').click()

        cy.get('#username').type(users.unknown.username)
        cy.get('input#kc-login').click()

        cy.get('#input-error-username').invoke("text").should(t => expect(t.trim()).equal(msg.errorInvalidUsernameOrEmail))
    });

    it('with known Username but invalid Password should fail', () => {

        visitClient('account')

        cy.get('#landingSignInButton').click()

        loginUser(users.testerInvalidPass)

        cy.get('#input-error-password').invoke("text").should(t => expect(t.trim()).equal(msg.errorInvalidPassword))
    });

})
  