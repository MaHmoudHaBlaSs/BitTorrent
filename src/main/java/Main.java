import com.google.gson.Gson;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import utils.EncodingUtils;
import utils.NetworkUtils;
import utils.ProtocolUtils;
import utils.Torrent;

public class Main {
    private static final Gson gson = new Gson();
    private static final String myId = "-JT0001-000000000000";

    public static void main(String[] args) throws Exception {
        System.err.println("Logs from your program will appear here!");

        String command = args[0];
        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            Object decoded = EncodingUtils.decodeBencode(bencodedValue);
            System.out.println(gson.toJson(decoded));
        }
        else if ("info".equals(command)){
            infoCommand(args[1]);
        }
        else if ("peers".equals(command)){
            peersCommand(args[1]);
        }
        else if ("handshake".equals(command)){
            String[] peerInfo = args[2].split(":");
            handshakeCommand(args[1], peerInfo[0], peerInfo[1]);
        }
        else if ("download_piece".equals(command)){

        }
        else {
            System.out.println("Unknown command: " + command);
        }

    }


    public static void infoCommand(String torrentPath){
        try {
            Torrent torrentFile = new Torrent(Files.readAllBytes(Path.of(torrentPath)));

            System.out.println("Tracker URL: " + torrentFile.getAnnounce());
            System.out.println("Length: " + torrentFile.getFileLength());
            System.out.println("Info Hash: " + torrentFile.getInfoHashHex());
            System.out.println("Piece Length: " + torrentFile.getPieceLength());
            System.out.println("Piece Hashes: ");
            for (String piece: torrentFile.getHexShaPieces())
                System.out.println(piece);

        } catch (IOException e){
            System.err.println(e.getMessage());
        }
    }

    public static void peersCommand(String torrentPath){
        try{
            Torrent torrentFile = new Torrent(Files.readAllBytes(Path.of(torrentPath)));

            String announce = torrentFile.getAnnounce();
            String infoHashString = torrentFile.getInfoHashHex();
            String infoHashURL = EncodingUtils.hexStringToURL(infoHashString);
            long left = torrentFile.getFileLength();

            String requestUrl = ProtocolUtils.buildURL(
                    announce, infoHashURL, myId, 6881, 0, 0, left, 1);

            // The tracker's response is a bencoded dictionary with two keys (interval and peers).
            byte[] trackerResponse = NetworkUtils.requestAvailablePeers(requestUrl);

            if (trackerResponse != null){

                ByteBuffer buffer = (ByteBuffer) EncodingUtils.rawDecodeDict(trackerResponse).get("peers");
                byte[] peers = new byte[buffer.remaining()];
                buffer.get(peers);

                // Each 6-Bytes group represents IP + Port of a pair.
                for (int i = 0; i < peers.length; i += 6){
                    // Java never changes bits — you change the interpretation.
                    // By default, Java treats (interprets) byte as a signed value
                    // print(0xA2) = -94 while it's 162 so we need to convert them to unsigned by adding 0xFF
                    System.out.print((peers[i] & 0xff)+ "." +(peers[i+1] & 0xff)+ "." +(peers[i+2] & 0xff)
                            + "." +(peers[i+3] & 0xff)
                            + ":" + Math.round((peers[i+4] & 0xff) * Math.pow(16, 2) + (peers[i+5] & 0xff)));
                    System.out.println();
                }
            }
            else {
                System.out.println("Error in sending GET request");
            }

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void handshakeCommand(String torrentPath, String peerIP, String peerPort){
        try{
            // TODO: Refactor to use utils.Torrent
            Torrent torrentFile = new Torrent(Files.readAllBytes(Path.of(torrentPath)));

            byte[] handshake = ProtocolUtils.buildPeerHandshake(torrentFile, myId);

            byte[] responseHandshake = NetworkUtils.sendAndReceiveHandshake(handshake, peerIP, peerPort);

            assert responseHandshake != null;
            System.out.println("Peer ID: "
                    + HexFormat.of().formatHex(Arrays.copyOfRange(responseHandshake, 48, 68)));

        } catch (IOException e){
            System.err.println(e.getMessage());
        } catch (AssertionError e){
            System.out.println("Response Handshake is Empty!!");
        }
    }

}
