package xyz.kumaraswamy.sketch.lex;

import xyz.kumaraswamy.sketch.Slime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("true",      TokenType.TRUE);
        keywords.put("false",     TokenType.FALSE);
        keywords.put("null",      TokenType.NULL);

        keywords.put("this",      TokenType.THIS);

        keywords.put("val",       TokenType.VAL);
        keywords.put("fun",       TokenType.FUN);
        keywords.put("if",        TokenType.IF);
        keywords.put("else",      TokenType.ELSE);
        keywords.put("then",      TokenType.THEN);
        keywords.put("or",        TokenType.OR);
        keywords.put("for",       TokenType.FOR);
        keywords.put("while",     TokenType.WHILE);

        keywords.put("return",    TokenType.RETURN);
        keywords.put("break",     TokenType.BREAK);
        keywords.put("continue",  TokenType.CONTINUE);
        keywords.put("forward",   TokenType.FORWARD);

        // value types
        keywords.put("lev",       TokenType.LEV);
    }

    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            case '+': addToken(match('+') ? TokenType.INCREMENT : TokenType.PLUS); break;
            case '-': {
                minusChar();
            } break;
            case '/': addToken(TokenType.SLASH);break;
            case '*': addToken(TokenType.STAR); break;
            case '%': addToken(TokenType.PERCENTAGE); break;
            case '!': addToken(match('=') ? TokenType.NOT_EQUAL : TokenType.EXCLAMATION); break;
            case ',': addToken(TokenType.COMMA); break;
            case '.': addToken(TokenType.DOT); break;
            case ';': addToken(TokenType.SEMICOLON); break;
            case '(': addToken(TokenType.LEFT_PAREN); break;
            case ')': addToken(TokenType.RIGHT_PAREN); break;
            case '{': addToken(TokenType.LEFT_BRACE); break;
            case '}': addToken(TokenType.RIGHT_BRACE); break;
            case '>': addToken(match('=') ? TokenType.ABOVE_EQUAL : TokenType.ABOVE); break;
            case '<': {
                belowChar();
            } break;
            case '&': addToken(match('&') ? TokenType.LOGICAL_AND : TokenType.BITWISE_AND); break;
            case '|': addToken(match('|') ? TokenType.LOGICAL_OR : TokenType.BITWISE_OR); break;
            case ' ':
            case '\r':
            case '\t':
                // Ignore whitespace.
                break;
            case '=':
                addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.EQUAL);
                break;
            case '"': string(); break;
            case '\n':
                line++;
                break;
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    Slime.error(line, "Unexpected character \"" + c + "\"");
                }
        }
    }

    private void belowChar() {
        if (isAtEnd()) return;
        char ch = source.charAt(current);
        if (ch == '=')  {
            current++;
            addToken(TokenType.BELOW_EQUAL);
        } else if (ch == '-') {
            current++;
            addToken(TokenType.RIGHT_LEFT);
        } else {
            addToken(TokenType.BELOW);
        }
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == TokenType.TRUE || type == TokenType.FALSE) {
            addToken(type, Boolean.parseBoolean(text));
        } else {
            if (type == null) type = TokenType.IDENTIFIER;
            addToken(type);
        }
    }

    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();
            while (isDigit(peek())) advance();
        }

        addToken(TokenType.NUMBER,
                Double.parseDouble(source.substring(start, current)));
    }

    private void minusChar() {
        if (isAtEnd()) return;
        char ch = source.charAt(current);
        if (ch == '-') {
            current++;
            addToken(TokenType.DECREMENT);
        } else if (ch == '>') {
            current++;
            addToken(TokenType.LEFT_RIGHT);
        } else {
            addToken(TokenType.MINUS);
        }
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;

        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Slime.error(line, "Unterminated string.");
            return;
        }

        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value.translateEscapes());
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    public static void main(String[] args) {
        Lexer lex = new Lexer("val z = 7;");
        System.out.println(lex.scanTokens());
    }
}
