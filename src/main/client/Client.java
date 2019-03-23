package main.client;

public class Client
{

    public static final int SERVER_LOGIN_OK = 0x0;
    public static final int SERVER_LOGIN_FAIL = 0x1;
    public static final int SERVER_LOGOUT_OK = 0x2;

    public static final int PEER_CONN_JOIN_OK = 0x10;
    public static final int PEER_CONN_JOIN_FAIL = 0x11;
    public static final int PEER_BUSY = 0x12;
    public static final int PEER_CONN_EXIT_OK = 0x13;

    public int loginToServer( final String username, final String serverIP, final String serverPort )
    {
        return SERVER_LOGIN_OK;
    }

    public static void main( final String [ ] args )
    {

    }
}
