import java.io.*;

class CodeGenerator implements AATVisitor {

    public CodeGenerator(String output_filename) {
        try {
            output = new PrintWriter(new FileOutputStream(output_filename));
        } catch (IOException e) {
            System.out.println("Could not open file "+output_filename+" for writing.");
        }
	/*  Feel free to add code here, if you want to */
        EmitSetupCode();
    }

    public Object VisitCallExpression(AATCallExpression expression) {
        int n = expression.actuals().size();

        for(int i = 0; i < n; i++){
            int multiplier = i + 1;
            ((AATExpression) expression.actuals().get(i)).Accept(this);

            if(multiplier == n){
                emit("sw " + Register.ACC() + ", -" + 0 + "(" + Register.SP() + ")");
            } else {
                emit("sw " + Register.ACC() + ", -" + -1 * ((4 * n) - (4 * multiplier)) + "(" + Register.SP() + ")");
            }
        }


        emit("addi " + Register.SP() + ", " + Register.SP() + ", " + (-1 * (4 * n)));
        emit("jal " + expression.label().toString());
        emit("addi " + Register.SP() + ", " + Register.SP() + ", " + ((4 * n)));
        emit("addi " + Register.ACC() + ", " + Register.Result() + ", 0");
        return null;
    }

    public Object VisitMemory(AATMemory expression) {
        expression.mem().Accept(this);
        emit("lw " + Register.ACC() + ", 0(" + Register.ACC() + ")");
        return null;
    }


    public Object VisitOperator(AATOperator expression) {

        if(expression.operator() != AATOperator.NOT) {
            expression.left().Accept(this);

            emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")");
            emit("addi " + Register.ESP() + ", " + Register.ESP() + ", -4");

            expression.right().Accept(this);

            emit("lw " + Register.Tmp1() + ", 4(" + Register.ESP() + ")");
            emit("addi " + Register.ESP() + ", " + Register.ESP() + ", 4");
        }

        if(expression.operator() == AATOperator.PLUS) {
            emit("add " + Register.ACC() + ", " + Register.Tmp1() + ", " + Register.ACC() + "");
        } else if (expression.operator() == AATOperator.MINUS) {
            emit("sub " + Register.ACC() + ", " + Register.Tmp1() + ", " + Register.ACC() + "");
        } else if (expression.operator() == AATOperator.GREATER_THAN) {
            emit("slt " + Register.ACC() + ", " + Register.Tmp1() + ", " + Register.ACC() + "");
        } else if (expression.operator() == AATOperator.LESS_THAN) {
            emit("slt " + Register.ACC() + ", " + Register.ACC() + ", " + Register.Tmp1() + "");
            emit("sw " + Register.ACC() + ", 4(" + Register.ESP() + ")");
            emit("add " + Register.ESP() + ", " + Register.ESP() + ", 4");
        } else if (expression.operator() == AATOperator.GREATER_THAN_EQUAL) {
            emit("addi " + Register.Tmp1() + ", " + Register.Tmp1() + ", -1");
            emit("slt " + Register.ACC() + ", " + Register.Tmp1() + ", " + Register.ACC() + "");
        } else if (expression.operator() == AATOperator.LESS_THAN_EQUAL) {
            emit("addi " + Register.ACC() + ", " + Register.ACC() + ", -1");
            emit("slt " + Register.ACC() + ", " + Register.ACC() + ", " + Register.Tmp1() + "");
        } else if (expression.operator() == AATOperator.EQUAL) {
            emit("beq " + Register.ACC() + ", " + Register.Tmp1() + ", truelab");
            emit("addi " + Register.ACC() + ", 0");
            emit("j endlab");
            emit("truelab:");
            emit("addi " + Register.ACC() + ", 1");
            emit("endlab:");
        } else if (expression.operator() == AATOperator.NOT_EQUAL) {
            emit("beq " + Register.ACC() + ", " + Register.Tmp1() + ", truelab");
            emit("addi " + Register.ACC() + ", 1");
            emit("j endlab");
            emit("truelab:");
            emit("addi " + Register.ACC() + ", 0");
            emit("endlab:");
        } else if (expression.operator() == AATOperator.AND) {
            emit("mult " + Register.ACC() + ", " + Register.Tmp1() + "");
            emit("mflo " + Register.ACC() + "");
            emit("bgtz " + Register.ACC() + ", truelab");
            emit("addi " + Register.ACC() + ", 0");
            emit("j endlab");
            emit("truelab:");
            emit("addi " + Register.ACC() + ", 1");
            emit("endlab:");
        } else if (expression.operator() == AATOperator.OR) {
            emit("add " + Register.ACC() + ", " + Register.ACC() + ", " + Register.Tmp1() + "");
            emit("bgtz " + Register.ACC() + ", truelab");
            emit("addi " + Register.ACC() + ", 0");
            emit("j endlab");
            emit("truelab:");
            emit("addi " + Register.ACC() + ", 1");
            emit("endlab:");
        } else if (expression.operator() == AATOperator.NOT) {
            expression.left().Accept(this);
            emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")");
            emit("addi " + Register.ESP() + ", " + Register.ESP() + ", -4");

            emit("addi " + Register.Tmp1() + ", 1");

            emit("sub " + Register.ACC() + ", " + Register.Tmp1() + ", " + Register.ACC() + "");
        }

//        emit("sw " + Register.Tmp1() + ", 8(" + Register.ESP() + ")");
//        emit("add " + Register.ESP() + ", " + Register.ESP() + ", 4");

        return null;
    }

    public Object VisitRegister(AATRegister expression) {
//        Simple Tiling
//        emit("sw " + expression.register().toString() + ", 0(" + Register.ESP() + ")");
//        emit("addi " + Register.ESP() + ", " + Register.ESP() + ", -4");

//        Improved Tiling
        emit("addi " + Register.ACC() + ", " + expression.register() + ", 0");
        return null;
    }

    public Object VisitCallStatement(AATCallStatement statement) {
        int n = statement.actuals().size();

        for(int i = 0; i < n; i++){
            int multiplier = i + 1;
            ((AATExpression) statement.actuals().get(i)).Accept(this);

            if(multiplier == n){
                emit("sw " + Register.ACC() + ", -" + 0 + "(" + Register.SP() + ")");
            } else {
                emit("sw " + Register.ACC() + ", -" + -1 * ((4 * n) - (4 * multiplier)) + "(" + Register.SP() + ")");
            }
        }


        emit("addi " + Register.SP() + ", " + Register.SP() + ", " + (-1 * (4 * n)));
        emit("jal " + statement.label().toString());
        emit("addi " + Register.SP() + ", " + Register.SP() + ", " + ((4 * n)));
        return null;
    }

    public Object VisitConditionalJump(AATConditionalJump statement) {
        statement.test().Accept(this);

        emit("bgtz " + Register.ACC() + ", " + statement.label().toString());
        return null;
    }

    public Object VisitEmpty(AATEmpty statement) {
        return null;
    }

    public Object VisitJump(AATJump statement) {
        emit("j " + statement.label());
        return null;
    }

    public Object VisitLabel(AATLabel statement) {
        emit(statement.label() + ":");
        return null;
    }

    public Object VisitMove(AATMove statement) {

        // <emit code to store acc on expression stack>

        if (statement.lhs() instanceof AATMemory) {

            // Accept the right hand side
            if (statement.rhs() instanceof AATConstant) {
            } else if (statement.rhs() instanceof AATRegister) {
            } else if (statement.rhs() instanceof AATOperator) {
            }

            // Store lhs value into ACC
            ((AATMemory)statement.lhs()).mem().Accept(this);
            
            // Move acc onto esp (can't assume lhs won't use t1)
            emit("sw " + Register.ACC() + " 0(" + Register.ESP() + ")");
            emit("addi " + Register.ESP() + ", " +Register.ESP() + ", -4");
            

            // Store rhs value into ACC
            statement.rhs().Accept(this);

            // Load lhs value into t1
            emit("lw $t1, 4(" + Register.ESP() + ")");
            emit("addi " + Register.ESP() + ", " +Register.ESP() + ", 4");
            
            // Move acc into t1
            emit("sw " + Register.ACC() + " 0(" + Register.Tmp1() + ")");

        } else if (statement.lhs() instanceof AATRegister) {
            if (statement.rhs() instanceof AATConstant) {

            } else if (statement.rhs() instanceof AATRegister) {

            } else if (statement.rhs() instanceof AATOperator) {

            }

            // Get value of rhs into ACC
            statement.rhs().Accept(this);

            // Store ACC into Register denoted by lhs
            emit("sw " + ((AATRegister)statement.lhs()).register() + " 0(" + Register.ACC() + ")");
        }
        return null;
    }

    public Object VisitReturn(AATReturn statement) {
        emit("jr " + Register.ReturnAddr());
        return null;
    }

    public Object VisitHalt(AATHalt halt) {
	/* Don't need to implement halt -- you can leave
	   this as it is, if you like */
        return null;
    }
    public Object VisitSequential(AATSequential statement) {
        statement.left().Accept(this);
        statement.right().Accept(this);
        return null;
    }

    public Object VisitConstant(AATConstant expression) {
//        Simple Tiling
//        emit("addi " + Register.Tmp1() + ", $zero, " + expression.value());
//        emit("sw " + Register.Tmp1() + ", 0(" + Register.ESP() + ")");
//        emit("addi " + Register.ESP() + ", " + Register.ESP() + ", -4");

//        Improved Tiling
        emit("addi " + Register.ACC() + ", $zero, " + expression.value());

        return null;
    }

    private void emit(String assem) {
        assem = assem.trim();
        if (assem.charAt(assem.length()-1) == ':')
            output.println(assem);
        else
            output.println("\t" + assem);
    }

    public void GenerateLibrary() {
        emit("Print:");
        emit("lw $a0, 4(" + Register.SP() + ")");
        emit("li $v0, 1");
        emit("syscall");
        emit("li $v0,4");
        emit("la $a0, sp");
        emit("syscall");
        emit("jr $ra");
        emit("Println:");
        emit("li $v0,4");
        emit("la $a0, cr");
        emit("syscall");
        emit("jr $ra");
        emit("Read:");
        emit("li $v0,5");
        emit("syscall");
        emit("jr $ra");
        emit("allocate:");
        emit("la " + Register.Tmp1() + ", HEAPPTR");
        emit("lw " + Register.Result() + ",0(" + Register.Tmp1() + ")");
        emit("lw " + Register.Tmp2() + ", 4(" + Register.SP() + ")");
        emit("sub " + Register.Tmp2() + "," + Register.Result() + "," + Register.Tmp2());
        emit("sw " + Register.Tmp2() + ",0(" + Register.Tmp1() + ")");
        emit("jr $ra");
        emit(".data");
        emit("cr:");
        emit(".asciiz \"\\n\"");
        emit("sp:");
        emit(".asciiz \" \"");
        emit("HEAPPTR:");
        emit(".word 0");
        output.flush();
    }

    private void EmitSetupCode() {
        emit(".globl main");
        emit("main:");
        emit("addi " + Register.ESP() + "," + Register.SP() + ",0");
        emit("addi " + Register.SP() + "," + Register.SP() + "," +
                - MachineDependent.WORDSIZE * STACKSIZE);
        emit("addi " + Register.Tmp1() + "," + Register.SP() + ",0");
        emit("addi " + Register.Tmp1() + "," + Register.Tmp1() + "," +
                - MachineDependent.WORDSIZE * STACKSIZE);
        emit("la " + Register.Tmp2() + ", HEAPPTR");
        emit("sw " + Register.Tmp1() + ",0(" + Register.Tmp2() + ")");
        emit("sw " + Register.ReturnAddr() + "," + MachineDependent.WORDSIZE  + "("+ Register.SP() + ")");
        emit("jal main1");
        emit("li $v0, 10");
        emit("syscall");
    }

    private final int STACKSIZE = 1000;
    private PrintWriter output;
    /* Feel Free to add more instance variables, if you like */
}
