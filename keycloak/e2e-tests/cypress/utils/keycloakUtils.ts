const {keycloak_host, test_realm} = Cypress.env();

export function visitClient(clientId: string) {
    cy.visit(`${keycloak_host}/auth/realms/${test_realm}/clients/${clientId}/redirect`)
}

export function loginUser(user: any) {

    cy.get('#username').type(user.username)
    cy.get('#kc-login').click()

    cy.get('#password').type(user.password)
    cy.get('#kc-login').click()
}
