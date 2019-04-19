package main.common;

import java.io.Serializable;
import java.util.Objects;

public class ClientInst implements Serializable
{
    private static final long serialVersionUID = -2929911179757165561L;

    private final String username;
    private final String address;
    private final boolean busy;

    public ClientInst( final String username, final String address, final boolean busy )
    {
        this.username = username;
        this.address = address;
        this.busy = busy;
    }

    public String getUsername()
    {
        return username;
    }

    public String getAddress()
    {
        return address;
    }

    public boolean isBusy()
    {
        return busy;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( username, address, busy );
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

        if ( !username.equals( other.username ) || !address.equals( other.address ) || busy != other.busy )
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
