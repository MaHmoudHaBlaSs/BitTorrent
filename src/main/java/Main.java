import com.dampcake.bencode.Type;
import com.google.gson.Gson;


import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;

import com.dampcake.bencode.Bencode;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.digest.DigestUtils;

import javax.net.ssl.HttpsURLConnection;

public class Main {
    private static final Bencode bencode = new Bencode();
    private static final Gson gson = new Gson();
    private static String peerId = "-JT0001-000000000000";

    public static void main(String[] args) throws Exception {
        System.err.println("Logs from your program will appear here!");

        String command = args[0];
        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            Object decoded;
            try {
                decoded = decodeBencode(bencodedValue);
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(gson.toJson(decoded));
        }
        else if ("info".equals(command)){
            byte[] torrentBytes = Files.readAllBytes(Path.of(args[1]));

            Map<String, Object> meta = bencode.decode(torrentBytes, Type.DICTIONARY);
            Map<String, Object> info = (Map<String, Object>) meta.get("info");

            // Extracting SHA1 pieces
            String[] shaPieces = extractShaPieces(torrentBytes);

            System.out.println("Tracker URL: " + meta.get("announce"));
            System.out.println("Length: " + info.get("length"));
            System.out.println("Info Hash: " + DigestUtils.sha1Hex(getInfoBytes(torrentBytes)));
            System.out.println("Piece Length: " + info.get("piece length"));
            System.out.println("Piece Hashes: ");
            for (String piece: shaPieces)
                System.out.println(piece);
        }
        else if ("peers".equals(command)){
            byte[] torrentBytes = Files.readAllBytes(Path.of(args[1]));
            Map<String, Object> meta = bencode.decode(torrentBytes, Type.DICTIONARY);
            Map<String, Object> info = (Map<String, Object>) meta.get("info");

            //  GET: /announce?peer_id=aaaaaaaaaaaaaaaaaaaa&info_hash=aaaaaaaaaaaaaaaaaaaa
            //  &port=6881&left=0&downloaded=100&uploaded=0&compact=1

            String announce = (String) meta.get("announce");
            String infoHashString = DigestUtils.sha1Hex(getInfoBytes(torrentBytes));
            String infoHashURL = hexStringToURL(infoHashString);

            int port = 6881;
            int uploaded = 0;
            int downloaded = 0;
            long left = (Long) info.get("length");
            int compact = 1;

            String requestUrl = String.format(
                    "%s?info_hash=%s&peer_id=%s&port=%d&uploaded=%d&downloaded=%d&left=%d&compact=%d",
                    announce, infoHashURL, peerId, port, uploaded, downloaded, left, compact);
            try{
                URL url = new URI(requestUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int responseCode = connection.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK){
                    // The tracker's response is a bencoded dictionary with two keys (interval and peers).
                    byte[] responseBytes = connection.getInputStream().readAllBytes();

                    Bencode rawBencode = new Bencode(true);
                    ByteBuffer buffer = (ByteBuffer) rawBencode.decode(responseBytes, Type.DICTIONARY).get("peers");
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
                    System.err.println("Error in sending GET request");
                }

            } catch (IOException e){
                System.out.println(e.getMessage());
            }

        }
        else if ("handshake".equals(command)){
            byte[] torrentBytes = Files.readAllBytes(Path.of(args[1]));
            String[] peer_info = args[2].split(":");

            try{
                Socket socket = new Socket(peer_info[0], Integer.parseInt(peer_info[1]));

                // Building and sending Handshake
                byte[] handshake = new byte[68];
                handshake[0] = 19; // 1 Byte
                System.arraycopy("BitTorrent protocol".getBytes(StandardCharsets.US_ASCII), 0,
                                handshake, 1, 19); // 19 Byte
                Arrays.fill(handshake, 20, 28, (byte) 0); // 8 Byte
                byte[] infoHash = DigestUtils.sha1(getInfoBytes(torrentBytes));
                System.arraycopy(infoHash, 0, handshake, 28, infoHash.length); // 20 Byte
                System.arraycopy(peerId.getBytes(StandardCharsets.US_ASCII), 0, handshake, 48, 20);

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
                    if (actualRead == -1)
                        throw new IOException("Peer closed connection during handshake");

                    read += actualRead;
                }

                System.out.println("Peer ID: "+HexFormat.of().formatHex(Arrays.copyOfRange(response, 48, 68)));

            } catch (Exception e){
                System.err.println(e.getMessage());
            }

        }
        else {
            System.out.println("Unknown command: " + command);
        }

    }


    // NOTE: Encoding Scheme
    // Integer: -23 => i-23e
    // String: "str" => 3:str
    // List: ["str", -23] => l3:stri-23ee
    // Dict: ["A": 1, "B": "name"] => d1:Ai1e1:B4:namee

    static Object decodeBencode(String bencodedString) {
        EncodingType type = getType(bencodedString.charAt(0));

        switch (type) {
            case EncodingType.STRING -> { // String Decode
                return decodeString(bencodedString);
            }
            case EncodingType.INTEGER -> {
                return decodeInteger(bencodedString);
            }
            case EncodingType.LIST -> {
                return decodeList(bencodedString);
            }
            case EncodingType.DICT -> {
                return decodeDict(bencodedString);
            }
            default -> {
                throw new RuntimeException("Unknown coded value!");
            }
        }
    }

    static EncodingType getType(char flag){
        if (Character.isDigit(flag)) { // String Decode
            return EncodingType.STRING;
        }
        else if (flag == 'i') { // Integer Decode
            return EncodingType.INTEGER;
        }
        else if (flag == 'l') { // List Decode
            return EncodingType.LIST;
        }
        else if (flag == 'd'){
            return EncodingType.DICT;
        }
        else{
            return EncodingType.UNKNOWN;
        }
    }

    static Long decodeInteger(String bencodedString){
        int end = 1;
        while (bencodedString.charAt(end) != 'e')
            end++;

        return Long.parseLong(bencodedString.substring(1, end));
    }

    static String decodeString(String bencodedString){
        int end = 1;
        while (bencodedString.charAt(end) != ':')
            end++;
        int length = Integer.parseInt(bencodedString.substring(0, end));

        return bencodedString.substring(end + 1, end + 1 + length);
    }

    static List<Object> decodeList(String bencodedString){
        return bencode.decode(bencodedString.getBytes(), Type.LIST);
    }

    static Map<String, Object> decodeDict(String bencodedString){
        // Keys must be strings and sorted Lexicographically.
        // {"hello": 52, "foo":"bar"} would be encoded as: d3:foo3:bar5:helloi52ee
        return bencode.decode(bencodedString.getBytes(), Type.DICTIONARY);
    }

    static byte[] getInfoBytes(byte[] torrentBytes){
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

        Bencode rawBencode = new Bencode(true); // useBytes = true → keep string bytes as is (pieces saved).
        // .decode() method forces lexicographical order by its implementation nature (order saved).
        byte[] infoBytes = rawBencode.encode(
                (Map<String, Object>) rawBencode.decode(torrentBytes, Type.DICTIONARY).get("info"));

        return infoBytes;
    }

    static String[] extractShaPieces(byte[] torrentBytes){
        Bencode rawBencode = new Bencode(true);
        byte[] infoBytes = rawBencode.encode(
                (Map<String, Object>)rawBencode.decode(torrentBytes, Type.DICTIONARY).get("info"));

        // You can run the debugger to check that the Object returned is actually a ByteBuffer
        ByteBuffer buffer = (ByteBuffer) rawBencode.decode(infoBytes, Type.DICTIONARY).get("pieces");
        byte[] rawPiecesHashes = new byte[buffer.remaining()];
        buffer.get(rawPiecesHashes);

        // Each SHA1 code is length fixed to 20 Byte which is 40 Hex Character.
        String piecesHexStr = HexFormat.of().formatHex(rawPiecesHashes);
        String[] shaPieces = new String[piecesHexStr.length() / 40];
        for (int i = 0; i < shaPieces.length; i+= 1){
            shaPieces[i] = piecesHexStr.substring(i * 40, i * 40 + 40); // Exclusive substring
        }

        return shaPieces;
    }

    static String hexStringToURL(String str){
        // d69f91e6b2ae4c542468d1073a71d4ea13879a7f
        // %d6%9f%91%e6%b2%ae%4c%54%24%68%d1%07%3a%71%d4%ea%13%87%9a%7f
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < str.length(); i++){
            if (i % 2 == 0)
                sb.append('%').append(str.charAt(i));
            else
                sb.append(str.charAt(i));
        }
        return sb.toString();
    }

}
