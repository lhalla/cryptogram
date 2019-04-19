package main.client.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class ClientGUI extends JFrame
{

    private static final long serialVersionUID = 6962717251338732138L;
    private final JPanel contentPane;

    private final String username;

    private JTextArea history;

    private JTextField txtMessage;

    /**
     * Create the frame.
     */
    public ClientGUI( final String username, final String serverIP, final int serverPort )
    {
        setResizable( false );
        this.username = username;
        contentPane = new JPanel();

        createWindow();
        log( String.format( "Successfully connected to %s:%d as %s!", serverIP, serverPort, username ) );
        log( "For a list of available chat commands, type /help." );
    }

    private void createWindow()
    {
        try
        {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        }
        catch ( final Exception e )
        {
            System.err.println( "Failed to set login window to system native look." );
        }

        setTitle( "CryptoGram Client - " + username );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setSize( 800, 500 );
        contentPane.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
        setContentPane( contentPane );

        final GridBagLayout gbl_contentPane = new GridBagLayout();
        gbl_contentPane.columnWidths = new int [ ] { 30, 665, 75, 30 };
        gbl_contentPane.rowHeights = new int [ ] { 50, 400, 40, 10 };
        gbl_contentPane.columnWeights = new double [ ] { 1.0, Double.MIN_VALUE };
        gbl_contentPane.rowWeights = new double [ ] { 1.0, Double.MIN_VALUE };
        contentPane.setLayout( gbl_contentPane );

        history = new JTextArea();
        history.setEditable( false );
        final JScrollPane scrollHistory = new JScrollPane( history );
        final GridBagConstraints gbc_scrollHistory = new GridBagConstraints();
        gbc_scrollHistory.insets = new Insets( 0, 0, 5, 5 );
        gbc_scrollHistory.fill = GridBagConstraints.BOTH;
        gbc_scrollHistory.gridx = 1;
        gbc_scrollHistory.gridy = 1;
        gbc_scrollHistory.gridwidth = 2;
        contentPane.add( scrollHistory, gbc_scrollHistory );

        txtMessage = new JTextField();
        txtMessage.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyPressed( final KeyEvent arg0 )
            {
                if ( arg0.getKeyCode() == KeyEvent.VK_ENTER )
                {
                    send( txtMessage.getText() );
                }
            }
        } );
        final GridBagConstraints gbc_txtMessage = new GridBagConstraints();
        gbc_txtMessage.insets = new Insets( 5, 5, 5, 5 );
        gbc_txtMessage.fill = GridBagConstraints.BOTH;
        gbc_txtMessage.gridx = 1;
        gbc_txtMessage.gridy = 2;
        contentPane.add( txtMessage, gbc_txtMessage );

        final JButton btnSend = new JButton( "Send" );
        btnSend.addActionListener( click -> send( txtMessage.getText() ) );
        final GridBagConstraints gbc_btnSend = new GridBagConstraints();
        gbc_btnSend.insets = new Insets( 0, 0, 5, 5 );
        gbc_btnSend.gridx = 2;
        gbc_btnSend.gridy = 2;
        contentPane.add( btnSend, gbc_btnSend );
        setVisible( true );

        txtMessage.requestFocusInWindow();
    }

    private void send( final String message )
    {
        if ( !message.trim().equals( "" ) )
        {
            if ( !message.startsWith( "/" ) )
            {
                log( username + ": " + message );
            }
            history.setCaretPosition( history.getDocument().getLength() );
            txtMessage.setText( "" );
            txtMessage.requestFocusInWindow();
        }
    }

    public void log( final String message )
    {
        final String timeStamp = new SimpleDateFormat( "HH:mm:ss" ).format( new Date() );
        history.append( timeStamp + " " + message + "\n" );
    }

}
