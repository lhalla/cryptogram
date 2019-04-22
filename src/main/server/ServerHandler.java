package main.server;

/**
 * A class providing the functionality to start a server.
 *
 * @author Lauri Halla-aho
 */
public class ServerHandler
{
    /**
     * Constructs a new server handler.
     *
     * @param port - port of the server to be started
     */
    public ServerHandler( final int port )
    {
        new Server( port );
    }

    public static void main( final String [ ] args )
    {
        int port;
        try
        {
            port = Integer.parseInt( args[ 0 ] );
        }
        catch ( final Exception e )
        {
            System.out.println( "Usage: java -jar CryptoGramServer.jar [port]" );
            System.out.println( "Starting CryptoGram Server on the default port 1948." );
            port = 1984;
        }

        new ServerHandler( port );
    }
}
