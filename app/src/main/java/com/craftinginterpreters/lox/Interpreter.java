package com.craftinginterpreters.lox;

import java.text.DecimalFormat;
import java.util.List;


class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor {
    private final DecimalFormat decimalFormat = new DecimalFormat("0.#");

    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public void visit(Stmt.Block stmt) {
        executeBlock(stmt.statements(), new Environment(environment));
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        var previous = this.environment;
        try {
            this.environment = environment;

            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } finally {
            this.environment = previous;
        }
    }

    @Override
    public void visit(Stmt.Expression stmt) {
        evaluate(stmt.expr());
    }

    @Override
    public void visit(Stmt.Print stmt) {
        var value = evaluate(stmt.expr());
        System.out.println(stringify(value));
    }

    @Override
    public void visit(Stmt.Var stmt) {
        Object value = null;
        if (stmt.expr() != null) {
            value = evaluate(stmt.expr());
        }

        environment.define(stmt.identifier().lexeme(), value);
    }

    @Override
    public Object visit(Expr.Assign expr) {
        var value = evaluate(expr.value());
        environment.assign(expr.name(), value);
        return value;
    }

    @Override
    public Object visit(Expr.Binary expr) {
        var left = evaluate(expr.left());
        var right = evaluate(expr.right());

        switch (expr.operator().type()) {
        case GREATER:
            checkNumberOperands(expr.operator(), left, right);
            return (double) left > (double) right;
        case GREATER_EQUAL:
            checkNumberOperands(expr.operator(), left, right);
            return (double) left >= (double) right;
        case LESS:
            checkNumberOperands(expr.operator(), left, right);
            return (double) left < (double) right;
        case LESS_EQUAL:
            checkNumberOperands(expr.operator(), left, right);
            return (double) left <= (double) right;
        case MINUS:
            checkNumberOperands(expr.operator(), left, right);
            return (double) left - (double) right;
        case BANG_EQUAL:
            return !isEqual(left, right);
        case EQUAL_EQUAL:
            return isEqual(left, right);
        case PLUS:
            if (left instanceof Double dLeft && right instanceof Double dRight) {
                return dLeft + dRight;
            } else if (left instanceof String sLeft && right instanceof String sRight) {
                return sLeft + sRight;
            } else if (left instanceof String sLeft && right instanceof Double dRight) {
                return sLeft + dRight;
            } else {
                throw new RuntimeError(expr.operator(),
                        "Operands must be two numbers or two strings.");
            }
        case SLASH:
            checkNumberOperands(expr.operator(), left, right);
            try {
                return (double) left / (double) right;
            } catch (ArithmeticException e) {
                throw new RuntimeError(expr.operator(), "Divide by zero error.");
            }
        case STAR:
            checkNumberOperands(expr.operator(), left, right);
            return(double) left * (double) right;
        default:
            return null;
        }
    }

    @Override
    public Object visit(Expr.Grouping expr) {
        return evaluate(expr.expr());
    }

    @Override
    public Object visit(Expr.Literal expr) {
        return expr.value();
    }

    @Override
    public Object visit(Expr.Unary expr) {
        var right = evaluate(expr.right());

        switch (expr.operator().type()) {
        case BANG:
            return !isTruthy(right);
        case MINUS:
            checkNumberOperand(expr.operator(), right);
            return -(double) right;
        default:
            return null;
        }
    }

    @Override
    public Object visit(Expr.Variable expr) {
        return environment.get(expr.identifier());
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean bool) return bool;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double d) {
            return decimalFormat.format(d);
        }
        return object.toString();
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
}
