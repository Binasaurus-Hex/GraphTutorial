import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    static boolean matches(String program_text, String search_text, int text_index){
        for(int i = 0; i < search_text.length(); i++){
            char a = program_text.charAt(text_index + i);
            char b = search_text.charAt(i);
            if(a != b)return false;
        }
        return true;
    }

    static boolean is_number(String program_text, int index, int length){
        for(int i = 0; i < length; i++){
            char c = program_text.charAt(index + i);
            if(c < '0' || c > '9')return false;
        }
        return true;
    }

    static List<Token> tokenize(String program_text){
        List<Token> tokens = new ArrayList<>();

        List<Token.Type> search_types = new ArrayList<>();
        List<Token.Type> keyword_types = new ArrayList<>();

        int last_token_index = 0;

        for(Token.Type token_type : Token.Type.values()){
            if(token_type.text != null){
                if(token_type.is_keyword()){
                    keyword_types.add(token_type);
                }
                else{
                    search_types.add(token_type);
                }
            }
        }

        for(int i = 0; i < program_text.length(); i++){

            if(program_text.charAt(i) == '"'){
                int string_search_index = i + 1;
                while(program_text.charAt(string_search_index) != '"'){
                    string_search_index++;
                }
                string_search_index++;

                Token string = new Token();
                string.type = Token.Type.STRING;
                string.index = i;
                string.length = string_search_index - i;

                tokens.add(string);

                i += string.length - 1;
                last_token_index = i + 1;
            }


            for(Token.Type search_type : search_types){
                if(matches(program_text, search_type.text, i)){
                    if(last_token_index < i){
                        Token identifier = new Token();
                        identifier.type = Token.Type.IDENTIFIER;
                        identifier.index = last_token_index;
                        identifier.length = i - last_token_index;

                        for(Token.Type keyword_type : keyword_types){
                            if(matches(program_text, keyword_type.text, identifier.index)){
                                identifier.type = keyword_type;
                                break;
                            }
                        }

                        if(is_number(program_text, identifier.index, identifier.length)){
                            identifier.type = Token.Type.NUMBER;
                        }

                        tokens.add(identifier);
                    }

                    Token token = new Token();
                    token.type = search_type;
                    token.index = i;
                    token.length = search_type.text.length();

                    tokens.add(token);

                    i += token.length - 1;
                    last_token_index = i + 1;
                }
            }
        }

        return tokens;
    }

    public static void main(String[] args) {
        String program_text;

        try {
            program_text = Files.readString(Path.of("res/main.graph"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(program_text);

        List<Token> tokens = tokenize(program_text);

        for(Token token : tokens){
            String type_text = token.type.name();
            String token_value = "";
            if(token.type == Token.Type.IDENTIFIER || token.type == Token.Type.NUMBER || token.type == Token.Type.STRING){
                token_value = program_text.substring(token.index, token.index + token.length);
            }
            System.out.printf("%s %s\n", type_text, token_value);
        }
    }
}