import com.dampcake.bencode.Type;
import com.google.gson.Gson;


import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import com.dampcake.bencode.Bencode;
import org.apache.commons.codec.digest.DigestUtils;

public class Main {
    private static final Bencode bencode = new Bencode();
    private static final Gson gson = new Gson();

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

            // The problem is that when we re encode the decoded info dictionary,
            // we may get different code because Java Map may change the original order,
            // and also we need to take care of `pieces` binary format.

            Bencode rawBencode = new Bencode(true); // keep string bytes as is (pieces saved).
            // .decode() method forces lexicographical order by its implementation nature (order saved).
            byte[] infoBytes = rawBencode.encode(
                    (Map<String, Object>) rawBencode.decode(torrentBytes, Type.DICTIONARY).get("info"));
            String shaHex = DigestUtils.sha1Hex(infoBytes);

            // Extracting SHA1 pieces
            ByteBuffer buffer = (ByteBuffer) rawBencode.decode(infoBytes, Type.DICTIONARY).get("pieces");
            byte[] pieces = new byte[buffer.remaining()];
            buffer.get(pieces);

            String piecesHexStr = HexFormat.of().formatHex(pieces);
            String[] sha1Pieces = new String[piecesHexStr.length() / 40];
            for (int i = 0; i < sha1Pieces.length; i+= 1){
                sha1Pieces[i] = piecesHexStr.substring(i * 40, i * 40 + 40); // Exclusive substring
            }

            System.out.println("Tracker URL: " + meta.get("announce"));
            System.out.println("Length: " + info.get("length"));
            System.out.println("Info Hash: " + shaHex);
            System.out.println("Piece Length: " + info.get("piece length"));
            System.out.println("Piece Hashes: ");
            for (String piece: sha1Pieces)
                System.out.println(piece);
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

    // Keys must be strings and sorted Lexicographically.
    // {"hello": 52, "foo":"bar"} would be encoded as: d3:foo3:bar5:helloi52ee
    static Map<String, Object> decodeDict(String bencodedString){
        return bencode.decode(bencodedString.getBytes(), Type.DICTIONARY);
    }

}
