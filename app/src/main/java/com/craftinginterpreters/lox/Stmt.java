package com.craftinginterpreters.lox;

import java.util.List;

sealed interface Stmt permits Stmt.Expression, Stmt.If, Stmt.Block, Stmt.Function,
        Stmt.Print, Stmt.Return, Stmt.Var, Stmt.While {

    interface Visitor {
        void visit(Expression stmt);
        void visit(If stmt);
        void visit(Block stmt);
        void visit(Function stmt);
        void visit(Print stmt);
        void visit(Return stmt);
        void visit(Var stmt);
        void visit(While stmt);
    }

    void accept(Visitor visitor);

    record Expression(Expr expr) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }

    record If(Expr test, Stmt then, Stmt _else) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }

    record Block(List<Stmt> statements) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }

    record Function(Token name, List<Token> params, List<Stmt> body) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }

    record Print(Expr expr) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }

    record Return(Token keyword, Expr value) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }

    record Var(Token identifier, Expr expr) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }

    record While(Expr test, Stmt body) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }
}
