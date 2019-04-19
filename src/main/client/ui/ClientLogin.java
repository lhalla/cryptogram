package main.client.ui;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

import main.client.LoginHandler;

public class ClientLogin extends JFrame
{

    private static final long serialVersionUID = 7051067988953935071L;

    private final JPanel contentPane;

    private final JTextField fUsername;
    private final JTextField fServerIP;
    private final JTextField fServerPort;

    private final LoginHandler loginHandler;

    /**
     * Create the frame.
     *
     * @throws UnsupportedLookAndFeelException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public ClientLogin( final LoginHandler loginHandler )
    {
        this.loginHandler = loginHandler;

        contentPane = new JPanel();

        fUsername = new JTextField();
        fUsername.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyPressed( final KeyEvent arg0 )
            {
                if ( arg0.getKeyCode() == KeyEvent.VK_ENTER )
                {
                    fServerIP.requestFocusInWindow();
                }
            }
        } );
        fServerIP = new JTextField();
        fServerIP.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyPressed( final KeyEvent e )
            {
                if ( e.getKeyCode() == KeyEvent.VK_ENTER )
                {
                    fServerPort.requestFocusInWindow();
                }
            }
        } );
        fServerPort = new JTextField();
        fServerPort.addKeyListener( new KeyAdapter()
        {
            @Override
            public void keyPressed( final KeyEvent e )
            {
                if ( e.getKeyCode() == KeyEvent.VK_ENTER )
                {
                    performLogin( fUsername.getText(), fServerIP.getText(), fServerPort.getText() );
                }
            }
        } );

        createLoginPrompt();
    }

    private void createLoginPrompt()
    {
        setDefaultLookAndFeelDecorated( true );

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
        setVisible( true );

        fUsername.requestFocusInWindow();
    }

    private void performLogin( final String username, final String serverIP, final String serverPort )
    {
        final boolean success = loginHandler.apply( username, serverIP, serverPort );

        if ( success )
        {
            dispose();
        }
        else
        {
            System.err.println( "Login failed!" );
        }
    }
}
