package xyz.kumaraswamy.sketch.lex;

public class Token {


    public final TokenType type;
    public final String lexeme;
    public final Object literal;
    public final int line;

    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    public boolean matches(TokenType... types) {
        for (TokenType type : types) {
            if (this.type == type) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        if (literal == null) {
            return lexeme != null
                    ? "[" + type.name() +
                        (lexeme.isEmpty() ? "" : ", " + lexeme) + "]"
                    : type.name();
        }
        return String.valueOf(literal);
    }
}