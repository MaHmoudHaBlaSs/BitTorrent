package utils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ProtocolUtils {

    public static String buildURL(String announce ,String infoHashURL, String peerId, int port,
                                  int uploaded, int downloaded, long left, int compact){

        //  GET Request:
        //  /announce?peer_id=aaaaaaaaaaaaaaaaaaaa&info_hash=aaaaaaaaaaaaaaaaaaaa
        //  &port=6881&left=0&downloaded=0&uploaded=0&compact=1

        return String.format(
                "%s?info_hash=%s&peer_id=%s&port=%d&uploaded=%d&downloaded=%d&left=%d&compact=%d",
                announce, infoHashURL, peerId, port, uploaded, downloaded, left, compact);
    }

    // P2P handshake
    public static byte[] buildPeerHandshake(Torrent torrentFile, String peerId){
        byte[] handshake = new byte[68];

        // US_ASCII guarantees us that each character is converted to exact 1 byte.
        handshake[0] = 19; // 1 Byte
        System.arraycopy("BitTorrent protocol".getBytes(StandardCharsets.US_ASCII), 0,
                handshake, 1, 19); // 19 Byte
        Arrays.fill(handshake, 20, 28, (byte) 0); // 8 Byte

        byte[] infoHash = torrentFile.getRawInfoHash();
        System.arraycopy(infoHash, 0, handshake, 28, infoHash.length); // 20 Byte

        System.arraycopy(peerId.getBytes(StandardCharsets.US_ASCII), 0, handshake, 48, 20);

        return handshake;
    }
}
