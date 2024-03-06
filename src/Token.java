public class Token {

    public enum Type {
        IDENTIFIER,
        NUMBER,
        STRING,

        SPACE(" "),
        NEWLINE("\n"),
        TAB("\t"),
        CARRIAGE_RETURN("\r"),

        COLON(":"),
        SEMI_COLON(";"),
        FORWARD_ARROW("->"),
        BACKWARDS_ARROW("<-"),

        OPEN_PARENTHESIS("("),
        CLOSE_PARENTHESIS(")"),
        OPEN_BRACE("{"),
        CLOSE_BRACE("}"),

        COMMA(","),
        DOT("."),

        PLUS("+"),
        MINUS("-"),
        STAR("*"),
        FORWARD_SLASH("/"),
        EQUALS("="),

        WHILE("while"),
        FOR("for"),
        IF("if"),
        ELSE("else"),
        STRUCT("struct"),
        TRUE("true"),
        FALSE("false");

        String text;
        Type(){
            text = null;
        }
        Type(String text){
            this.text = text;
        }

        public boolean is_whitespace(){
            return switch (this){
                case NEWLINE, SPACE, TAB, CARRIAGE_RETURN -> true;
                default -> false;
            };
        }

        public boolean is_keyword(){
            return switch (this){
                case WHILE, FOR, IF, ELSE, STRUCT, TRUE, FALSE -> true;
                default -> false;
            };
        }
    }

    public Type type;
    public int index;
    public int length;
}
