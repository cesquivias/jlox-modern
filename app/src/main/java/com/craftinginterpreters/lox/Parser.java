package com.craftinginterpreters.lox;

import java.util.*;
import java.util.function.Supplier;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        var statements = new ArrayList<Stmt>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Stmt declaration() {
        try {
            if (match(FUN)) return function("function");
            if (match(VAR)) return varDeclaration();

            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(FOR)) return forStatement();
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE)) return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr test = null;
        if (!check(SEMICOLON)) {
            test = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        var body = statement();

        if (increment != null) {
            body = new Stmt.Block(
                    Arrays.asList(body, new Stmt.Expression(increment))
            );
        }
        if (test == null) test = new Expr.Literal(true);
        body = new Stmt.While(test, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }
        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        var test = expression();
        consume(RIGHT_PAREN, "Except ')' after if test.");

        var then = statement();
        Stmt _else = null;
        if (match(ELSE)) {
            _else = statement();
        }

        return new Stmt.If(test, then, _else);
    }

    private Stmt expressionStatement() {
        var expr = expression();
        consume(SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind) {
        var name = consume(IDENTIFIER, "Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        var parameters = new ArrayList<Token>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(consume(IDENTIFIER, "Expect parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.");

        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        var body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private List<Stmt> block() {
        var statements = new ArrayList<Stmt>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment() {
        var expr = or();

        if (match(EQUAL)) {
            var equals = previous();
            var value = assignment();

            if (expr instanceof Expr.Variable) {
                var name = ((Expr.Variable) expr).identifier();
                return new Expr.Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        var expr = and();

        while (match(OR)) {
            var operator = previous();
            var right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        var expr = equality();

        while (match(AND)) {
            var operator = previous();
            var right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Stmt printStatement() {
        var value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        var keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }

        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        var test = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        var body = statement();

        return new Stmt.While(test, body);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr equality() {
        return parseLeftAssociative(this::comparison, BANG_EQUAL, EQUAL_EQUAL);
    }

    private Expr comparison() {
        return parseLeftAssociative(this::term,
                GREATER, GREATER_EQUAL, LESS, LESS_EQUAL);
    }

    private Expr term() {
        return parseLeftAssociative(this::factor, MINUS, PLUS);
    }

    private Expr factor() {
        return parseLeftAssociative(this::unary, SLASH, STAR);
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            var operator = previous();
            var right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        var expr = primary();

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else {
              break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        var arguments = new ArrayList<Expr>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        var paren = consume(RIGHT_PAREN,
                "Expect ')' after arguments.");

        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal());
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }

        if (match(LEFT_PAREN)) {
            var expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private Expr parseLeftAssociative(Supplier<Expr> rightParse, TokenType... types) {
        var expr = rightParse.get();

        while (match(types)) {
            var operator = previous();
            var right = rightParse.get();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type() == SEMICOLON) return;

            switch (peek().type()) {
            case CLASS:
            case FUN:
            case VAR:
            case FOR:
            case IF:
            case WHILE:
            case PRINT:
            case RETURN:
                return;
            }
        }

        advance();
    }
}
