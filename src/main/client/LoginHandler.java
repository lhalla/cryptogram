package main.client;

@FunctionalInterface
public interface LoginHandler
{
    boolean apply( String username, String serverIP, String serverPort );
}
