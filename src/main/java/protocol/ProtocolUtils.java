package protocol;

import files.Block;
import utils.EncodingUtils;
import utils.Torrent;

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

    public static byte[] makeRequestPayload(int pieceIndex, int begin, int length){
        byte[] payload = new byte[12];
        System.arraycopy(EncodingUtils.convertIntToBytes(pieceIndex), 0, payload, 0, 4);
        System.arraycopy(EncodingUtils.convertIntToBytes(begin), 0, payload, 4, 4);
        System.arraycopy(EncodingUtils.convertIntToBytes(length), 0, payload, 8, 4);
        return payload;
    }

    public static Block extractBlockFromPayload(byte[] payload){
        // * <4-Bytes index> <4-Bytes begin> <n-Bytes block_data>
        int index = EncodingUtils.convertBytesToInt(Arrays.copyOfRange(payload, 0, 4));
        int begin = EncodingUtils.convertBytesToInt(Arrays.copyOfRange(payload, 4, 8));
        byte[] data = Arrays.copyOfRange(payload, 8, payload.length);
        return new Block(index, begin, data.length, data);
    }

    public static byte[] extractPieceShaHash(byte[] piecesShaHash, int index){
        return Arrays.copyOfRange(piecesShaHash, index * 20, (index * 20) + 20);
    }

    public static String getIpFromBytes (byte[] raw){
        return (raw[0] & 0xff)+ "." +(raw[1] & 0xff)+ "." +(raw[2] & 0xff) + "." +(raw[3] & 0xff);
    }

    public static String getPortFromBytes (byte[] raw){
        return String.valueOf(Math.round((raw[0] & 0xff) * Math.pow(16, 2) + (raw[1] & 0xff)));
    }

    public static String[][] toPeersString(byte[] rawPeers){
        String[][] peers = new String[rawPeers.length / 6][2];

        for (int i = 0; i < rawPeers.length / 6; i++ ){
            peers[i][0] = getIpFromBytes(Arrays.copyOfRange(rawPeers, i*6, i*6 +4));
            peers[i][1] = getPortFromBytes(Arrays.copyOfRange(rawPeers, i*6 + 4, i*6 + 6));
        }
        return peers;
    }
}
