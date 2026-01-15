import com.google.gson.Gson;
// import com.dampcake.bencode.Bencode; - available if you need it!

public class Main {
  private static final Gson gson = new Gson();

  public static void main(String[] args) throws Exception {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");
    
    String command = args[0];
    if("decode".equals(command)) {
        String bencodedValue = args[1];
        String decoded;
        try {
          decoded = decodeBencode(bencodedValue);
        } catch(RuntimeException e) {
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

  static String decodeBencode(String bencodedString) {
    char flag = bencodedString.charAt(0);

    if (Character.isDigit(flag)) {
      int firstColonIndex = 0;
      for(int i = 0; i < bencodedString.length(); i++) { 
        if(bencodedString.charAt(i) == ':') {
          firstColonIndex = i;
          break;
        }
      }
      int length = Integer.parseInt(bencodedString.substring(0, firstColonIndex));
      return bencodedString.substring(firstColonIndex+1, firstColonIndex+1+length);
    }
    else if (flag == 'i') {
      int end;
      for (end = 1; end < bencodedString.length(); end++){
        if (bencodedString.charAt(end) == 'e')
          break;
      }
      return bencodedString.substring(1, end);
    }
    else {
      throw new RuntimeException("Only strings are supported at the moment");
    }
  }
  
}
