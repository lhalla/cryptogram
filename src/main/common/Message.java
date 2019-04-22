package main.common;

import java.io.Serializable;

/**
 * Contains the type and payload of a message.
 *
 * @author Lauri Halla-aho
 */
public class Message implements Serializable
{
    public static final int MSG_CHAT = 10;
    public static final int MSG_PARAMS = 11;
    public static final int MSG_CHAT_CMD = 12;
    public static final int KEEP_ALIVE = 13;

    public static final int LOGIN = 20;
    public static final int LOGOUT = 21;
    public static final int PEER_AVAILABLE = 22;
    public static final int PEER_BUSY = 23;
    public static final int PEER_LEFT = 24;
    public static final int NEW_PEER = 25;

    public static final int DHKX_REQ = 30;
    public static final int DHKX_RES = 31;

    private static final long serialVersionUID = 8890994480826677411L;

    private final int type;

    private final byte [ ] payload;

    private String username = null;
    private String message = null;

    /**
     * Constructs a message with the specified type, username, and payload.
     *
     * @param type     - type of this message
     * @param username - username of the sender or an affected party
     * @param payload  - contents of this message
     */
    public Message( final int type, final String username, final String payload )
    {
        this( type, username + "/" + payload );
    }

    /**
     * Constructs a message with the specified type and payload.
     *
     * @param type    - type of this message
     * @param payload - contents of this message
     */
    public Message( final int type, final String payload )
    {
        this( type, payload.getBytes() );
    }

    /**
     * Constructs a message with the specified type and payload.
     *
     * @param type    - type of this message
     * @param payload - contents of this message
     */
    public Message( final int type, final byte [ ] payload )
    {
        this.type = type;
        this.payload = payload;

        switch ( type )
        {
            case LOGIN:
            case LOGOUT:
                username = new String( payload ).trim();
                break;
            case PEER_LEFT:
                message = new String( payload ).trim();
                break;
            case NEW_PEER:
            case MSG_CHAT_CMD:
                final String payloadString = new String( payload ).trim();
                username = payloadString.substring( 0, payloadString.indexOf( '/' ) );
                message = payloadString.substring( payloadString.indexOf( '/' ) + 1 );
                break;
            case MSG_CHAT:
            case MSG_PARAMS:
            default:
                break;
        }
    }

    /**
     * Constructs a message with the specified type.
     *
     * @param type - type of this message.
     */
    public Message( final int type )
    {
        this.type = type;
        payload = new byte [ 0 ];
    }

    /**
     * Returns the type of this message.
     *
     * @return the type of this message
     */
    public int getType()
    {
        return type;
    }

    /**
     * Returns the payload of this message.
     *
     * @return the payload of this message
     */
    public byte [ ] getPayload()
    {
        return payload;
    }

    /**
     * Returns the username of the sender of this message or an affected party.
     *
     * @return the username of the sender of this message or an affected party
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * Returns the string representation of the payload if available.
     *
     * @return the string representation of the payload if available
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Returns a formatted byte array of this message.
     *
     * @return a formatted byte array
     */
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

    /**
     * Parses the specified byte array into a message object.
     * 
     * @param formattedMessage - a formatted message produced by {@link #getFormattedMessage()}
     * @return a message based on the specified formatted message
     */
    public static Message parseFormattedMessage( final byte [ ] formattedMessage )
    {
        if ( formattedMessage.length >= 3 )
        {
            final int type = Integer
                    .parseInt( new String( new byte [ ] { formattedMessage[ 0 ], formattedMessage[ 1 ] } ) );
            final byte [ ] payload = new byte [ formattedMessage.length - 3 ];

            for ( int i = 0; i < payload.length; i++ )
            {
                payload[ i ] = formattedMessage[ i + 3 ];
            }

            return new Message( type, payload );
        }
        return new Message( 0, new byte [ 0 ] );
    }

}
