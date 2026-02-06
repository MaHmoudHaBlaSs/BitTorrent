package peer;

import files.FileDownloader;
import files.PieceDownloader;
import protocol.Handshake;
import protocol.PeerMessage;
import protocol.ProtocolUtils;
import utils.NetworkUtils;
import utils.Torrent;

import java.io.IOException;

public class Swarm {
    // Connection Response is just a wrapper class to enable passing (message + connection)
    public static class ConnectionResponse{
        PeerMessage message;
        PeerConnection connection;

        public ConnectionResponse(PeerMessage message, PeerConnection connection) {
            this.message = message;
            this.connection = connection;
        }
    }

    static final int MAX_CONNECTIONS = 10;
    final String myId;

    String[][] peersInfo;
    PeerConnection[] peerConnections;
    Torrent torrentFile;
    PeerMessageHandler messageHandler;
    int availablePeers;


    public Swarm(byte[] rawPeersInfo, Torrent torrentFile, String myId){
        this.myId = myId;
        this.peersInfo = ProtocolUtils.toPeersString(rawPeersInfo);
        this.peerConnections = new PeerConnection[peersInfo.length];
        this.torrentFile = torrentFile;
        this.messageHandler = new PeerMessageHandler();

        for (int i = 0; i < peersInfo.length && i < MAX_CONNECTIONS; i++){
            peerConnections[i] = new PeerConnection( new Handshake(torrentFile, myId),
                    peersInfo[i][0],
                    Integer.parseInt(peersInfo[i][1]));
            availablePeers++;
        }
    }

    public boolean downloadPiece(PieceDownloader pieceDownloader){
        try{
            while (!pieceDownloader.isCompleted()){
                ConnectionResponse response = getMessage();

                if (response != null){
                    messageHandler.handle(response.message,
                            new PeerContext(
                                    response.connection.getState(),
                                    torrentFile,
                                    response.connection.getOut(),
                                    pieceDownloader
                            )
                    );
                }
                else{
                    System.out.println("No Response Received.");
                    if (availablePeers == 0)
                        return false;
                }
            }
            pieceDownloader.saveToDisk();

        } catch (Exception e){
            return false;
        }

        return true;
    }

    // TODO: Needed to be enhanced to allow simultaneous downloading from more than a single peer.
    public boolean downloadFile(FileDownloader fileDownloader){
        try{
            while (!fileDownloader.isCompleted()){
                ConnectionResponse response = getMessage();

                if (response != null) {
                    // To handel message, we need to pass message + context (state, torrentFile, os, piece downloader)
                    // So, to download a piece we must have piece downloader for it
                    messageHandler.handle(
                            response.message,
                            new PeerContext(
                                    response.connection.getState(),
                                    torrentFile, response.connection.getOut(),
                                    fileDownloader
                            )
                    );
                }
                else{
                    System.out.println("No Response Received.");
                    if (availablePeers == 0)
                        return false;
                }
            }
            fileDownloader.saveToDisk();

        } catch (Exception e){
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }

    // CAUTION: Do NOT request a block you already requested too many times
    public ConnectionResponse getMessage(){
        for (int i = 0; i < peerConnections.length; i++) {
            if (peerConnections[i] == null)
                continue;

            try {
                PeerMessage receivedMessage = NetworkUtils.readPeerMessage(peerConnections[i].getIn());

                if (receivedMessage != null) {
                    return new ConnectionResponse(receivedMessage, peerConnections[i]);
                }

            } catch (IOException e) {
                    peerConnections[i] = null;
                    availablePeers--;
                    System.out.println(e.getMessage());
            }
        }
        return null;
    }

    public void destroy() {
        try{
            for (PeerConnection connection: peerConnections) {
                if (connection != null)
                    connection.closeConnection();
            }
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}