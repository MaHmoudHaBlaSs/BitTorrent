package protocol;

import utils.Torrent;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class Handshake {
    private byte length;
    private byte[] bittorrent;
    private byte[] reserved;
    private byte[] infoHash;
    private byte[] peerId;
    private byte[] rawHandshake;

    // For sending
    public Handshake(Torrent torrentFile, String myId){
        // US_ASCII guarantees us that each character is converted to exact 1 byte.
        length = 19; // 1 Byte
        bittorrent = "BitTorrent protocol".getBytes(StandardCharsets.US_ASCII); // 19 Bytes

        // eight reserved bytes, which are all set to zero (8 bytes)
        reserved = new byte[8];

        infoHash = torrentFile.getRawInfoHash(); // 20 Bytes
        peerId = myId.getBytes(StandardCharsets.US_ASCII); // 20 Bytes

        rawHandshake = new byte[68];
        fillRawHandshake();
    }

    // For reading
    public Handshake(byte[] rawHandshake){
        length = rawHandshake[0];
        bittorrent = Arrays.copyOfRange(rawHandshake, 1, 20);
        reserved = Arrays.copyOfRange(rawHandshake, 20, 28);
        infoHash = Arrays.copyOfRange(rawHandshake, 28, 48);
        peerId = Arrays.copyOfRange(rawHandshake, 48, 68);
        this.rawHandshake = rawHandshake;
    }

    private void fillRawHandshake(){
        // US_ASCII guarantees us that each character is converted to exact 1 byte.
        rawHandshake[0] = length; // 1 Byte
        System.arraycopy(bittorrent, 0, rawHandshake, 1, 19); // 19 Byte
        System.arraycopy(reserved, 0, rawHandshake, 20, 8); // 8 Bytes
        System.arraycopy(infoHash, 0, rawHandshake, 28, 20); // 20 Bytes
        System.arraycopy(peerId, 0, rawHandshake, 48, 20); // 20 Bytes
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public byte[] getRawHandshake() {
        return Objects.requireNonNullElse(rawHandshake, new byte[68]);
    }

    public byte[] getPeerId() {
        return peerId;
    }
}