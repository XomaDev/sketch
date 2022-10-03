package xyz.kumaraswamy.sketch.processor;

import xyz.kumaraswamy.sketch.lex.Token;

class RuntimeError extends RuntimeException {
  Token token;

  RuntimeError(Token token, String message) {
    super(message);
    this.token = token;
  }

  RuntimeError(String message) {
    super(message);
  }
}