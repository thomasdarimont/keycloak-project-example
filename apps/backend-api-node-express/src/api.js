/**
 * Initializes the API endpoints
 * @param app
 * @param LOG
 */
function createApiEndpoints(app, config, LOG) {

    LOG.info('Create API endpoints');

    // API routes can then access JWT claims in the request object via request.user
    app.get('/api/users/me', (req, res) => {

        let username = req.user.preferred_username;

        LOG.info(`### Accessing ${req.path}`);

        const data = {
            datetime: new Date().toISOString(),
            message: `Hello ${username}`,
            backend: 'NodeJS Express',
        };

        res.status(200).send(JSON.stringify(data));
    });

}

export default createApiEndpoints;
