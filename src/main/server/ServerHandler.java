package main.server;

/**
 * A class providing the functionality to start a server.
 *
 * @author Lauri Halla-aho
 */
public class ServerHandler
{
    private final int port;
    private final Server server;

    /**
     * Constructs a new server handler.
     * 
     * @param port - port of the server to be started
     */
    public ServerHandler( final int port )
    {
        this.port = port;
        server = new Server( port );
    }

    public static void main( final String [ ] args )
    {
        if ( args.length < 1 )
        {
            System.out.println( "Usage: java -jar CryptoGramServer.jar [port]" );
            return;
        }

        final int port = args[ 0 ].matches( "[0-9]+" ) ? Integer.parseInt( args[ 0 ] ) : 8080;
        new ServerHandler( port );
    }
}
