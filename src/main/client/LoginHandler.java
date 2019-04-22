package main.client;

/**
 * Provides a method for logging into a server.
 *
 * @author Lauri Halla-aho
 */
@FunctionalInterface
public interface LoginHandler
{
    /**
     * Login to the server with the specified credentials.
     *
     * @param username   - client's username
     * @param serverIP   - server's IP address
     * @param serverPort - server's port number
     * @return error code corresponding to the result
     */
    int apply( String username, String serverIP, String serverPort );
}
