package com.craftinginterpreters.lox;

sealed interface Expr
    permits Expr.Binary, Expr.Grouping, Expr.Literal, Expr.Unary {

    interface Visitor<R> {
        R visit(Binary expr);
        R visit(Grouping expr);
        R visit(Literal expr);
        R visit(Unary expr);
    }

    <R> R accept(Visitor<R> visitor);

    record Binary(Expr left, Token operator, Expr right)
            implements Expr {
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
    record Literal(Object value) implements Expr {
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
}
