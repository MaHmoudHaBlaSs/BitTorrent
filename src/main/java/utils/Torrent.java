package utils;/*
    Main
     ├── utils.Torrent
     ├── protocol.ProtocolUtils
     │     └── utils.EncodingUtils
     └── utils.NetworkUtils
 */

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.ByteBuffer;
import java.util.HexFormat;
import java.util.Map;


public class Torrent {
    private byte[] torrentBytes;
    private byte[] infoBytes;

    private Map<String, Object> meta;
    private Map<String, Object> info;

    private String announce;
    private String comment;
    private String creationDate;
    private long fileLength;
    private long pieceLength;

    private String infoHashHex;
    private byte[] rawInfoHash;

    private byte[] rawShaPieces;

    public Torrent(byte[] torrentBytes){
        this.torrentBytes = torrentBytes;
        this.infoBytes = getInfoBytes();

        meta = EncodingUtils.decodeDict(torrentBytes);
        info = EncodingUtils.decodeDict(infoBytes);

        // Extracting SHA1 pieces
        rawShaPieces = extractShaPieces();
        infoHashHex = DigestUtils.sha1Hex(infoBytes); // Hex String Representation (40 Character)
        rawInfoHash = DigestUtils.sha1(infoBytes); // Raw Bytes Representation (20 Byte)

        announce = (String) meta.get("announce");
        fileLength = (Long) info.get("length");
        pieceLength = (Long) info.get("piece length");
    }

    private byte[] getInfoBytes(){
        /*
         * Problem:
         * Re-encoding the decoded `info` dictionary may not produce identical bytes.
         *
         * Reasons:
         * 1. Java Map does not preserve the original key order by default.
         * 2. The `pieces` field contains raw binary data and must not be treated as text.
         *
         * Impact:
         * Any byte-level change results in a different info-hash.
         */
        // rawDecode keeps string bytes as is (pieces saved).
        // .decode() method forces lexicographical order by its implementation nature (order saved).

        return EncodingUtils.encodeDict(
                (Map<String, Object>) EncodingUtils.rawDecodeDict(torrentBytes).get("info"));
    }

    private byte[] extractShaPieces(){

        // You can run the debugger to check that the Object returned is actually a ByteBuffer
        ByteBuffer buffer = (ByteBuffer) EncodingUtils.rawDecodeDict(infoBytes).get("pieces");
        byte[] rawPiecesHashes = new byte[buffer.remaining()];
        buffer.get(rawPiecesHashes);

        return rawPiecesHashes;
    }

    public String[] getHexShaPieces(){
        // Each SHA1 code is length fixed to 20 Byte which is 40 Hex Character.
        String piecesHexStr = HexFormat.of().formatHex(rawShaPieces);

        String[] shaPieces = new String[piecesHexStr.length() / 40];
        for (int i = 0; i < shaPieces.length; i+= 1){
            shaPieces[i] = piecesHexStr.substring(i * 40, i * 40 + 40); // Exclusive substring
        }

        return shaPieces;
    }

    public long getFileLength() {
        return fileLength;
    }
    public long getPieceLength() {
        return pieceLength;
    }
    public String getAnnounce() {
        return announce;
    }
    public String getInfoHashHex() {
        return infoHashHex;
    }
    public byte[] getRawInfoHash() {
        return rawInfoHash;
    }
    public byte[] getRawShaPieces() {
        return rawShaPieces;
    }
    public Map<String, Object> getMeta() {
        return meta;
    }
    public Map<String, Object> getInfo() {
        return info;
    }

}
