package main.common;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Objects;

/**
 * Contains information of a single client connected to the server.
 *
 * @author Lauri Halla-aho
 */
public class ClientInst implements Serializable
{
    private static final long serialVersionUID = -2929911179757165561L;

    private final String username;
    private final InetAddress address;
    private final int port;

    private int keepAliveAttempt = 0;

    /**
     * Constructs a new client instance with the specified info.
     *
     * @param username - connected client's username
     * @param address  - connected client's address
     * @param port     - connected client's port number
     */
    public ClientInst( final String username, final InetAddress address, final int port )
    {
        this.username = username;
        this.address = address;
        this.port = port;
    }

    /**
     * Returns this client instance's username.
     *
     * @return this client instance's username
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * Returns this client instance's address.
     *
     * @return this client instance's address
     */
    public InetAddress getAddress()
    {
        return address;
    }

    /**
     * Returns this client instance's port number.
     *
     * @return this client instance's port number
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Returns this client instance's current keep-alive message counter.
     *
     * @return this client instance's current keep-alive message counter
     */
    public int getKeepAliveAttempt()
    {
        return keepAliveAttempt;
    }

    /**
     * Increments this client instance's keep-alive message counter.
     */
    public void incKeepAliveAttempt()
    {
        keepAliveAttempt++;
    }

    /**
     * Resets this client instance's keep-alive message counter.
     */
    public void resetKeepAliveAttempt()
    {
        keepAliveAttempt = 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( username, address, port );
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( obj == null )
        {
            return false;
        }

        if ( obj == this )
        {
            return true;
        }

        if ( !( obj instanceof ClientInst ) )
        {
            return false;
        }

        final ClientInst other = (ClientInst) obj;

        if ( !username.equals( other.username ) || !address.equals( other.address ) || port != other.port )
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return String.format( "%s", username );
    }
}
