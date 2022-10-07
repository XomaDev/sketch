package xyz.kumaraswamy.sketch.lex;

public enum TokenType {
    VAL, FUN, RETURN,
    IF, ELSE, WHILE, FOR,
    LEFT_RIGHT, RIGHT_LEFT,
    // inline
    THEN, OR,
    COMMA, SEMICOLON,
    MINUS, EXCLAMATION, PERCENTAGE,
    INCREMENT, DECREMENT, PLUS, SLASH, STAR,
    NULL, TRUE, FALSE, EQUAL, NOT_EQUAL, EQUAL_EQUAL, ABOVE, BELOW,
    THIS, EACH,
    ABOVE_EQUAL, BELOW_EQUAL,
    BITWISE_OR, BITWISE_AND,
    LOGICAL_OR, LOGICAL_AND,
    BREAK, CONTINUE, FORWARD,

    STRING, NUMBER, DOT, IDENTIFIER,
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    LEFT_SQUARE, RIGHT_SQUARE,
    // data types
    LEV,
    // with statement
    // with sketch.random : random
    WITH,
    EOF;
}
