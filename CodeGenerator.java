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
		return null;
    }

    public Object VisitMemory(AATMemory expression) {
        expression.mem().Accept(this);
        emit("lw $ACC, 0($ACC)");
		return null;
    }


    public Object VisitOperator(AATOperator expression) {

        if(expression.operator() != AATOperator.NOT) {
            expression.left().Accept(this);

            emit("sw $ACC, 0($ESP)");
            emit("addi $ESP, $ESP, -4");

            expression.right().Accept(this);

            emit("lw $t1, 4($ESP)");
            emit("addi $ESP, $ESP, 4");
        }

        if(expression.operator() == AATOperator.PLUS) {
            emit("add $ACC, $t1, $ACC");
        } else if (expression.operator() == AATOperator.MINUS) {
            emit("sub $ACC, $t1, $ACC");
        } else if (expression.operator() == AATOperator.GREATER_THAN) {
            emit("slt $ACC, $t1, $ACC");
        } else if (expression.operator() == AATOperator.LESS_THAN) {
            emit("slt $ACC, $ACC, $t1");
            emit("sw $ACC, 4($ESP)");
            emit("add $ESP, $ESP, 4");
        } else if (expression.operator() == AATOperator.GREATER_THAN_EQUAL) {
            emit("addi $t1, $t1, -1");
            emit("slt $ACC, $t1, $ACC");
        } else if (expression.operator() == AATOperator.LESS_THAN_EQUAL) {
            emit("addi $ACC, $ACC, -1");
            emit("slt $ACC, $ACC, $t1");
        } else if (expression.operator() == AATOperator.EQUAL) {
            emit("beq $ACC, $t1, truelab");
            emit("addi $ACC, 0");
            emit("j endlab");
            emit("truelab:");
            emit("addi $ACC, 1");
            emit("endlab:");
        } else if (expression.operator() == AATOperator.NOT_EQUAL) {
            emit("beq $ACC, $t1, truelab");
            emit("addi $ACC, 1");
            emit("j endlab");
            emit("truelab:");
            emit("addi $ACC, 0");
            emit("endlab:");
        } else if (expression.operator() == AATOperator.AND) {
            emit("mult $ACC, $t1");
            emit("mflo $ACC");
            emit("bgtz $ACC, truelab");
            emit("addi $ACC, 0");
            emit("j endlab");
            emit("truelab:");
            emit("addi $ACC, 1");
            emit("endlab:");
        } else if (expression.operator() == AATOperator.OR) {
            emit("add $ACC, $ACC, $t1");
            emit("bgtz $ACC, truelab");
            emit("addi $ACC, 0");
            emit("j endlab");
            emit("truelab:");
            emit("addi $ACC, 1");
            emit("endlab:");
        } else if (expression.operator() == AATOperator.NOT) {
            expression.left().Accept(this);
            emit("sw $ACC, 0($ESP)");
            emit("addi $ESP, $ESP, -4");

            emit("addi $t1, 1");

            emit("sub $ACC, $t1, $ACC");
        }

//        emit("sw $t1, 8($ESP)");
//        emit("add $ESP, $ESP, 4");

		return null;
    }

    public Object VisitRegister(AATRegister expression) {
//        Simple Tiling
//        emit("sw " + expression.register().toString() + ", 0($ESP)");
//        emit("addi $ESP, $ESP, -4");

//        Improved Tiling
        emit("addi $ACC, " + expression.register() + ", 0");
		return null;
    }

    public Object VisitCallStatement(AATCallStatement statement) {
        return null;
    }

    public Object VisitConditionalJump(AATConditionalJump statement) {
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
        if (statement.lhs() instanceof AATMemory) {
            ((AATMemory)statement.lhs()).mem().Accept(this);
            // <emit code to store acc on expression stack>
            statement.rhs().Accept(this);
            // <code to pop value off expression stack to T1>
            emit("sw " + Register.ACC() + " 0(" + Register.Tmp1() + ")");

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
        return null;
    }

    public Object VisitConstant(AATConstant expression) {
//        Simple Tiling
//        emit("addi $t1, $zero, " + expression.value());
//        emit("sw $t1, 0($ESP)");
//        emit("addi $ESP, $ESP, -4");

//        Improved Tiling
        emit("addi $ACC, $zero, " + expression.value());

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
