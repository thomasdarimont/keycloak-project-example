/**
 * Initializes the API endpoints
 * @param app
 * @param LOG
 */
function initApi(app, LOG, config) {

    // API routes can then access JWT claims in the request object via request.user
    app.get('/api/users/me', (req, res) => {

        let username = req.user.preferred_username;

        LOG.info(`### Accessing ${req.path}`);

        const data = {
            message: `Hello ${username}`,
        };

        res.status(200).send(JSON.stringify(data));
    });

}

export default initApi;
