const {keycloak_host, test_realm} = Cypress.env();

import users from '../../fixtures/users.json'
import i18nMsg from '../../fixtures/messages.json'
import {loginUser, visitClient} from '../../utils/keycloakUtils'

let browserLang = (navigator.language || 'en-EN').split("-")[0];
let msg = (i18nMsg as any)[browserLang];
let accountClientId = 'account-console';

context('Login...', () => {

    Cypress.on('uncaught:exception', (err, runnable) => {
        return false;
    });

    it('with known Username and Password, then Logout should pass', () => {

        visitClient(accountClientId)
        cy.get('#kc-login').click()

        loginUser(users.tester)

        cy.get('.pf-v5-c-menu-toggle__text').click()
        cy.get('.pf-v5-c-menu__item').invoke("text").should('eq', msg.signOut)
        cy.get('.pf-v5-c-menu__item').click()
    });

    it('with unknown Username should fail', () => {

        visitClient(accountClientId)

        cy.get('#kc-login').click()

        cy.get('#username').type(users.unknown.username)
        cy.get('input#kc-login').click()

        cy.get('#input-error-username').invoke("text").should(t => expect(t.trim()).equal(msg.errorInvalidUsernameOrEmail))
    });

    it('with known Username but invalid Password should fail', () => {

        visitClient(accountClientId)

        cy.get('#kc-login').click()

        loginUser(users.testerInvalidPass)

        cy.get('#input-error-password').invoke("text").should(t => expect(t.trim()).equal(msg.errorInvalidPassword))
    });

})
  