package main.common;

import java.io.Serializable;

public class Message implements Serializable
{
    public static final int MSG_CHAT = 0;
    public static final int MSG_HELP_CMD = 1;
    public static final int PEER_AVAILABLE = 2;
    public static final int PEER_BUSY = 3;
    public static final int DH_KEY_EXCHANGE1 = 4;
    public static final int DH_KEY_EXCHANGE2 = 5;

    private static final long serialVersionUID = 8890994480826677411L;

    private final int type;

    private final byte [ ] payload;

    public Message( final int type, final String payload )
    {
        this( type, payload.getBytes() );
    }

    public Message( final int type, final byte [ ] payload )
    {
        this.type = type;
        this.payload = payload;
    }

    public int getType()
    {
        return type;
    }

    public byte [ ] getPayload()
    {
        return payload;
    }

    public static Message parseFormattedMessage( final byte [ ] formattedMessage )
    {
        final int type = Integer.parseInt( new String( new byte [ ] { formattedMessage[ 0 ] } ) );
        final byte [ ] payload = new byte [ formattedMessage.length - 2 ];

        for ( int i = 0; i < payload.length; i++ )
        {
            payload[ i ] = formattedMessage[ i + 2 ];
        }

        return new Message( type, payload );
    }

    public byte [ ] getFormattedMessage()
    {
        final String prefix = type + "/";
        final int prefixLength = prefix.getBytes().length;
        final byte [ ] result = new byte [ prefixLength + payload.length ];

        for ( int i = 0; i < prefixLength; i++ )
        {
            result[ i ] = prefix.getBytes()[ i ];
        }

        for ( int i = prefixLength; i < result.length; i++ )
        {
            result[ i ] = payload[ i - prefixLength ];
        }

        return result;
    }

}
