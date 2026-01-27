package utils;

import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;

import java.util.List;
import java.util.Map;

public class EncodingUtils {
    private static final Bencode bencode = new Bencode();
    // useBytes = true → keep string bytes as is (pieces saved).
    private static final Bencode rawBencode = new Bencode(true);

    private enum EncodingType{
        STRING,
        INTEGER,
        LIST,
        DICT,
        UNKNOWN
    }

    // NOTE: Encoding Scheme
    // Integer: -23 => i-23e
    // String: "str" => 3:str
    // List: ["str", -23] => l3:stri-23ee
    // Dict: ["A": 1, "B": "name"] => d1:Ai1e1:B4:namee

    public static Object decodeBencode(String bencodedString) {
        EncodingType type = getType(bencodedString.charAt(0));

        try{
            switch (type) {
                case EncodingType.STRING -> {
                    return bencode.decode(bencodedString.getBytes(), Type.STRING);
                }
                case EncodingType.INTEGER -> {
                    return bencode.decode(bencodedString.getBytes(), Type.NUMBER);
                }
                case EncodingType.LIST -> {
                    return bencode.decode(bencodedString.getBytes(), Type.LIST);
                }
                case EncodingType.DICT -> {
                    return bencode.decode(bencodedString.getBytes(), Type.DICTIONARY);
                }
                default -> {
                    throw new RuntimeException("Unknown coded value!");
                }
            }
        } catch (Exception e){
            System.err.println(e.getMessage());
            return new Object();
        }

    }

    private static EncodingType getType(char flag){
        if (Character.isDigit(flag)) {
            return EncodingType.STRING;
        }
        else if (flag == 'i') {
            return EncodingType.INTEGER;
        }
        else if (flag == 'l') {
            return EncodingType.LIST;
        }
        else if (flag == 'd'){
            return EncodingType.DICT;
        }
        else{
            return EncodingType.UNKNOWN;
        }
    }

    static List<Object> decodeList(byte[] bencodedBytes){
        //
        return bencode.decode(bencodedBytes, Type.LIST);
    }

    static Map<String, Object> decodeDict(byte[] bencodedBytes){
        // Keys must be strings and sorted Lexicographically.
        // {"hello": 52, "foo":"bar"} would be encoded as: d3:foo3:bar5:helloi52ee
        return bencode.decode(bencodedBytes, Type.DICTIONARY);
    }

    public static Map<String, Object> rawDecodeDict(byte[] bencodedBytes){
        return rawBencode.decode(bencodedBytes, Type.DICTIONARY);
    }

    static byte[] encodeDict(Map<String, Object> dict){
        try{
            return bencode.encode(dict);
        } catch (Exception e){
            System.err.println(e.getMessage());
            return null;
        }
    }

    public static String hexStringToURL(String str){
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
