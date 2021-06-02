const {keycloak_host, test_realm} = Cypress.env();

import users from '../../fixtures/users.json'
import {loginUser, visitClient} from '../../utils/keycloakUtils'

context('Login...', () => {

    it('with known Username and Password, then Logout should pass', () => {

        visitClient('account')
        cy.get('#landingSignInButton').click()

        loginUser(users.tester)

        cy.get('#landingSignOutButton').invoke("text").should('eq', "Sign Out")
        cy.get('#landingSignOutButton').click()
        cy.get('#landingSignInButton').invoke("text").should('eq', "Sign In")
    });

    it('with unknown Username should fail', () => {

        visitClient('account')

        cy.get('#landingSignInButton').click()

        cy.get('#username').type(users.unknown.username)
        cy.get('input#kc-login').click()

        cy.get('#input-error-username').invoke("text").should(t => expect(t.trim()).equal('Invalid username or email.'))
    });

    it('with known Username but invalid Password should fail', () => {

        visitClient('account')

        cy.get('#landingSignInButton').click()

        loginUser(users.testerInvalidPass)

        cy.get('#input-error-password').invoke("text").should(t => expect(t.trim()).equal('Invalid password.'))
    });

})
  