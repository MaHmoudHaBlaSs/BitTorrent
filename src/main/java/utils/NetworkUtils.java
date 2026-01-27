package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

/**
 * Build handshake
 * Parse handshake
 * Build peer messages
 * Interpret received messages
 */
public class NetworkUtils {

    public static byte[] requestAvailablePeers(String requestUrl){
        try{
            HttpRequest getPeersRequest = HttpRequest.newBuilder()
                    .uri(new URI(requestUrl))
                    .GET()
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            // The tracker's response is a bencoded dictionary with two keys (interval and peers).
            HttpResponse<byte[]> response = client.send(getPeersRequest, BodyHandlers.ofByteArray());

            if (response.statusCode() == HttpURLConnection.HTTP_OK)
                return response.body();
            else
                System.err.println("Problem occurred at connection: "+ response.statusCode());

        } catch (Exception e){
            System.err.println(e.getMessage());
        }
        return null;
    }

    public static byte[] sendAndReceiveHandshake(byte[] handshake, String peerIP, String peerPort){
        try{
            Socket socket = new Socket(peerIP, Integer.parseInt(peerPort));
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            out.write(handshake);
            out.flush();

            byte[] response = new byte[68];
            int read = 0;
            while(read < 68){
                // read() method doesn't guarantee full read,
                // so we perform while loop till we ensure reading 68 bytes
                int actualRead = in.read(response, read, 68 - read);
                if (actualRead == -1){
                    socket.close();
                    throw new RuntimeException("Peer closed connection during handshake");
                }
                read += actualRead;
            }

            socket.close();
            return response;

        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
    }
}
