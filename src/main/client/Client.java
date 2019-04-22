package main.client;

import java.awt.EventQueue;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import main.client.ui.ClientGUI;
import main.client.ui.ClientLogin;
import main.common.Message;

/**
 * Handles the connectivity and communication between the clients and the server.
 *
 * @author Lauri Halla-aho
 */
public class Client implements Runnable, Crypter
{
    private static final int DH_KEYSIZE = 2048;

    /**
     * Used key exchange protocol.
     */
    private static final String KX_PROTOCOL = "DH";

    /**
     * Used encryption algorithm.
     */
    private static final String KEY_TYPE = "AES";

    /**
     * Used cipher parameters: AES encryption, CBC mode, PKCS5 padding.
     */
    private static final String CIPHER_TYPE = "AES/CBC/PKCS5Padding";

    private String username;
    private String peername;

    private DatagramSocket socket;
    private InetAddress ip;
    private int serverPort;

    private final KeyPairGenerator kpairGen;
    private KeyPair kpair;
    private final KeyAgreement keyAgree;
    private SecretKeySpec aesKey;

    private Thread messageSender;
    private Thread messageReceiver;
    private Thread runner;
    private boolean running = false;

    /**
     * Reference to the chat window GUI.
     */
    private ClientGUI gui;

    /**
     * Starts a new CryptoGram client backend and displays the login screen.
     *
     * @throws NoSuchAlgorithmException if selected key exchange protocol does not exist
     */
    public Client() throws NoSuchAlgorithmException
    {
        kpairGen = KeyPairGenerator.getInstance( KX_PROTOCOL );
        keyAgree = KeyAgreement.getInstance( KX_PROTOCOL );

        showLoginPrompt();
    }

    @Override
    public void setPeername( final String peername )
    {
        this.peername = peername;
    }

    /**
     * Instantiates a new login prompt.
     */
    private void showLoginPrompt()
    {
        new ClientLogin( this::joinServer );
    }

    /**
     * Join the specified server.
     *
     * @param username   - selected username
     * @param serverIP   - specified server IP address
     * @param serverPort - specified server port
     * @return <code>true</code> if parameters were valid
     */
    private boolean joinServer( final String username, final String serverIP, final String serverPort )
    {
        try
        {
            this.username = username;
            this.serverPort = serverPort.matches( "[0-9]+" ) ? Integer.parseInt( serverPort ) : 8080;

            socket = new DatagramSocket();
            ip = InetAddress.getByName( serverIP );

            gui = new ClientGUI( this, username, serverIP, this.serverPort );

            runner = new Thread( this, "CryptoGram Client" );
            runner.start();

            return true;
        }
        catch ( final SocketException | UnknownHostException e )
        {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void sendMessage( final byte [ ] formattedMessage )
    {
        messageSender = new Thread( "Message Sender" )
        {
            @Override
            public void run()
            {
                final DatagramPacket packet = new DatagramPacket( formattedMessage, formattedMessage.length, ip,
                        serverPort );
                try
                {
                    socket.send( packet );
                }
                catch ( final IOException e )
                {
                    e.printStackTrace();
                }
            }
        };

        messageSender.start();
    }

    /**
     * Attempts to receive a message from the server socket.
     *
     * @return a received message
     */
    private Message receiveMessage()
    {
        final byte [ ] messageBytes = new byte [ 1024 ];
        final DatagramPacket packet = new DatagramPacket( messageBytes, messageBytes.length );

        try
        {
            socket.receive( packet );
        }
        catch ( final IOException e )
        {
            e.printStackTrace();
        }

        final byte [ ] payload = new byte [ packet.getLength() ];

        System.arraycopy( packet.getData(), packet.getOffset(), payload, 0, packet.getLength() );

        final Message message = Message.parseFormattedMessage( payload );
        return message;
    }

    /**
     * Listens for incoming messages.
     */
    private void listen()
    {
        messageReceiver = new Thread()
        {
            @Override
            public void run()
            {
                while ( running )
                {
                    final Message message = receiveMessage();

                    /* Response from the server with the given username or its variant if it was already taken. */
                    if ( message.getType() == Message.LOGIN && message.getUsername() != null )
                    {
                        if ( message.getUsername().equals( username ) )
                        {
                            gui.log( "Successfully connected to the lobby!" );
                            gui.setTitle( "CryptoGram Client - CONNECTED (" + username + ")" );
                        }
                        else if ( message.getUsername().substring( 0, username.length() ).equals( username ) )
                        {
                            gui.log( "Selected username was already taken, logged in as " + message.getUsername() );
                            gui.setTitle( "CryptoGram Client - CONNECTED (" + message.getUsername() + ")" );
                            gui.setUsername( message.getUsername() );
                        }
                        else
                        {
                            gui.log( "Connection failed! Please restart the client." );
                        }
                    }
                    /* A keep-alive message from the server to determine whether this client has disconnected. */
                    else if ( message.getType() == Message.KEEP_ALIVE )
                    {
                        sendMessage( message.getFormattedMessage() );
                    }
                    /* A general response from the server that is displayed in the chat history. */
                    else if ( message.getType() == Message.MSG_CHAT_CMD )
                    {
                        gui.log( message.getMessage() );
                    }
                    /* A notification from the server about the conversation partner having left the chat. */
                    else if ( message.getType() == Message.PEER_LEFT )
                    {
                        gui.log( message.getMessage() );
                        peername = null;
                    }
                    /* A notification from the server about another client having started a chat with this client. */
                    else if ( message.getType() == Message.NEW_PEER )
                    {
                        gui.log( message.getMessage() );
                        peername = message.getUsername();
                    }
                    /*
                     * A confirmation from the server that the selected other client is available for chat. Continue by
                     * sending a secret for Diffie-Hellman key exchange.
                     */
                    else if ( message.getType() == Message.PEER_AVAILABLE )
                    {
                        gui.log( "Peer available" );
                        final byte [ ] secret = generateSecret();
                        sendMessage( new Message( Message.DHKX_REQ, secret ).getFormattedMessage() );
                        gui.log( "Secret sent." );
                    }
                    /**
                     * A secret for Diffie-Hellman key exchange sent by the new conversation partner. Replied to with
                     * the local secret and used to derive the shared secret and AES encryption key.
                     */
                    else if ( message.getType() == Message.DHKX_REQ )
                    {
                        gui.log( "Secret received." );
                        final byte [ ] secret = generateSecret( message.getPayload() );
                        sendMessage( new Message( Message.DHKX_RES, secret ).getFormattedMessage() );
                        gui.log( "Secret sent." );
                        gui.log( "Encryption keys generated." );
                    }
                    /**
                     * A secret from the new conversation partner, sent in response to the initial DHKX_REQ message.
                     * Used to derive the shared secret and AES encryption key.
                     */
                    else if ( message.getType() == Message.DHKX_RES )
                    {
                        gui.log( "Secret received." );
                        receiveSecret( message.getPayload() );
                        gui.log( "Encryption keys generated." );
                    }
                    /**
                     * A notification from the server informing that the selected client is in another conversation.
                     */
                    else if ( message.getType() == Message.PEER_BUSY )
                    {
                        gui.log( peername + " is busy." );
                        peername = null;
                    }
                    /**
                     * An encrypted chat message received from the conversation partner. Followed by the cipher
                     * parameters used to initialise the decryption cipher.
                     */
                    else if ( message.getType() == Message.MSG_CHAT )
                    {
                        final Message params = receiveMessage();

                        if ( params.getType() == Message.MSG_PARAMS )
                        {
                            final byte [ ] plainText = decrypt( message.getPayload(), params.getPayload() );
                            gui.log( peername + ": " + new String( plainText ) );
                        }
                    }
                }
            }
        };

        messageReceiver.start();
    }

    @Override
    public void close()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                running = false;

                synchronized ( socket )
                {
                    socket.close();
                }
            }
        }.start();
    }

    /**
     * Generates a local secret for the initiating party.
     *
     * @return the initiating party's encoded public key
     */
    private byte [ ] generateSecret()
    {
        try
        {
            kpairGen.initialize( DH_KEYSIZE );
            kpair = kpairGen.generateKeyPair();
            keyAgree.init( kpair.getPrivate() );

            return kpair.getPublic().getEncoded();
        }
        catch ( final InvalidKeyException e )
        {
            e.printStackTrace();
            return new byte [ 0 ];
        }
    }

    /**
     * Derives the AES key from the secret received from the new peer.
     *
     * @param peerSecret - secret received from the new peer
     */
    public void receiveSecret( final byte [ ] peerSecret )
    {
        try
        {
            final KeyFactory keyFac = KeyFactory.getInstance( KX_PROTOCOL );
            final X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec( peerSecret );

            final PublicKey peerPubKey = keyFac.generatePublic( x509KeySpec );

            keyAgree.doPhase( peerPubKey, true );

            final byte [ ] sharedSecret = keyAgree.generateSecret();

            aesKey = new SecretKeySpec( sharedSecret, 0, 16, KEY_TYPE );
        }
        catch ( final NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException
                | IllegalStateException e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Generates and returns a local secret and uses the secret of the initiating party to derive the AES key.
     *
     * @param peerSecret - secret received from the new peer
     * @return encoded public key
     */
    public byte [ ] generateSecret( final byte [ ] peerSecret )
    {
        try
        {
            final KeyFactory keyFac = KeyFactory.getInstance( KX_PROTOCOL );
            final X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec( peerSecret );
            final PublicKey peerPubKey = keyFac.generatePublic( x509KeySpec );
            final DHParameterSpec dhParams = ( (DHPublicKey) peerPubKey ).getParams();

            kpairGen.initialize( dhParams );
            kpair = kpairGen.generateKeyPair();
            keyAgree.init( kpair.getPrivate() );
            keyAgree.doPhase( peerPubKey, true );

            final byte [ ] sharedSecret = keyAgree.generateSecret();

            aesKey = new SecretKeySpec( sharedSecret, 0, 16, KEY_TYPE );

            return kpair.getPublic().getEncoded();
        }
        catch ( final NoSuchAlgorithmException | InvalidKeySpecException | InvalidAlgorithmParameterException
                | InvalidKeyException e )
        {
            e.printStackTrace();
            return new byte [ 0 ];
        }
    }

    @Override
    public byte [ ] [ ] encrypt( final byte [ ] plainText )
    {
        try
        {
            final Cipher encipher = Cipher.getInstance( CIPHER_TYPE );
            encipher.init( Cipher.ENCRYPT_MODE, aesKey );

            final byte [ ] cipherText = encipher.doFinal( plainText );
            final byte [ ] params = encipher.getParameters().getEncoded();

            return new byte [ ] [ ] { cipherText, params };
        }
        catch ( NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException
                | BadPaddingException | IOException e )
        {
            e.printStackTrace();
            return new byte [ 0 ] [ 0 ];
        }
    }

    @Override
    public byte [ ] decrypt( final byte [ ] cipherText, final byte [ ] params )
    {
        try
        {
            final AlgorithmParameters aesParams = AlgorithmParameters.getInstance( KEY_TYPE );
            aesParams.init( params );

            final Cipher decipher = Cipher.getInstance( CIPHER_TYPE );
            decipher.init( Cipher.DECRYPT_MODE, aesKey, aesParams );

            return decipher.doFinal( cipherText );
        }
        catch ( NoSuchAlgorithmException | IOException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e )
        {
            e.printStackTrace();
            return new byte [ 0 ];
        }

    }

    @Override
    public void run()
    {
        running = true;
        listen();
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
            catch ( final NoSuchAlgorithmException e )
            {
                e.printStackTrace();
            }
        } );
    }
}
