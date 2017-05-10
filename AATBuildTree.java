//TEAM MEMBERS: JOSEPH PEREZ, GILBERT CHAN

import java.util.Vector;

public class AATBuildTree {

    public AATBuildTree() {
    }

    public AATStatement functionDefinition(AATStatement body, int framesize, Label start,
                                           Label end) {

        AATExpression FP = new AATRegister(Register.SP());
        if (framesize != 0) {
            FP = new AATOperator(FP,
                    new AATConstant(framesize),
                    AATOperator.MINUS);
        }
        AATExpression RetAddr = new AATOperator(new AATRegister(Register.SP()),
                new AATConstant(framesize + MachineDependent.WORDSIZE),
                AATOperator.MINUS);
        AATExpression SP = new AATOperator(new AATRegister(Register.SP()),
                new AATConstant(framesize + MachineDependent.WORDSIZE * 2),
                AATOperator.MINUS);

        return sequentialStatement(new AATLabel(start),
                sequentialStatement(new AATMove(new AATMemory(FP), new AATRegister(Register.FP())),
                        sequentialStatement(new AATMove(new AATMemory(RetAddr), new AATRegister(Register.ReturnAddr())),
                                sequentialStatement(new AATMove(new AATRegister(Register.FP()), new AATRegister(Register.SP())),
                                        sequentialStatement(new AATMove(new AATRegister(Register.SP()), SP),
                                                sequentialStatement(body,
                                                        sequentialStatement(new AATLabel(end),
                                                                sequentialStatement(new AATMove(new AATRegister(Register.SP()), new AATRegister(Register.FP())),
                                                                        sequentialStatement(new AATMove(new AATRegister(Register.ReturnAddr()), new AATMemory(RetAddr)),
                                                                                sequentialStatement(new AATMove(new AATRegister(Register.FP()), new AATMemory(FP)),
                                                                                        new AATReturn()))))))))));

    }

    public AATStatement saveCurrRecord(int framesize) {
        // Save old FP / SP / ReturnAddres pointers
        // Set new FP (to old SP)
        // Set new SP (tp SP - (# of locals + 3) * WORDSIZE (the 3+ is for the saved registers
        return null;
    }

    public AATExpression SPPlusVal(int val) {
        return new AATOperator(new AATRegister(Register.SP()), constantExpression(val), AATOperator.PLUS);
    }

    public AATStatement restorePrevRegisters() {
        // [SP + 4] => RetAddr
        // [SP + 8] => SP
        // [SP + 12] => FP
        return null;
    }

    public AATStatement ifStatement(AATExpression test, AATStatement ifbody, AATStatement elsebody) {
        Label label1 = new Label("iftrue");
        Label label2 = new Label("ifend");
        return sequentialStatement(new AATConditionalJump(test, label1),
                sequentialStatement(elsebody,
                        sequentialStatement(new AATJump(label2),
                                sequentialStatement(new AATLabel(label1),
                                        sequentialStatement(ifbody, new AATLabel(label2))))));
    }

    public AATExpression allocate(AATExpression size) {
        // • Allocate creates an AAT that makes a call to the built-in function allocate, which takes as input the size (in
        // bytes) to allocate, and returns a pointer to the beginning of the allocated block.

        Vector tree_size = new Vector();
        tree_size.add(size);
        return new AATCallExpression(Label.AbsLabel("allocate"), tree_size);
    }

    public AATStatement whileStatement(AATExpression test, AATStatement whilebody) {
        Label label1 = new Label("whilestart");
        Label label2 = new Label("whiletest");

        return sequentialStatement(new AATJump(label2),
                sequentialStatement(new AATLabel(label1),
                        sequentialStatement(whilebody,
                                sequentialStatement(new AATLabel(label2),
                                        new AATConditionalJump(test, label1)))));
    }

    public AATStatement dowhileStatement(AATExpression test, AATStatement dowhilebody) {
        Label label1 = new Label("dowhile");

        return sequentialStatement(new AATLabel(label1),
                sequentialStatement(dowhilebody,
                        new AATConditionalJump(test, label1)));
    }

    public AATStatement forStatement(AATStatement init, AATExpression test, AATStatement increment, AATStatement body) {
        // return sequentialStatement(init, sequentialStatement(body, body));
        return sequentialStatement(init, whileStatement(test, sequentialStatement(body, increment)));
    }

    public AATStatement emptyStatement() {
        // • In code generation phase, these statements will be dropped
        return new AATEmpty();
    }

    public AATStatement callStatement(Vector actuals, Label name) {
        // • Just like Call Expressions, except they return no value
        // • Contain an assembly language label, and a list of expressions that represent actual parameters

        // AATCallStatement(Label label, Vector actuals)
        return new AATCallStatement(name, actuals);
    }

    public AATStatement assignmentStatement(AATExpression lhs, AATExpression rhs) {
        return new AATMove(lhs, rhs);
    }

    public AATStatement sequentialStatement(AATStatement first, AATStatement second) {
        return new AATSequential(first, second);
    }

    public AATExpression baseVariable(int offset) {
        return new AATMemory(new AATOperator(new AATRegister(Register.FP()),
                constantExpression(offset),
                AATOperator.PLUS)) ;
    }

    public AATExpression arrayVariable(AATExpression base,
                                       AATExpression index,
                                       int elementSize) {
        // How do we represent A[3] in abstract assembly?
        // Use the offset of A to get at the beginning of the array
        // Subtract 3 * (element size) from this pointer

        AATExpression address = null;
        AATExpression offset = null;

        // Simple index number
        if (index instanceof AATConstant) {
            offset = new AATConstant(((AATConstant) index).value() * -elementSize);
        }
        // Value must be taken from AATExpression
        else {
            offset = new AATOperator(index, new AATConstant(-elementSize), AATOperator.MULTIPLY);
        }

        address = new AATOperator(base, offset, AATOperator.PLUS);
        return new AATMemory(address);
    }

    public AATExpression classVariable(AATExpression base, int offset) {
        return new AATMemory(
                new AATOperator(
                        base,
                        new AATConstant(offset),
                        AATOperator.PLUS
                )
        );
    }

    public AATExpression constantExpression(int value) {
        return new AATConstant(value);
    }

    public AATExpression operatorExpression(AATExpression left, AATExpression right, int operator) {
        return new AATOperator(left, right, operator);
    }

    public AATExpression callExpression(Vector actuals, Label name) {
        return new AATCallExpression(name, actuals);
    }

    public AATStatement returnStatement(AATExpression value, Label functionend) {
        // copy the value of the return statement into the Result register
        // Jump to end of function
        return sequentialStatement(new AATMove(new AATRegister(Register.Result()), value),
                new AATJump(functionend));
    }
}
