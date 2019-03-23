package main.client.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class ClientGUI extends JFrame
{

    private static final long serialVersionUID = 6962717251338732138L;
    private final JPanel contentPane;

    private final String username;

    /**
     * Create the frame.
     */
    public ClientGUI( final String username, final String serverIP, final String serverPort )
    {
        setResizable( false );
        this.username = username;
        contentPane = new JPanel();

        createWindow();
    }

    private void createWindow()
    {
        setTitle( "CryptoGram - " + username );
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        setSize( 800, 500 );
        contentPane.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
        setContentPane( contentPane );

        final GridBagLayout gbl_contentPane = new GridBagLayout();
        gbl_contentPane.columnWidths = new int [ ] { 30, 200, 540, 30 };
        gbl_contentPane.rowHeights = new int [ ] { 50, 400, 40, 10 };
        gbl_contentPane.columnWeights = new double [ ] { 1.0, Double.MIN_VALUE };
        gbl_contentPane.rowWeights = new double [ ] { 1.0, Double.MIN_VALUE };
        contentPane.setLayout( gbl_contentPane );

        final DefaultListModel < String > model = new DefaultListModel <>();
        final JList < String > list = new JList <>( model );
        final GridBagConstraints gbc_list = new GridBagConstraints();
        model.addElement( "TEST" );
        model.addElement( "BEST" );
        model.addElement( "JEST" );
        gbc_list.fill = GridBagConstraints.BOTH;
        gbc_list.gridx = 1;
        gbc_list.gridy = 1;
        contentPane.add( list, gbc_list );
        setVisible( true );
    }

}
