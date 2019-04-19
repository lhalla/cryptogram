package main.client;

import java.awt.EventQueue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyAgreement;
import javax.swing.JFrame;

import main.client.ui.ClientGUI;
import main.client.ui.ClientLogin;
import main.common.Message;

public class Client
{

    private enum ClientState
    {
        DISCONNECTED, LOGGED_IN, CONVERSING, CLOSING
    }

    public static final int SERVER_LOGIN_OK = 0x0;
    public static final int SERVER_LOGIN_FORMAT_ERROR = 0x1;
    public static final int SERVER_LOGIN_HOST_ERROR = 0x2;
    public static final int SERVER_LOGOUT_OK = 0xE;

    public static final int PEER_CONN_JOIN_OK = 0x10;
    public static final int PEER_CONN_JOIN_FAIL = 0x11;
    public static final int PEER_BUSY = 0x12;
    public static final int PEER_KEY_GEN_OK = 0x13;
    public static final int PEER_KEY_ALGO_FAIL = 0x14;
    public static final int PEER_KEY_GEN_FAIL = 0x15;
    public static final int PEER_CONN_EXIT_OK = 0x1E;

    private static final int DH_KEYSIZE = 2048;

    private String username;
    private String serverIP;
    private int serverPort;

    private ClientState state;

    private DatagramSocket server;
    private InetAddress ip;
    private ObjectInputStream sin;
    private ObjectOutputStream sout;

    private JFrame gui;

    private final KeyPairGenerator kpairGen;
    private KeyPair kpair;
    private final KeyAgreement keyAgree;

    private Thread sendThread;

    public Client() throws NoSuchAlgorithmException
    {
        System.err.println( "Starting a new client..." );

        kpairGen = KeyPairGenerator.getInstance( "DH" );
        keyAgree = KeyAgreement.getInstance( "DH" );

        state = ClientState.DISCONNECTED;

        showLoginPrompt();
    }

    private void showLoginPrompt()
    {
        gui = new ClientLogin( this::joinServer );
    }

    private boolean joinServer( final String username, final String serverIP, final String serverPort )
    {
        try
        {
            this.username = username;
            this.serverIP = serverIP;
            this.serverPort = serverPort.matches( "[0-9]+" ) ? Integer.parseInt( serverPort ) : 8080;

            server = new DatagramSocket();
            ip = InetAddress.getByName( serverIP );

            state = ClientState.LOGGED_IN;

            gui = new ClientGUI( this.username, this.serverIP, this.serverPort );

            return true;
        }
        catch ( final Exception e )
        {
            e.printStackTrace();
            return false;
        }
    }

    private void sendMessage( final byte [ ] formattedMessage )
    {
        sendThread = new Thread( "Client send" )
        {
            @Override
            public void run()
            {
                final DatagramPacket packet = new DatagramPacket( formattedMessage, formattedMessage.length, ip,
                        serverPort );
                try
                {
                    server.send( packet );
                }
                catch ( final IOException e )
                {
                    e.printStackTrace();
                }
            }
        };

        sendThread.start();
    }

    private Message receiveMessage()
    {
        final byte [ ] messageBytes = new byte [ 1024 ];
        final DatagramPacket packet = new DatagramPacket( messageBytes, messageBytes.length );

        try
        {
            server.receive( packet );
        }
        catch ( final IOException e )
        {
            e.printStackTrace();
        }

        final byte [ ] payload = packet.getData();

        final Message message = Message.parseFormattedMessage( payload );
        return message;
    }

    public int requestConnection( final String peerName )
    {
        int errorCode = PEER_KEY_GEN_OK;

        kpairGen.initialize( DH_KEYSIZE );
        kpair = kpairGen.generateKeyPair();

        try
        {
            keyAgree.init( kpair.getPrivate() );
        }
        catch ( final InvalidKeyException e )
        {
            errorCode = PEER_KEY_GEN_FAIL;
        }

        if ( errorCode == PEER_KEY_GEN_OK )
        {
            try
            {
                sout.writeObject( new Message( Message.DH_KEY_EXCHANGE1, peerName.getBytes() ) );
                sout.flush();

                final Object response = sin.readObject();

                if ( response instanceof Message )
                {

                }
            }
            catch ( final IOException e )
            {
                e.printStackTrace();
            }
            catch ( final ClassNotFoundException e )
            {
                e.printStackTrace();
            }
            System.err.println( "Send address: " + server.getLocalAddress().toString() );
            System.err.println( "Send key: " + kpair.getPublic().getEncoded() );
        }

        return errorCode;
    }

    /**
     * Launch the application.
     */
    public static void main( final String [ ] args )
    {
        EventQueue.invokeLater( () ->
        {
            try
            {
                new Client();
            }
            catch ( final Exception e )
            {
                e.printStackTrace();
            }
        } );
    }
}
