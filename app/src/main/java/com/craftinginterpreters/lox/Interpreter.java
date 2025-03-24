package com.craftinginterpreters.lox;

import java.text.DecimalFormat;


class Interpreter implements Expr.Visitor<Object> {
    private final DecimalFormat decimalFormat = new DecimalFormat("0.#");

    void interpret(Expr expression) {
        try {
            var value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
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
            } else {
                throw new RuntimeError(expr.operator(),
                        "Operands must be two numbers or two strings.");
            }
        case SLASH:
            checkNumberOperands(expr.operator(), left, right);
            return (double) left / (double) right;
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
