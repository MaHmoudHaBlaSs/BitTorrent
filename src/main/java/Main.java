import com.google.gson.Gson;

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
// import com.dampcake.bencode.Bencode; - available if you need it!

public class Main {
    private static final Gson gson = new Gson();

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
            int firstColonIndex = 0;
            for (int i = 0; i < bencodedString.length(); i++) {
                if (bencodedString.charAt(i) == ':') {
                    firstColonIndex = i;
                    break;
                }
            }
            int length = Integer.parseInt(bencodedString.substring(0, firstColonIndex));
            return bencodedString.substring(firstColonIndex + 1, firstColonIndex + length + 1);

        } else if (flag == 'i') { // Integer Decode
            int end = 1;
            while (bencodedString.charAt(end) != 'e')
                end++;

            return Long.parseLong(bencodedString.substring(1, end));

        } else if (flag == 'l') { // List
            LinkedList<Object> list = new LinkedList<>();

            for (int i = 1; i < bencodedString.length(); i++) {
                if (Character.isDigit(bencodedString.charAt(i))) {
                    int end = 0;
                    while (bencodedString.charAt(end) != ':')
                        end++;
                    int length = Integer.parseInt(bencodedString.substring(i, end));
                    list.add(bencodedString.substring(end + 1, end + length + 1));
                    i = end + length;

                } else if (bencodedString.charAt(i) == 'i') {
                    int end = i+1;
                    while (bencodedString.charAt(end) != 'e')
                        end++;
                    list.add(Long.parseLong(bencodedString.substring(i + 1, end)));
                    i = end;

                }
            }
            return list;

        } else {
            throw new RuntimeException("Only strings are supported at the moment");
        }
    }
}
