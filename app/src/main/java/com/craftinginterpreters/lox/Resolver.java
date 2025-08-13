package com.craftinginterpreters.lox;

import java.util.*;

final class Resolver implements Expr.Visitor<Void>, Stmt.Visitor {
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private enum FunctionType {
        NONE,
        FUNCTION
    }

    void resolve(List<Stmt> statements) {
        for (var statement : statements) {
            resolve(statement);
        }
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        var enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params()) {
            declare(param);
            define(param);
        }
        resolve(function.body());
        endScope();
        currentFunction = enclosingFunction;
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        var scope = scopes.peek();
        if (scope.containsKey(name.lexeme())) {
            Lox.error(name, "Already a variable with this name in this scope.");
        }
        scope.put(name.lexeme(), false);
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme(), true);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme())) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    @Override
    public Void visit(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visit(Expr.Logical expr) {
        resolve(expr.left());
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visit(Expr.Unary expr) {
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visit(Expr.Assign expr) {
        resolve(expr.value());
        resolveLocal(expr, expr.name());
        return null;
    }

    @Override
    public Void visit(Expr.Binary expr) {
        resolve(expr.left());
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visit(Expr.Call expr) {
        resolve(expr.callee());

        for (Expr argument : expr.arguments()) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visit(Expr.Grouping expr) {
        resolve(expr.expr());
        return null;
    }

    @Override
    public Void visit(Expr.Variable expr) {
        if (!scopes.isEmpty() &&
                scopes.peek().get(expr.identifier().lexeme()) == Boolean.FALSE) {
            Lox.error(expr.identifier(),
                    "Can't read local variable in its own initializer.");
        }
        resolveLocal(expr, expr.identifier());
        return null;
    }

    @Override
    public void visit(Stmt.Expression stmt) {
        resolve(stmt.expr());
    }

    @Override
    public void visit(Stmt.If stmt) {
        resolve(stmt.test());
        resolve(stmt.then());
        if (stmt._else() != null) resolve(stmt._else());
    }

    @Override
    public void visit(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements());
        endScope();
    }

    @Override
    public void visit(Stmt.Function stmt) {
        declare(stmt.name());
        define(stmt.name());

        resolveFunction(stmt, FunctionType.FUNCTION);
    }

    @Override
    public void visit(Stmt.Print stmt) {
        resolve(stmt.expr());
    }

    @Override
    public void visit(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword(), "Can't return from top-level code");
        }

        if (stmt.value() != null) {
            resolve(stmt.value());
        }
    }

    @Override
    public void visit(Stmt.Var stmt) {
        declare(stmt.identifier());
        if (stmt.expr() != null) {
            resolve(stmt.expr());
        }
        define(stmt.identifier());
    }

    @Override
    public void visit(Stmt.While stmt) {
        resolve(stmt.test());
        resolve(stmt.body());
    }

    public Interpreter interpreter() {
        return interpreter;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Resolver) obj;
        return Objects.equals(this.interpreter, that.interpreter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interpreter);
    }

    @Override
    public String toString() {
        return "Resolver[" +
                "interpreter=" + interpreter + ']';
    }

}
