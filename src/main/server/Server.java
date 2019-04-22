package main.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import main.common.ClientInst;
import main.common.ClientPair;
import main.common.Message;

/**
 * A server that handles clients and forwards messages between connected clients.
 *
 * @author Lauri Halla-aho
 */
public class Server implements Runnable
{
    private static final int MAX_KEEPALIVE_ATTEMPTS = 5;

    private DatagramSocket socket;
    private final int serverPort;

    private final Thread serverThread;
    private boolean serverRunning = false;

    private Thread clientHandler;
    private Thread messageSender;
    private Thread messageReceiver;

    private final List < ClientInst > clients;
    private final List < ClientInst > alive;
    private final List < ClientPair > clientPairs;

    /**
     * Constructs a server occupying the specified port number.
     *
     * @param serverPort - the port number this server is run on
     */
    public Server( final int serverPort )
    {
        this.serverPort = serverPort;
        clients = new ArrayList <>();
        alive = new ArrayList <>();
        clientPairs = new ArrayList <>();

        try
        {
            socket = new DatagramSocket( serverPort );
        }
        catch ( final SocketException e )
        {
            e.printStackTrace();
        }

        serverThread = new Thread( this, "Server" );
        serverThread.start();
    }

    @Override
    public void run()
    {
        serverRunning = true;

        handleClients();
        receiveMessages();

        log( "CryptoGram lobby running on port " + serverPort );
    }

    /**
     * Sends keep-alive messages to connected clients and removes unresponsive clients.
     */
    private void handleClients()
    {
        clientHandler = new Thread( "Client Handler" )
        {
            @Override
            public void run()
            {
                while ( serverRunning )
                {
                    final Message msg = new Message( Message.KEEP_ALIVE );

                    /* Send keep-alive messages to all connected clients. */
                    broadcast( msg.getFormattedMessage() );

                    /* Wait two seconds to give the clients time to respond. */
                    try
                    {
                        Thread.sleep( 2000 );
                    }
                    catch ( final InterruptedException e )
                    {
                        e.printStackTrace();
                    }

                    /*
                     * Check if the clients have responded, if not increment the attempt counter or kick them if the
                     * maximum has been reached.
                     */
                    for ( int clientNo = 0; clientNo < clients.size(); clientNo++ )
                    {
                        final ClientInst client = clients.get( clientNo );

                        if ( !alive.contains( client ) )
                        {
                            if ( client.getKeepAliveAttempt() >= MAX_KEEPALIVE_ATTEMPTS )
                            {
                                final Message logout = new Message( Message.LOGOUT, client.getUsername() );
                                processMessage( logout, client.getAddress(), client.getPort() );
                                log( client.getUsername() + " timed out." );
                            }
                            else
                            {
                                client.incKeepAliveAttempt();
                            }
                        }
                        else
                        {
                            alive.remove( client );
                            client.resetKeepAliveAttempt();
                        }
                    }
                }
            }
        };

        clientHandler.start();
    }

    /**
     * Forms a datagram packet from the specified information and sends it.
     *
     * @param formattedMessage - the message to be sent as a formatted byte array
     * @param address          - the address of the recipient
     * @param port             - the port of the recipient
     */
    private void sendMessage( final byte [ ] formattedMessage, final InetAddress address, final int port )
    {
        final DatagramPacket packet = new DatagramPacket( formattedMessage, formattedMessage.length, address, port );
        sendMessage( packet );
    }

    /**
     * Sends the specified datagram packet to its recipient.
     *
     * @param packet - the datagram packet to be sent
     */
    private void sendMessage( final DatagramPacket packet )
    {
        messageSender = new Thread( "Message Sender" )
        {
            @Override
            public void run()
            {
                try
                {
                    socket.send( packet );
                }
                catch ( final IOException e )
                {
                    e.printStackTrace();
                }
            }
        };

        messageSender.start();
    }

    /**
     * Sends the specified message to all connected clients.
     *
     * @param formattedMessage - message to be sent to all
     */
    private void broadcast( final byte [ ] formattedMessage )
    {
        for ( int clientNo = 0; clientNo < clients.size(); clientNo++ )
        {
            final ClientInst client = clients.get( clientNo );
            sendMessage( formattedMessage, client.getAddress(), client.getPort() );
        }
    }

    /**
     * Starts a thread that handles listening for incoming messages and processes them accordingly.
     */
    private void receiveMessages()
    {
        messageReceiver = new Thread( "Message Receiver" )
        {
            @Override
            public void run()
            {
                while ( serverRunning )
                {
                    final byte [ ] messageBytes = new byte [ 1024 ];
                    final DatagramPacket packet = new DatagramPacket( messageBytes, messageBytes.length );
                    try
                    {
                        socket.receive( packet );
                    }
                    catch ( final IOException e )
                    {
                        e.printStackTrace();
                    }

                    final byte [ ] payload = new byte [ packet.getLength() ];

                    System.arraycopy( packet.getData(), packet.getOffset(), payload, 0, packet.getLength() );

                    final Message message = Message.parseFormattedMessage( payload );

                    processMessage( message, packet.getAddress(), packet.getPort() );
                }
            }
        };

        messageReceiver.start();
    }

    /**
     * Processes the received messages and reacts appropriately.
     *
     * @param message       - received message
     * @param senderAddress - address of the sender
     * @param senderPort    - port of the sender
     */
    private void processMessage( final Message message, final InetAddress senderAddress, final int senderPort )
    {
        /* USER LOGIN HANDLING */
        if ( message.getType() == Message.LOGIN )
        {
            if ( isUsernameAvailable( message.getUsername() ) )
            {
                log( message.getUsername() + " joined the lobby." );
                clients.add( new ClientInst( message.getUsername(), senderAddress, senderPort ) );

                sendMessage( message.getFormattedMessage(), senderAddress, senderPort );
            }
            /* A user with an identical name is already connected, find an available variation. */
            else
            {
                log( "User with a duplicate name tried to join the lobby." );

                String newUsername = message.getUsername();

                int ctr = 0;
                while ( !isUsernameAvailable( newUsername ) )
                {
                    newUsername = message.getUsername() + ctr;
                    ctr++;
                }

                log( "Generated a unique name for the new user." );

                clients.add( new ClientInst( newUsername, senderAddress, senderPort ) );

                final Message msg = new Message( Message.LOGIN, newUsername );

                sendMessage( msg.getFormattedMessage(), senderAddress, senderPort );
            }
        }
        /* KEEP ALIVE */
        else if ( message.getType() == Message.KEEP_ALIVE )
        {
            final Optional < ClientInst > maybeClient = findClient( senderAddress, senderPort );

            if ( maybeClient.isPresent() )
            {
                final ClientInst sender = maybeClient.get();

                alive.add( sender );
            }
        }
        /* SECRET SHARING */
        else if ( message.getType() == Message.DHKX_REQ || message.getType() == Message.DHKX_RES )
        {
            final Optional < ClientInst > maybeClient = findClient( senderAddress, senderPort );

            if ( maybeClient.isPresent() )
            {
                final ClientInst sender = maybeClient.get();
                if ( forwardToPeer( message.getFormattedMessage(), sender ) )
                {
                    log( "Forwarded secret parameters from " + sender.getUsername() );
                }
                else
                {
                    log( "Failed to forward secret parameters from " + sender.getUsername() + " (not paired)." );
                }
            }
        }
        /* CHAT HANDLING */
        else if ( message.getType() == Message.MSG_CHAT || message.getType() == Message.MSG_PARAMS )
        {
            final Optional < ClientInst > maybeClient = findClient( senderAddress, senderPort );

            if ( maybeClient.isPresent() )
            {
                final ClientInst sender = maybeClient.get();
                if ( forwardToPeer( message.getFormattedMessage(), sender ) )
                {
                    log( "Forwarded chat message/params from " + sender.getUsername() );
                }
                else
                {
                    log( "Failed to forward chat message/params from " + sender.getUsername() + " (not paired)." );

                    final Message msg = new Message( Message.MSG_CHAT_CMD, "server", "You are not in a conversation." );

                    sendMessage( msg.getFormattedMessage(), sender.getAddress(), sender.getPort() );
                }
            }
        }
        /* USER LOGOUT HANDLING */
        else if ( message.getType() == Message.LOGOUT )
        {
            final Optional < ClientInst > maybeClient = findClient( message.getUsername() );

            if ( maybeClient.isPresent() )
            {
                final ClientInst sender = maybeClient.get();

                log( sender.getUsername() + " left the lobby." );
                clients.remove( sender );

                final Optional < ClientPair > maybePair = findPair( sender );

                if ( maybePair.isPresent() )
                {
                    final ClientPair pair = maybePair.get();
                    final ClientInst peer = findPeer( sender ).get();

                    clientPairs.remove( pair );

                    final Message msg = new Message( Message.PEER_LEFT,
                            sender.getUsername() + " has left. You have been returned to the lobby." );

                    sendMessage( msg.getFormattedMessage(), peer.getAddress(), peer.getPort() );
                }
            }
        }
        /* USER LEFT CONVERSATION */
        else if ( message.getType() == Message.PEER_LEFT )
        {
            final Optional < ClientInst > maybeClient = findClient( senderAddress, senderPort );

            if ( maybeClient.isPresent() )
            {
                final ClientInst sender = maybeClient.get();

                final Optional < ClientPair > maybePair = findPair( sender );

                if ( maybePair.isPresent() )
                {
                    final ClientPair pair = maybePair.get();
                    final ClientInst peer = findPeer( sender ).get();

                    clientPairs.remove( pair );

                    Message msg = new Message( Message.PEER_LEFT,
                            sender.getUsername() + " has left. You have been returned to the lobby" );

                    sendMessage( msg.getFormattedMessage(), peer.getAddress(), peer.getPort() );

                    msg = new Message( Message.MSG_CHAT_CMD, "server",
                            "You have left the conversation with " + peer.getUsername() + "." );

                    sendMessage( msg.getFormattedMessage(), sender.getAddress(), sender.getPort() );
                }
            }
        }
        /* CHAT COMMAND HANDLING */
        else if ( message.getType() == Message.MSG_CHAT_CMD )
        {
            final Optional < ClientInst > maybeClient = findClient( message.getUsername() );

            if ( maybeClient.isPresent() )
            {
                final ClientInst sender = maybeClient.get();

                /* List command */
                if ( message.getMessage().equals( "list" ) )
                {
                    final String clientList = getAvailableClientsString( message.getUsername() );

                    log( sender.getUsername() + " requested a list of the available clients." );

                    final Message msg = new Message( Message.MSG_CHAT_CMD, "server", clientList );

                    sendMessage( msg.getFormattedMessage(), sender.getAddress(), sender.getPort() );
                }
                /* Join command. */
                else if ( message.getMessage().startsWith( "join" ) )
                {
                    final String [ ] params = message.getMessage().split( " " );

                    if ( params.length == 2 )
                    {
                        final Optional < ClientInst > maybeNewPeer = findClient( params[ 1 ] );

                        if ( maybeNewPeer.isPresent() )
                        {
                            final ClientInst newPeer = maybeNewPeer.get();
                            final Optional < ClientPair > maybePair = findPair( newPeer );

                            /* Client tried to start a conversation with themselves. */
                            if ( newPeer.equals( sender ) )
                            {
                                final Message msg = new Message( Message.MSG_CHAT_CMD, "server",
                                        "You can't start a conversation with yourself." );

                                sendMessage( msg.getFormattedMessage(), sender.getAddress(), sender.getPort() );
                            }
                            /* Client is already in a conversation. */
                            else if ( findPair( sender ).isPresent() )
                            {
                                final Message msg = new Message( Message.MSG_CHAT_CMD, "server",
                                        "You are already in a conversation with "
                                                + findPeer( sender ).get().getUsername() + "." );

                                sendMessage( msg.getFormattedMessage(), sender.getAddress(), sender.getPort() );
                            }
                            /* Requested peer is already in a conversation. */
                            else if ( maybePair.isPresent() )
                            {
                                final Message msg = new Message( Message.PEER_BUSY );

                                sendMessage( msg.getFormattedMessage(), sender.getAddress(), sender.getPort() );
                            }
                            /* Everything is OK. */
                            else
                            {
                                Message msg = new Message( Message.PEER_AVAILABLE );

                                sendMessage( msg.getFormattedMessage(), sender.getAddress(), sender.getPort() );

                                msg = new Message( Message.NEW_PEER, sender.getUsername(),
                                        "Entering a conversation with " + sender.getUsername() + "." );

                                sendMessage( msg.getFormattedMessage(), newPeer.getAddress(), newPeer.getPort() );

                                clientPairs.add( new ClientPair( sender, newPeer ) );
                            }

                        }
                        /* Specified username not found. */
                        else
                        {
                            final Message msg = new Message( Message.MSG_CHAT_CMD, "server", "User not found." );

                            sendMessage( msg.getFormattedMessage(), sender.getAddress(), sender.getPort() );
                        }
                    }
                    /* Invalid usage of the command. */
                    else
                    {
                        final Message msg = new Message( Message.MSG_CHAT_CMD, "server",
                                "Join command usage:\n/join <username>" );

                        sendMessage( msg.getFormattedMessage(), sender.getAddress(), sender.getPort() );
                    }
                }
            }
        }
    }

    /**
     * Returns a list of client instances not in a conversation.
     *
     * @return a list of available client instances
     */
    private List < ClientInst > getAvailableClients()
    {
        return clients.stream().filter( client -> clientPairs.stream().noneMatch( pair -> pair.contains( client ) ) )
                .collect( Collectors.toList() );
    }

    /**
     * Returns a string representation of a list of available clients.
     *
     * @param requester - username of the party requesting the list
     * @return a string representation of a list of available clients
     */
    private String getAvailableClientsString( final String requester )
    {
        final List < ClientInst > availableClients = getAvailableClients();

        final StringBuilder list = new StringBuilder( "Available users:\n" );
        for ( final ClientInst client : availableClients )
        {
            list.append( " - " + client.getUsername() );
            if ( client.getUsername().equals( requester ) )
            {
                list.append( " (you)" );
            }
            list.append( "\n" );
        }

        return list.toString();
    }

    /**
     * Returns a client instance with the specified username if one is connected to the server.
     *
     * @param username - username of a client to be searched
     * @return a client instance with the specified username if present, empty otherwise
     */
    private Optional < ClientInst > findClient( final String username )
    {
        return clients.stream().filter( client -> client.getUsername().equals( username ) ).findFirst();
    }

    /**
     * Returns a client instance with the specified address and port number if one is connected to the server.
     *
     * @param address - address of a client to be searched
     * @param port    - port number of a client to be searched
     * @return a client instance with the specified address and port number if present, empty otherwise
     */
    private Optional < ClientInst > findClient( final InetAddress address, final int port )
    {
        return clients.stream().filter( client -> client.getAddress().equals( address ) && client.getPort() == port )
                .findFirst();
    }

    /**
     * Returns <code>true</code> if the specified username is available.
     *
     * @param username - requested username
     * @return <code>true</code> if the specified username is available
     */
    private boolean isUsernameAvailable( final String username )
    {
        return !findClient( username ).isPresent();
    }

    /**
     * Returns a client pair containing the specified client instance if one is present.
     *
     * @param client - a client instance to be searched for
     * @return a client pair containing the specified client instance if one is present, empty otherwise
     */
    private Optional < ClientPair > findPair( final ClientInst client )
    {
        return clientPairs.stream().filter( pair -> pair.contains( client ) ).findFirst();
    }

    /**
     * Returns a client instance that is in a conversation with the specified client if one is present.
     *
     * @param client - a client instance to be searched for
     * @return a client instance that is in a conversation with the specified client if one is present, empty otherwise
     */
    private Optional < ClientInst > findPeer( final ClientInst client )
    {
        final Optional < ClientPair > maybePair = findPair( client );

        if ( maybePair.isPresent() )
        {
            final ClientPair pair = maybePair.get();

            final ClientInst peer = pair.getFirst().equals( client ) ? pair.getSecond() : pair.getFirst();

            return Optional.of( peer );
        }

        return Optional.empty();
    }

    /**
     * Forwards the specified message to the specified client's peer.
     *
     * @param formattedMessage - a message to be forwarded
     * @param sender           - the client instance the message originated from
     * @return <code>true</code> if the forwarding was successful
     */
    private boolean forwardToPeer( final byte [ ] formattedMessage, final ClientInst sender )
    {
        final Optional < ClientInst > maybePeer = findPeer( sender );

        if ( maybePeer.isPresent() )
        {
            final ClientInst peer = maybePeer.get();

            sendMessage( formattedMessage, peer.getAddress(), peer.getPort() );

            return true;
        }

        return false;
    }

    /**
     * Logs an event to the console of the server.
     *
     * @param message - event message to be logged
     */
    private void log( final String message )
    {
        final String timeStamp = new SimpleDateFormat( "HH:mm:ss" ).format( new Date() );
        System.err.println( timeStamp + " " + message );
    }
}
