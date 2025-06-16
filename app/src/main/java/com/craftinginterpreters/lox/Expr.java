package com.craftinginterpreters.lox;

sealed interface Expr
    permits Expr.Literal, Expr.Logical, Expr.Unary, Expr.Assign, Expr.Binary,
        Expr.Grouping, Expr.Variable {

    interface Visitor<R> {
        R visit(Literal expr);
        R visit(Logical expr);
        R visit(Unary expr);
        R visit(Assign expr);
        R visit(Binary expr);
        R visit(Grouping expr);
        R visit(Variable expr);
    }

    <R> R accept(Visitor<R> visitor);

    record Literal(Object value) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }
    record Logical(Expr left, Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }
    record Unary(Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }
    record Assign(Token name, Expr value) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }
    record Binary(Expr left, Token operator, Expr right) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }
    record Grouping(Expr expr) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }
    record Variable(Token identifier) implements Expr {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }
}
