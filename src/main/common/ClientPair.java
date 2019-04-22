package main.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Contains information of a single conversing client pair.
 *
 * @author Lauri Halla-aho
 */
public class ClientPair implements Serializable
{
    private static final long serialVersionUID = -1735483448518434431L;

    private final ClientInst client1;
    private final ClientInst client2;

    /**
     * Constructs a new client pair with the specified client instances.
     *
     * @param client1 - first participating client instance
     * @param client2 - second participating client instance
     */
    public ClientPair( final ClientInst client1, final ClientInst client2 )
    {
        this.client1 = client1;
        this.client2 = client2;
    }

    /**
     * Returns the first participating client instance.
     *
     * @return the first participating client instance
     */
    public ClientInst getFirst()
    {
        return client1;
    }

    /**
     * Returns the second participating client instance.
     *
     * @return the second participating client instance
     */
    public ClientInst getSecond()
    {
        return client2;
    }

    /**
     * Returns <code>true</code> if the specified client instance is a member of this pair.
     * 
     * @param other - a client instance
     * @return <code>true</code> if the specified client instance is a member of this pair
     */
    public boolean contains( final ClientInst other )
    {
        return client1.equals( other ) || client2.equals( other );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( client1, client2 );
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

        if ( obj instanceof ClientInst )
        {
            final ClientInst other = (ClientInst) obj;

            if ( other.equals( client1 ) || other.equals( client2 ) )
            {
                return true;
            }
        }
        else if ( obj instanceof ClientPair )
        {
            final ClientPair other = (ClientPair) obj;

            if ( other.client1.equals( client1 ) && other.client2.equals( client2 ) )
            {
                return true;
            }
        }

        return false;
    }
}
