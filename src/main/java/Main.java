import com.dampcake.bencode.Type;
import com.google.gson.Gson;


import java.util.LinkedList;
import java.util.Map;

import com.dampcake.bencode.Bencode; // available if you need it!

public class Main {
    private static final Bencode bencode = new Bencode();
    private static final Gson gson = new Gson();
    private record Pair(Object retVal, Integer end){ }

    public static void main(String[] args) throws Exception {
        // You can use print statements as follows for debugging, they'll be visible when running tests.
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
        } else {
            System.out.println("Unknown command: " + command);
        }

    }

    // NOTE: Encoding Scheme
    // Integer: -23 => i-23e
    // String: "str" => 3:str
    // List: ["str", -23] => l3:stri-23ee
    // Dict: ["A": 1, "B": "name"] => d1:Ai1e1:B4:namee

    static Object decodeBencode(String bencodedString) {
        char flag = bencodedString.charAt(0);

        if (Character.isDigit(flag)) { // String Decode
            Pair ret = decodeString(bencodedString, 0);
            return ret.retVal;
        }
        else if (flag == 'i') { // Integer Decode
            Pair ret = decodeInteger(bencodedString, 0);
            return ret.retVal;
        }
        else if (flag == 'l') { // List Decode
            Pair ret = decodeList(bencodedString, 0);
            return ret.retVal;
        }
        else if (flag == 'd'){
            return decodeDict(bencodedString);
        }
        else {
            throw new RuntimeException("Not supported B-encoded value");
        }
    }
    static Pair decodeInteger(String bencodedString, int start){
        int end = start+1;
        while (bencodedString.charAt(end) != 'e')
            end++;

        return new Pair(Long.parseLong(bencodedString.substring(start+1, end)), end);
    }

    static Pair decodeString(String bencodedString, int start){
        int end = start+1;
        while (bencodedString.charAt(end) != ':')
            end++;
        int length = Integer.parseInt(bencodedString.substring(start, end));

        Object retVal = bencodedString.substring(end + 1, end + length + 1);
        end = end + length; // Last character index in the string
        return new Pair(retVal, end);
    }

    static Pair decodeList(String bencodedString, int start){
        int end = start + 1;
        LinkedList<Object> list = new LinkedList<>();

        for (int i = start + 1 ; i < bencodedString.length(); i++) {

            if (Character.isDigit(bencodedString.charAt(i))) { // String Element
                Pair ret = decodeString(bencodedString, i);
                list.add(ret.retVal);
                i = ret.end;
            }
            else if (bencodedString.charAt(i) == 'i') { // Integer Element
                Pair ret = decodeInteger(bencodedString, i);
                list.add(ret.retVal);
                i = ret.end;

            }
            else if (bencodedString.charAt(i) == 'l'){ // Nested list
                Pair ret = decodeList(bencodedString, i);
                list.add(ret.retVal);
                i = ret.end;
            }
            else if (bencodedString.charAt(i) == 'd'){
                // TODO: Dictionary
            }
            else{ // Character = 'e' → means the end of the list
                end = i;
                break;
            }
        }
        return new Pair(list, end);
    }

    // Keys must be strings and sorted Lexicographically.
    // {"hello": 52, "foo":"bar"} would be encoded as: d3:foo3:bar5:helloi52ee
    static Map<String, Object> decodeDict(String bencodedString){
        return bencode.decode(bencodedString.getBytes(), Type.DICTIONARY);
    }
}
