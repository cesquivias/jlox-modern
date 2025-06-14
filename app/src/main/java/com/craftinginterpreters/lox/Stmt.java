package com.craftinginterpreters.lox;

import java.util.List;

sealed interface Stmt permits Stmt.Print, Stmt.Expression, Stmt.Block, Stmt.Var {

    interface Visitor {
        void visit(Print stmt);
        void visit(Expression stmt);
        void visit(Block stmt);
        void visit(Var stmt);
    }

    void accept(Visitor visitor);

    record Print(Expr expr) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }

    record Expression(Expr expr) implements Stmt {
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

    record Var(Token identifier, Expr expr) implements Stmt {
        @Override
        public void accept(Visitor visitor) {
            visitor.visit(this);
        }
    }
}
