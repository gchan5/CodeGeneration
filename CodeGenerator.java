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
		return null;
    }


    public Object VisitOperator(AATOperator expression) {
        expression.left().Accept(this);
        expression.right().Accept(this);

        if(expression.operator() == AATOperator.PLUS) {
            emit("lw $t1, 8($ESP)");
            emit("lw $t2, 4($ESP)");
            emit("add $t1, $t1, $t2");
            emit("sw $t1, 8($ESP)");
            emit("add $ESP, $ESP, 4");
        } else if (expression.operator() == AATOperator.MINUS) {
            emit("lw $t1, 8($ESP)");
            emit("lw $t2, 4($ESP)");
            emit("sub $t1, $t1, $t2");
            emit("sw $t1, 8($ESP)");
            emit("add $ESP, $ESP, 4");
        }

		return null;
    }

    public Object VisitRegister(AATRegister expression) {
        emit("sw " + expression.register().toString() + ", 0($ESP)");
        emit("addi $ESP, $ESP, -4");
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
        emit("addi $t1, $zero, " + expression.value());
        emit("sw $t1, 0($ESP)");
        emit("addi $ESP, $ESP, -4");

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
