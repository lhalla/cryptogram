package main.client;

import main.common.Message;

/**
 * Provides methods for encrypting and decrypting data.
 *
 * @author Lauri Halla-aho
 */
public interface Crypter
{
    /**
     * Encrypts the specified byte array and returns the encrypted data and the used cipher parameters.
     *
     * @param plainText - data to be encrypted
     * @return encrypted data and cipher parameters
     */
    byte [ ] [ ] encrypt( final byte [ ] plainText );

    /**
     * Decrypts the specified ciphertext using the specified cipher parameters.
     *
     * @param cipherText - data to be decrypted
     * @param params     - parameters used to decrypt the data
     * @return decrypted data
     */
    byte [ ] decrypt( final byte [ ] cipherText, final byte [ ] params );

    /**
     * Sends the specified formatted {@link Message} to the server.
     *
     * @param formattedMessage - a formatted message received
     */
    void sendMessage( final byte [ ] formattedMessage );

    /**
     * Closes the client's socket and ends its running processes.
     */
    void close();

    /**
     * Sets the name of the peer this client is conversing with.
     *
     * @param peername - name of the peer this client is conversing with
     */
    void setPeername( final String peername );
}
