package peer;

import protocol.Handshake;
import utils.NetworkUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

public class PeerConnection {
    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private PeerState state;



    public PeerConnection(Handshake handshake, String peerIP, int peerPort){
        try{
            socket = new Socket(peerIP, peerPort);
            out = socket.getOutputStream();
            in = socket.getInputStream();

            Handshake responseHandshake = NetworkUtils.sendAndReceiveHandshake(in, out, handshake.getRawHandshake());

            if (responseHandshake == null)
                throw new RuntimeException("Empty Response Handshake");

            if (!Arrays.equals(handshake.getInfoHash(), responseHandshake.getInfoHash()))
                throw new RuntimeException("Incorrect Info Hash Value.");

            state = new PeerState();

        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    public void closeConnection() throws IOException {
        in.close();
        out.close();
        if (!socket.isClosed())
            socket.close();
    }

    public PeerState getState() {
        return state;
    }

    public InputStream getIn() {
        return in;
    }

    public OutputStream getOut() {
        return out;
    }
}
