import Nodes.*;

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
                    break;
                }
            }
        }

        return tokens;
    }

    static void print_error(String text){
        System.out.println(text);
        System.exit(1);
    }

    static class Parser {
        String program_text;
        List<Token> tokens;
        int token_index;

        Token eat_token(){
            while(token_index < tokens.size()){
                Token current_token = tokens.get(token_index++);
                if(!current_token.type.is_whitespace())return current_token;
            }
            return null;
        }

        Token peek_token(int lookahead){
            int peek_index = token_index;
            int found_tokens = 0;
            while (peek_index < tokens.size()){
                Token current_token = tokens.get(peek_index++);
                if(!current_token.type.is_whitespace()) found_tokens++;
                if(found_tokens == lookahead)return current_token;
            }
            return null;
        }
        Token peek_token(){ return peek_token(1); }

        String get_token_value(Token token){
            return program_text.substring(token.index, token.index + token.length);
        }
    }

    static BinaryOperator.Operation to_binary_operation(Token token){
        return switch (token.type){
            case PLUS -> BinaryOperator.Operation.ADD;
            case MINUS -> BinaryOperator.Operation.SUBTRACT;
            case STAR -> BinaryOperator.Operation.MULTIPLY;
            case FORWARD_SLASH -> BinaryOperator.Operation.DIVIDE;
            default -> null;
        };
    }

    static Node parse_subexpression(Parser parser){
        Token next = parser.eat_token();
        if(next.type == Token.Type.IDENTIFIER){
            if(parser.peek_token().type == Token.Type.OPEN_PARENTHESIS){
                ProcedureCall procedure_call = new ProcedureCall();
                procedure_call.name = parser.get_token_value(next);
            }
            VariableCall variable_call = new VariableCall();
            variable_call.name = parser.get_token_value(next);
            return variable_call;
        }
        else if(next.type == Token.Type.NUMBER){
            if(parser.peek_token().type == Token.Type.DOT){
                parser.eat_token();
                int float_length = next.length;
                if(parser.peek_token().type == Token.Type.NUMBER){
                    Token decimal = parser.eat_token();
                    float_length += 1 + decimal.length;
                }
                String float_string = parser.program_text.substring(next.index, next.index + float_length);
                return new Literal<>(Float.parseFloat(float_string));
            }
            return new Literal<>(Integer.parseInt(parser.get_token_value(next)));
        }
        else if(next.type == Token.Type.OPEN_PARENTHESIS){

        }
        return null;
    }

    static Node parse_increasing_precedence(Parser parser, Node left, int min_precedence){
        if(!parser.peek_token().type.is_binary_operator())return left;
        BinaryOperator.Operation operation = to_binary_operation(parser.peek_token());
        if(operation.ordinal() <= min_precedence) return left;

        parser.eat_token();

        BinaryOperator binary_operator = new BinaryOperator();
        binary_operator.operation = operation;
        binary_operator.left = left;
        binary_operator.right = parse_expression(parser, operation.ordinal());
        return binary_operator;
    }

    static Node parse_expression(Parser parser, int precedence){
        Node left = parse_subexpression(parser);
        while (true){
            Node right = parse_increasing_precedence(parser, left, precedence);
            if(left == right)break;
            left = right;
        };
        return left;
    }

    static Node parse_expression(Parser parser){
        return parse_expression(parser, Integer.MIN_VALUE);
    }

    static Node parse_type(Parser parser){
        Token next = parser.peek_token();
        if(next.type == Token.Type.IDENTIFIER){
            Token identifier = parser.eat_token();
            String identifier_text = parser.get_token_value(identifier);
            PrimitiveType.Type primitive_type = switch (identifier_text){
                case "float" -> PrimitiveType.Type.FLOAT;
                case "int" -> PrimitiveType.Type.INT;
                case "bool" -> PrimitiveType.Type.BOOL;
                default -> null;
            };
            if(primitive_type == null){
                StructType structType = new StructType();
                structType.name = identifier_text;
                return structType;
            }
            PrimitiveType primitive = new PrimitiveType();
            primitive.type = primitive_type;
            return primitive;
        }
        return null;
    }

    interface ParseFunction {
        Node parse(Parser parser);
    }

    static List<Node> parse_variadic(Parser parser, ParseFunction parse_function){
        if(parser.peek_token().type != Token.Type.OPEN_PARENTHESIS){
            print_error("variadic must start with open parenthesis");
        }
        parser.eat_token();

        List<Node> nodes = new ArrayList<>();
        while (parser.peek_token() != null && parser.peek_token().type != Token.Type.CLOSE_PARENTHESIS){
            Node node = parse_function.parse(parser);
            nodes.add(node);
            if(parser.peek_token().type != Token.Type.COMMA)break;
            parser.eat_token();
        }

        if(parser.peek_token().type != Token.Type.CLOSE_PARENTHESIS){
            print_error("variadic must end with close parenthesis");
        }
        parser.eat_token();
        return nodes;
    }

    static Node parse_statement(Parser parser){
        Token start = parser.peek_token();
        if(start.type == Token.Type.IDENTIFIER){
            Token next = parser.peek_token(2);
            if(next.type == Token.Type.COLON){
                Token name = parser.eat_token();
                String name_string = parser.get_token_value(name);
                parser.eat_token();

                next = parser.peek_token();
                if(next.type == Token.Type.COLON){
                    // constant decl
                    parser.eat_token();
                    next = parser.peek_token();
                    if(next.type == Token.Type.STRUCT){
                        parser.eat_token();
                        Struct struct = new Struct();
                        struct.name = name_string;
                        struct.body = parse_block(parser);
                        return struct;
                    }
                    else if(next.type == Token.Type.OPEN_PARENTHESIS){
                        Procedure procedure = new Procedure();
                        procedure.name = name_string;
                        procedure.inputs = parse_variadic(parser, Main::parse_statement);
                        if(parser.peek_token().type == Token.Type.FORWARD_ARROW){
                            parser.eat_token();
                            Node type = parse_type(parser);
                            if(type == null){
                                print_error("procedures that define a return must specify a return type");
                            }
                            procedure.output = type;
                        }
                        procedure.body = parse_block(parser);
                        return procedure;
                    }
                    else{
                        print_error("constant declaration must be either a struct or procedure");
                    }
                }
                else{
                    VariableDeclaration var_decleration = new VariableDeclaration();
                    var_decleration.name = name_string;
                    var_decleration.type = parse_type(parser);
                    return var_decleration;
                }
            }
            else{
                // expression
            }
        }
        else if(start.type.is_keyword()){

        }
        else if(start.type == Token.Type.BACKWARDS_ARROW){
            parser.eat_token();
            ReturnStatement statement = new ReturnStatement();
            statement.value = parse_expression(parser);
            return statement;
        }
        else{
            parse_expression(parser);
        }
        return null;
    }

    static List<Node> parse_block(Parser parser){
        Token next = parser.peek_token();
        if(next.type != Token.Type.OPEN_BRACE){
            print_error("block must start with open brace");
        }
        parser.eat_token();

        List<Node> block = new ArrayList<>();

        while (parser.peek_token() != null && parser.peek_token().type != Token.Type.CLOSE_BRACE){
            Node statement = parse_statement(parser);
            next = parser.peek_token();
            if(next.type != Token.Type.SEMI_COLON){
                print_error("missing semi colon");
            }
            parser.eat_token();

            block.add(statement);
        }

        if(parser.peek_token() == null){
            print_error("block must end with a close brace");
        }
        parser.eat_token();

        return block;
    }

    static List<Node> parse_program(Parser parser){
        List<Node> program = new ArrayList<>();
        while (parser.peek_token() != null){
            program.add(parse_statement(parser));
        }
        return program;
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

        Parser parser = new Parser();
        parser.program_text = program_text;
        parser.tokens = tokens;

        List<Node> program = parse_program(parser);
        System.out.printf("");
    }
}