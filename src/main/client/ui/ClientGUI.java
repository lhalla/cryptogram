package main.client.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import main.client.Crypter;
import main.common.Message;

/**
 * Displays the post-login GUI for a CryptoGram client with a chat history.
 *
 * @author Lauri Halla-aho
 */
public class ClientGUI extends JFrame
{

    private static final long serialVersionUID = 6962717251338732138L;

    private final Crypter crypter;

    private final JPanel content;

    private JTextArea chatHistory;

    private JTextField chatMessage;

    private String username;

    private boolean connected = false;

    /**
     * Constructs the chat window view for CryptoGram.
     *
     * @param crypter    - the interface used to communicate with a peer
     * @param username   - the selected username
     * @param serverIP   - the target server's IP address
     * @param serverPort - the target server's port
     */
    public ClientGUI( final Crypter crypter, final String username, final String serverIP, final int serverPort )
    {
        setResizable( false );
        setUsername( username );
        this.crypter = crypter;
        content = new JPanel();

        createWindow();
        log( "For a list of available chat commands, type /help." );
        log( String.format( "Attempting to connect to %s:%d as %s...", serverIP, serverPort, username ) );
        send( new Message( Message.LOGIN, username ) );

        new Timer( "Connection check" ).schedule( new TimerTask()
        {

            @Override
            public void run()
            {
                if ( !connected )
                {
                    log( "Connection failed! Please restart the client." );
                }
            }

        }, 5000 );
    }

    /**
     * Sets the username for this client.
     *
     * @param username - this client's username
     */
    public void setUsername( final String username )
    {
        this.username = username;
    }

    /**
     * Sets the connectivity status of this client to the specified one.
     *
     * @param connected - <code>true</code> if this client is connected to a server
     */
    public void setConnected( final boolean connected )
    {
        this.connected = connected;
    }

    /**
     * Draws the chat window.
     */
    private void createWindow()
    {
        setDefaultLookAndFeelDecorated( true );

        setTitle( "CryptoGram Client - DISCONNECTED" );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setSize( 800, 500 );
        content.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
        setContentPane( content );

        final GridBagLayout layout = new GridBagLayout();
        layout.columnWidths = new int [ ] { 30, 665, 75, 30 };
        layout.rowHeights = new int [ ] { 50, 400, 40, 10 };
        layout.columnWeights = new double [ ] { 1.0, Double.MIN_VALUE };
        layout.rowWeights = new double [ ] { 1.0, Double.MIN_VALUE };
        content.setLayout( layout );

        chatHistory = new JTextArea();
        chatHistory.setEditable( false );
        final JScrollPane scrollChatHistory = new JScrollPane( chatHistory );
        final GridBagConstraints scrollChatHistoryConstraints = new GridBagConstraints();
        scrollChatHistoryConstraints.insets = new Insets( 0, 0, 5, 5 );
        scrollChatHistoryConstraints.fill = GridBagConstraints.BOTH;
        scrollChatHistoryConstraints.gridx = 1;
        scrollChatHistoryConstraints.gridy = 1;
        scrollChatHistoryConstraints.gridwidth = 2;
        content.add( scrollChatHistory, scrollChatHistoryConstraints );

        chatMessage = new JTextField();
        chatMessage.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyPressed( final KeyEvent arg0 )
            {
                if ( arg0.getKeyCode() == KeyEvent.VK_ENTER )
                {
                    send( chatMessage.getText() );
                }
            }
        } );
        final GridBagConstraints chatMessageConstraints = new GridBagConstraints();
        chatMessageConstraints.insets = new Insets( 5, 5, 5, 5 );
        chatMessageConstraints.fill = GridBagConstraints.BOTH;
        chatMessageConstraints.gridx = 1;
        chatMessageConstraints.gridy = 2;
        content.add( chatMessage, chatMessageConstraints );

        final JButton sendButton = new JButton( "Send" );
        sendButton.addActionListener( click -> send( chatMessage.getText() ) );
        final GridBagConstraints sendButtonConstraints = new GridBagConstraints();
        sendButtonConstraints.insets = new Insets( 0, 0, 5, 5 );
        sendButtonConstraints.gridx = 2;
        sendButtonConstraints.gridy = 2;
        content.add( sendButton, sendButtonConstraints );

        addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosing( final WindowEvent e )
            {
                final Message logout = new Message( Message.LOGOUT, username );
                send( logout );
                crypter.close();
            }
        } );

        setVisible( true );

        chatMessage.requestFocusInWindow();
    }

    /**
     * Sends the specified string, if necessary, to the server.
     *
     * @param message - message extracted from the message field
     */
    private void send( final String message )
    {
        if ( !message.trim().equals( "" ) )
        {
            if ( message.equals( "/help" ) )
            {
                log( "Available chat commands:" );
                log( "  /list            - List available users present in the Lobby." );
                log( "  /join <username> - Start a conversation with the specified user." );
                log( "  /leave           - Leave your current conversation." );
                log( "  /quit            - Close the application." );

                chatHistory.setCaretPosition( chatHistory.getDocument().getLength() );
                chatMessage.setText( "" );
                chatMessage.requestFocusInWindow();
            }
            else if ( message.equals( "/quit" ) )
            {
                dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
            }
            else
            {
                Message msg;

                /* Chat command sent. */
                if ( message.startsWith( "/" ) )
                {
                    final String cmd = message.replaceFirst( "/", "" );

                    /* /list */
                    if ( cmd.equals( "list" ) )
                    {
                        msg = new Message( Message.MSG_CHAT_CMD, username, cmd );
                    }
                    /* /join */
                    else if ( cmd.startsWith( "join" ) )
                    {
                        crypter.setPeername( message.split( " " ).length > 1 ? message.split( " " )[ 1 ] : null );
                        msg = new Message( Message.MSG_CHAT_CMD, username, cmd );
                    }
                    /* /leave */
                    else
                    {
                        crypter.setPeername( null );
                        msg = new Message( Message.PEER_LEFT, username, cmd );
                    }

                    send( msg );
                }
                /* Chat message sent. */
                else
                {
                    log( username + ": " + message );
                    final byte [ ] [ ] cipherParams = crypter.encrypt( message.getBytes() );

                    msg = new Message( Message.MSG_CHAT, cipherParams[ 0 ] );
                    send( msg );

                    msg = new Message( Message.MSG_PARAMS, cipherParams[ 1 ] );
                    send( msg );
                }

            }
        }
    }

    /**
     * Sends the specified message to the server.
     *
     * @param message - message formed by {@link #send(String)}
     */
    private void send( final Message message )
    {
        crypter.sendMessage( message.getFormattedMessage() );
        chatHistory.setCaretPosition( chatHistory.getDocument().getLength() );
        chatMessage.setText( "" );
        chatMessage.requestFocusInWindow();
    }

    /**
     * Logs an entry into this client's chat history.
     *
     * @param message - entry to be added
     */
    public void log( final String message )
    {
        final String timeStamp = new SimpleDateFormat( "HH:mm:ss" ).format( new Date() );
        chatHistory.append( timeStamp + " " + message + "\n" );
    }

}
