package me.blvckbytes.minimalparser.parser;

import lombok.AllArgsConstructor;
import me.blvckbytes.minimalparser.error.AParserError;
import me.blvckbytes.minimalparser.error.UnexpectedTokenError;
import me.blvckbytes.minimalparser.parser.expression.*;
import me.blvckbytes.minimalparser.tokenizer.ITokenizer;
import me.blvckbytes.minimalparser.tokenizer.Token;
import me.blvckbytes.minimalparser.tokenizer.TokenType;
import org.jetbrains.annotations.Nullable;

/**
 * This parser uses the compact and flexible algorithm called "precedence climbing" / "top down
 * recursive decent", which of course is not highly efficient. The main purpose of this project is
 * to parse expressions within configuration files once and then just evaluate the AST within the
 * desired evaluation context at runtime over and over again. Due to the ahead-of-time nature of
 * this intended use-case, efficiency at the level of the parser is sacrificed for understandability.
 */
@AllArgsConstructor
public class ExpressionParser {

  private final ITokenizer tokenizer;

  /*
    Digit ::= [0-9]
    Letter ::= [A-Za-z]

    Int ::= "-"? Digit+
    Float ::= "-"? Digit* "." Digit+
    String ::= '"' ('\"' | [^"] | "\s")* '"'
    Identifier ::= Letter (Digit | Letter | '_')*
    Literal ::= "true" | "false" | "null"

    AdditiveOperator ::= "+" | "-"
    MultiplicativeOperator ::= "*" | "/" | "%"
    EqualityOperator ::= ">" | "<" | ">=" | "<=" | "==" | "!=" | "===" | "!=="

    PrimaryExpression ::= Int | Float | String | Identifier | Literal

    ExponentiationExpression ::= PrimaryExpression (MultiplicativeOperator PrimaryExpression)*
    MultiplicativeExpression ::= ExponentiationExpression (MultiplicativeOperator ExponentiationExpression)*
    AdditiveExpression ::= MultiplicativeExpression (AdditiveOperator MultiplicativeExpression)*

    MathExpression ::= AdditiveExpression | MultiplicativeExpression | ExponentiationExpression
    EqualityExpression ::= AdditiveExpression (EqualityOperator AdditiveExpression)*

    Expression ::= EqualityExpression | MathExpression | ("-" | "!")? "(" Expression ")" | PrimaryExpression
   */

  /**
   * Main entry point when parsing an expression
   */
  private @Nullable AExpression parseExpression() throws AParserError {
    return parseEqualityExpression();
  }

  /**
   * Parses an expression made up of equality expressions with additive expression
   * operands and keeps on collecting as many same-precedence expressions as available.
   * If there's no equality operator available, this path will yield a additive expression.
   */
  private AExpression parseEqualityExpression() {
    AExpression lhs = parseAdditiveExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
        (
          tk.getType() == TokenType.GREATER_THAN || tk.getType() == TokenType.GREATER_THAN_OR_EQUAL ||
          tk.getType() == TokenType.LESS_THAN || tk.getType() == TokenType.LESS_THAN_OR_EQUAL ||
          tk.getType() == TokenType.VALUE_EQUALS || tk.getType() == TokenType.VALUE_NOT_EQUALS ||
          tk.getType() == TokenType.VALUE_EQUALS_EXACT || tk.getType() == TokenType.VALUE_NOT_EQUALS_EXACT
        )
    ) {
      tokenizer.consumeToken();

      ComparisonOperation operator;

      switch (tk.getType()) {
        case GREATER_THAN:
          operator = ComparisonOperation.GREATER_THAN;
          break;

        case GREATER_THAN_OR_EQUAL:
          operator = ComparisonOperation.GREATER_THAN_OR_EQUAL;
          break;

        case LESS_THAN:
          operator = ComparisonOperation.LESS_THAN;
          break;

        case LESS_THAN_OR_EQUAL:
          operator = ComparisonOperation.LESS_THAN_OR_EQUAL;
          break;

        case VALUE_EQUALS:
          operator = ComparisonOperation.EQUAL;
          break;

        case VALUE_EQUALS_EXACT:
          operator = ComparisonOperation.EQUAL_EXACT;
          break;

        case VALUE_NOT_EQUALS:
          operator = ComparisonOperation.NOT_EQUAL;
          break;

        case VALUE_NOT_EQUALS_EXACT:
          operator = ComparisonOperation.NOT_EQUAL_EXACT;
          break;

        default:
          throw new IllegalStateException();
      }

      // Put the previously parsed expression into the left hand side of the new equality
      // and try to parse another same-precedence expression for the right hand side
      lhs = new ComparisonExpression(lhs, parseAdditiveExpression(), operator);
    }

    return lhs;
  }

  /**
   * Parses an expression made up of additive expressions with multiplicative expression
   * operands and keeps on collecting as many same-precedence expressions as available.
   * If there's no additive operator available, this path will yield a multiplicative expression.
   */
  private AExpression parseAdditiveExpression() {
    AExpression lhs = parseMultiplicativeExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      (tk.getType() == TokenType.PLUS || tk.getType() == TokenType.MINUS)
    ) {
      tokenizer.consumeToken();

      MathOperation operator = MathOperation.ADDITION;

      if (tk.getType() == TokenType.MINUS)
        operator = MathOperation.SUBTRACTION;

      // Put the previously parsed expression into the left hand side of the new addition
      // and try to parse another same-precedence expression for the right hand side
      lhs = new MathExpression(lhs, parseMultiplicativeExpression(), operator);
    }

    return lhs;
  }

  /**
   * Parses an expression made up of multiplicative expressions with exponentiation expression
   * operands and keeps on collecting as many same-precedence expressions as available.
   * If there's no multiplicative operator available, this path will yield a exponentiation expression.
   */
  private AExpression parseMultiplicativeExpression() throws AParserError {
    AExpression lhs = parseExponentiationExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      (tk.getType() == TokenType.MULTIPLICATION || tk.getType() == TokenType.DIVISION || tk.getType() == TokenType.MODULO)
    ) {
      tokenizer.consumeToken();

      MathOperation operator = MathOperation.MULTIPLICATION;

      if (tk.getType() == TokenType.DIVISION)
        operator = MathOperation.DIVISION;

      else if (tk.getType() == TokenType.MODULO)
        operator = MathOperation.MODULO;

      // Put the previously parsed expression into the left hand side of the new multiplication
      // and try to parse another same-precedence expression for the right hand side
      lhs = new MathExpression(lhs, parseExponentiationExpression(), operator);
    }

    return lhs;
  }

  /**
   * If there's no opening parenthesis this function will respond with just a parsed
   * primary expression, otherwise the opening parenthesis will be consumed, an expression
   * will be parsed and a closing parenthesis will be expected and also consumed.
   */
  private AExpression parseParenthesisExpression() throws AParserError {
    Token tk = tokenizer.peekToken();

    // End reached, let the default routine handle this case
    if (tk == null)
      return parsePrimaryExpression();

    TokenType firstTokenType = tk.getType();
    boolean consumedFirstToken = false;

    // The notation -() will flip the resulting number's sign
    // The notation !() will negate the resulting boolean
    if (tk.getType() == TokenType.MINUS || tk.getType() == TokenType.BOOL_NOT) {
      tokenizer.saveState();

      tokenizer.consumeToken();
      consumedFirstToken = true;

      tk = tokenizer.peekToken();
    }

    // Not a parenthesis expression, let the default routine handle this case
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_OPEN) {

      // Put back the consumed token which would have had an effect on these parentheses
      if (consumedFirstToken)
        tokenizer.restoreState();

      return parsePrimaryExpression();
    }

    // Consume the opening parenthesis
    tokenizer.consumeToken();

    // Parse the expression within the parentheses
    AExpression expression = parseExpression();

    tk = tokenizer.consumeToken();

    // A previously opened parenthesis has to be closed again
    if (tk == null || tk.getType() != TokenType.PARENTHESIS_CLOSE)
      throw new UnexpectedTokenError(tokenizer, tk, TokenType.PARENTHESIS_CLOSE);

    // Wrap the expression within a unary expression based on the first token's type
    switch (firstTokenType) {
      case MINUS:
        expression = new FlipSignExpression(expression);
        break;

      case BOOL_NOT:
        expression = new InvertExpression(expression);
        break;
    }

    return expression;
  }

  /**
   * Parses an expression made up of exponential expressions with primary expression
   * operands and keeps on collecting as many same-precedence expressions as available.
   * If there's no exponentiation operator available, this path will yield a primary expression.
   */
  private AExpression parseExponentiationExpression() throws AParserError {
    AExpression lhs = parseParenthesisExpression();
    Token tk;

    while (
      (tk = tokenizer.peekToken()) != null &&
      tk.getType() == TokenType.EXPONENT
    ) {
      tokenizer.consumeToken();

      // Put the previously parsed expression into the left hand side of the new exponentiation
      // and try to parse another same-precedence expression for the right hand side
      lhs = new MathExpression(lhs, parseParenthesisExpression(), MathOperation.POWER);
    }

    return lhs;
  }

  /**
   * Parses a primary expression - the smallest still meaningful expression possible which
   * then can be intertwined with all other available expressions
   * @throws AParserError No token available to consume or the token type mismatched
   */
  private AExpression parsePrimaryExpression() throws AParserError {
    Token tk = tokenizer.consumeToken();

    if (tk == null)
      throw new UnexpectedTokenError(tokenizer, null, TokenType.valueTypes);

    // Whether the primary expression has been marked as negative
    boolean isNegative = false;

    // Notation of a negative number
    if (tk.getType() == TokenType.MINUS) {
      tk = tokenizer.consumeToken();

      // Either no token left or it's not a number, a float or an identifier
      if (tk == null || !(tk.getType() == TokenType.INT || tk.getType() == TokenType.FLOAT || tk.getType() == TokenType.IDENTIFIER))
        throw new UnexpectedTokenError(tokenizer, tk, TokenType.INT, TokenType.FLOAT, TokenType.IDENTIFIER);

      isNegative = true;
    }

    switch (tk.getType()) {
      case INT:
        return new IntExpression((isNegative ? -1 : 1) * Integer.parseInt(tk.getValue()));

      case FLOAT:
        return new FloatExpression((isNegative ? -1 : 1) * Float.parseFloat(tk.getValue()));

      case STRING:
        return new StringExpression(tk.getValue());

      case IDENTIFIER:
        return new IdentifierExpression(tk.getValue(), isNegative);

      case TRUE:
        return new LiteralExpression(LiteralType.TRUE);

      case FALSE:
      return new LiteralExpression(LiteralType.FALSE);

      case NULL:
      return new LiteralExpression(LiteralType.NULL);

      default:
        throw new UnexpectedTokenError(tokenizer, tk, TokenType.valueTypes);
    }
  }

  /**
   * Parses the noted expression into an abstract syntax tree
   * @return AST root ready for execution
   */
  public AExpression parse() throws AParserError {
    AExpression result = parseExpression();
    Token tk = tokenizer.peekToken();

    // If there are still tokens left after parsing an expression, the expression
    // wasn't closed in itself and has thus to be malformed, as this parser is only
    // intended for mono-expression "programs"
    if (tk != null)
      throw new UnexpectedTokenError(tokenizer, tk);

    return result;
  }
}