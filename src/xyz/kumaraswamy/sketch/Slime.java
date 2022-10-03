package xyz.kumaraswamy.sketch;

import xyz.kumaraswamy.sketch.lex.TokenType;
import xyz.kumaraswamy.sketch.lex.Lexer;
import xyz.kumaraswamy.sketch.lex.Token;
import xyz.kumaraswamy.sketch.memory.GlobalMemory;
import xyz.kumaraswamy.sketch.processor.Expression;
import xyz.kumaraswamy.sketch.processor.Parser;
import xyz.kumaraswamy.sketch.processor.Evaluator;

import java.util.StringJoiner;

public class Slime {

    private final Evaluator executor;

    public Slime() {
        executor = new Evaluator(new GlobalMemory());
    }

    public void execute(String source) {
        Lexer lexer = new Lexer(source);

        Parser parser = new Parser(lexer.scanTokens());
        for (Expression expression : parser.parse()) {
             // System.out.println(expression.visit());
            // uncomment to view the tree
            expression.accept(executor);
        }
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where,
                               String message) {
        throw new IllegalArgumentException(  "[line " + line + "] Error" + where + ": " + message);
    }

    public static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
    }
}
