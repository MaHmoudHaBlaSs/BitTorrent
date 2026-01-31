package peer;

import files.Block;
import files.PieceDownload;
import protocol.Handshake;
import protocol.PeerMessage;
import protocol.ProtocolUtils;
import utils.NetworkUtils;
import utils.Torrent;

import java.io.IOException;
import java.util.LinkedList;

public class Swarm {
    public static class ConnectionResponse{
        PeerMessage message;
        PeerConnection connection;

        public ConnectionResponse(PeerMessage message, PeerConnection connection) {
            this.message = message;
            this.connection = connection;
        }
    }

    static final int MAX_CONNECTIONS = 10;

    String[][] peersInfo;
    LinkedList<PeerConnection> peerConnections;
    Torrent torrentFile;
    PieceDownload pieceDownloader;
    PeerMessageHandler messageHandler;


    public Swarm(byte[] rawPeersInfo, Torrent torrentFile, PieceDownload piece, String myId){

        this.peersInfo = ProtocolUtils.toPeersString(rawPeersInfo);
        this.peerConnections = new LinkedList<>();
        this.pieceDownloader = piece;
        this.torrentFile = torrentFile;
        this.messageHandler = new PeerMessageHandler();

        for (int i = 0; i < peersInfo.length && i < MAX_CONNECTIONS; i++){
            peerConnections.add(new PeerConnection( new Handshake(torrentFile, myId),
                    peersInfo[i][0],
                    Integer.parseInt(peersInfo[i][1])));
        }
    }

    public boolean work(){
        try{
            while (!pieceDownloader.isCompleted()){
                ConnectionResponse response = getMessage();

                if (response != null){
                    messageHandler.handle(response.message,
                            new PeerContext(response.connection.getState(),
                                    torrentFile, response.connection.getOut(),
                                    pieceDownloader)
                    );
                }
                else{
                    System.out.println("No Response Received.");
                }
            }

        } catch (Exception e){
            return false;
        }

        return true;
    }

    public ConnectionResponse getMessage(){
        for (PeerConnection connection: peerConnections) {
            try {
                PeerMessage receivedMessage = NetworkUtils.readPeerMessage(connection.getIn());

                if (receivedMessage != null)
                    return new ConnectionResponse(receivedMessage, connection);

            } catch (IOException e) {
                System.out.println("A Connection failed while receiving/handling");
            }
        }
        return null;
    }

    public void destroy() {
        try{
            for (PeerConnection connection: peerConnections) {
                connection.closeConnection();
            }
        } catch (Exception e){
            System.out.println("Smth happened while closing a Connection,");
        }
    }
}
