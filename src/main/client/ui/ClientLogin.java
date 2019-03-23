package main.client.ui;

import java.awt.EventQueue;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import main.client.Client;

public class ClientLogin extends JFrame
{

    private static final long serialVersionUID = 7051067988953935071L;

    private final Client clientBE;

    private final JPanel contentPane;

    private final JTextField fUsername;
    private final JTextField fServerIP;
    private final JTextField fServerPort;

    /**
     * Create the frame.
     *
     * @throws UnsupportedLookAndFeelException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public ClientLogin()
    {
        clientBE = new Client();

        contentPane = new JPanel();

        fUsername = new JTextField();
        fServerIP = new JTextField();
        fServerPort = new JTextField();

        createLoginPrompt();
    }

    private void createLoginPrompt()
    {

        try
        {
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );
        }
        catch ( final Exception e )
        {
            System.err.println( "Failed to set login window to system native look." );
        }

        setResizable( false );
        setTitle( "CryptoGram - Login" );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setBounds( 100, 100, 300, 200 );
        contentPane.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
        setContentPane( contentPane );
        contentPane.setLayout( null );

        fUsername.setBounds( 100, 11, 160, 20 );
        contentPane.add( fUsername );
        fUsername.setColumns( 10 );

        final JLabel lblUsername = new JLabel( "Username:" );
        lblUsername.setBounds( 10, 14, 80, 14 );
        contentPane.add( lblUsername );

        fServerIP.setBounds( 100, 42, 160, 20 );
        contentPane.add( fServerIP );
        fServerIP.setColumns( 10 );

        final JLabel lblServerAddr = new JLabel( "Server IP:" );
        lblServerAddr.setBounds( 10, 45, 80, 14 );
        contentPane.add( lblServerAddr );

        fServerPort.setBounds( 100, 73, 160, 20 );
        contentPane.add( fServerPort );
        fServerPort.setColumns( 10 );

        final JLabel lblPort = new JLabel( "Server Port:" );
        lblPort.setBounds( 10, 76, 80, 14 );
        contentPane.add( lblPort );

        final JButton btnCancel = new JButton( "Cancel" );
        btnCancel.addActionListener( click -> dispose() );
        btnCancel.setBounds( 171, 127, 89, 23 );
        contentPane.add( btnCancel );

        final JButton btnLogin = new JButton( "Login" );
        btnLogin.addActionListener(
                click -> performLogin( fUsername.getText(), fServerIP.getText(), fServerPort.getText() ) );
        btnLogin.setBounds( 10, 127, 89, 23 );
        contentPane.add( btnLogin );
    }

    private void performLogin( final String username, final String serverIP, final String serverPort )
    {
        System.err.println( String.format( "%s, %s:%s", username, serverIP, serverPort ) );

        final int errorCode = clientBE.loginToServer( username, serverIP, serverPort );

        if ( errorCode == Client.SERVER_LOGIN_OK )
        {
            dispose();
            new ClientGUI( username, serverIP, serverPort );
        }
        else
        {
            System.err.println( "Login failed!" );
        }
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
                final ClientLogin frame = new ClientLogin();
                frame.setVisible( true );
            }
            catch ( final Exception e )
            {
                e.printStackTrace();
            }
        } );
    }
}
