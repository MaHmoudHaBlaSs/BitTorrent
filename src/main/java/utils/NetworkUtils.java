package utils;

import protocol.Handshake;
import protocol.PeerMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

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

            // Each 6-Bytes group represents IP (4) + Port (2) of a pair.
            if (response.statusCode() == HttpURLConnection.HTTP_OK)
                return response.body();
            else
                System.err.println("Problem occurred at connection: "+ response.statusCode());

        } catch (Exception e){
            System.err.println(e.getMessage());
        }
        return null;
    }

    public static Handshake sendAndReceiveHandshake(InputStream in, OutputStream out, byte[] handshake){
        try{
            out.write(handshake);
            out.flush();

            byte[] response = new byte[68];
            int read = 0;
            while(read < 68){
                // read() method doesn't guarantee full read,
                // so we perform while loop till we ensure reading 68 bytes
                int actualRead = in.read(response, read, 68 - read);
                if (actualRead == -1){
                    throw new RuntimeException("Peer closed connection during handshake");
                }
                read += actualRead;
            }

            return new Handshake(response);

        } catch (Exception e) {
            System.err.println(e.getMessage());
            return null;
        }
    }

    public static PeerMessage readPeerMessage(final InputStream in) throws IOException {
        byte[] rawMsgLength = new byte[4];

        int read = 0;
        while(read < 4){
            // read() method doesn't guarantee full read,
            // so we perform while loop till we ensure reading 4 bytes
            int actualRead = in.read(rawMsgLength, read, 4 - read);
            if (actualRead == -1)
                throw new IOException("Peer closed connection during reading input stream length");

            read += actualRead;
        }

        int messageLength = EncodingUtils.convertBytesToInt(rawMsgLength);
        if (messageLength == 0)
            return null;

        byte[] message = new byte[messageLength];
        read = 0;
        while (read < messageLength){
            int actualRead = in.read(message, read, messageLength - read);
            if (actualRead == -1)
                throw new IOException("Peer closed connection during reading input stream");

            read += actualRead;
        }
        return new PeerMessage(messageLength, message);
    }

    public static void sendPeerMessage(final OutputStream out, PeerMessage message) throws IOException{
        out.write(message.getRawMessage());
        out.flush();
    }
}
