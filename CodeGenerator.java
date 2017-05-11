import java.io.*;

class CodeGenerator implements AATVisitor {

    public CodeGenerator(String output_filename) {
        this.labeldepth = 0;

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
        int multiplier = 0;

        for(int i = n-1; i >= 0; i--){
            ((AATExpression) expression.actuals().get(i)).Accept(this);
            emit("sw " + Register.ACC() + ", " + (multiplier * -4) + "(" + Register.SP() + ")");
            emit("addi " + Register.SP() + ", " + Register.SP() + ", -4");

        }

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

        String truelab = "truelab" + labeldepth;
        String endlab = "endlab" + labeldepth;

        // if (expression.left() instanceof AATConstant) {
        //     if (expression.right() instanceof AATConstant) {
        //             System.out.println("Left: " + ((AATConstant) expression.left()).value());
        //             System.out.println("Right: " + ((AATConstant) expression.right()).value());
        //     }
        // }

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
        } else if (expression.operator() == AATOperator.LESS_THAN) {
            emit("slt " + Register.ACC() + ", " + Register.Tmp1() + ", " + Register.ACC() + "");
        } else if (expression.operator() == AATOperator.GREATER_THAN) {
            emit("slt " + Register.ACC() + ", " + Register.ACC() + ", " + Register.Tmp1() + "");
            emit("sw " + Register.ACC() + ", 4(" + Register.ESP() + ")");
            emit("add " + Register.ESP() + ", " + Register.ESP() + ", 4");
        } else if (expression.operator() == AATOperator.LESS_THAN_EQUAL) {
            emit("addi " + Register.Tmp1() + ", " + Register.Tmp1() + ", -1");
            emit("slt " + Register.ACC() + ", " + Register.Tmp1() + ", " + Register.ACC() + "");
        } else if (expression.operator() == AATOperator.GREATER_THAN_EQUAL) {
            emit("addi " + Register.ACC() + ", " + Register.ACC() + ", -1");
            emit("slt " + Register.ACC() + ", " + Register.ACC() + ", " + Register.Tmp1() + "");
        } else if (expression.operator() == AATOperator.EQUAL) {
            this.labeldepth++;
            emit("beq " + Register.ACC() + ", " + Register.Tmp1() + ", " + truelab);
            emit("addi " + Register.ACC() + ", " + Register.Zero() + ", 0");
            emit("j " + endlab);
            emit(truelab + ":");
            emit("addi " + Register.ACC() + ", " + Register.Zero() + ", 1");
            emit(endlab + ":");
        } else if (expression.operator() == AATOperator.NOT_EQUAL) {
            this.labeldepth++;
            emit("bne " + Register.ACC() + ", " + Register.Tmp1() + ", " + truelab);
            emit("addi " + Register.ACC() + ", " + Register.Zero() + ", 0");
            emit("j " + endlab);
            emit(truelab + ":");
            emit("addi " + Register.ACC() + ", " + Register.Zero() + ", 1");
            emit(endlab +":");
        } else if (expression.operator() == AATOperator.AND) {
            emit("and " + Register.ACC() + ", " + Register.ACC() + ", " + Register.Tmp1());
        } else if (expression.operator() == AATOperator.OR) {
            emit("or " + Register.ACC() + ", " + Register.ACC() + ", " + Register.Tmp1());
        } else if (expression.operator() == AATOperator.NOT) {
            expression.left().Accept(this);
            emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")");
            emit("addi " + Register.ESP() + ", " + Register.ESP() + ", -4");

            emit("addi " + Register.Tmp1() + ", " + Register.Zero() + ", 1");

            emit("sub " + Register.ACC() + ", " + Register.Tmp1() + ", " + Register.ACC() + "");
        } else if (expression.operator() == AATOperator.MULTIPLY) {
            // emit("mult " + Register.Tmp1() + ", " + Register.ACC());
            emit("mul " + Register.ACC() + ", " + Register.ACC() + ", " + Register.Tmp1());
            // emit("mflo " + Register.ACC());
        } else if (expression.operator() == AATOperator.DIVIDE) {
            emit("div " + Register.Tmp1() + ", " + Register.ACC());
            // emit("div " + Register.ACC() + ", " + Register.Tmp1());
            emit("mflo " + Register.ACC());
        }

        return null;
    }

    public Object VisitRegister(AATRegister expression) {
        emit("addi " + Register.ACC() + ", " + expression.register() + ", 0");
        return null;
    }

    public Object VisitCallStatement(AATCallStatement statement) {
        int n = statement.actuals().size();

        for(int i = 0; i < n; i++){
            int multiplier = i + 1;
            ((AATExpression) statement.actuals().get(i)).Accept(this);

            if(multiplier == n){
                emit("sw " + Register.ACC() + ", " + 0 + "(" + Register.SP() + ")");
            } else {
                emit("sw " + Register.ACC() + ", " + -1 * ((4 * n) - (4 * multiplier)) + "(" + Register.SP() + ")");
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

    private int NUMMOVS = 0;

    public Object VisitMove(AATMove statement) {

        // <emit code to store acc on expression stack>
        if (statement.lhs() instanceof AATMemory) {


            if (statement.rhs() instanceof AATConstant) {
                emit("doingmov" + this.NUMMOVS + "_lhs_is_mem:");
            } else if (statement.rhs() instanceof AATRegister) {
                emit("doingmov" + this.NUMMOVS + "_lhs_is_mem:");
            } else if (statement.rhs() instanceof AATOperator) {
                emit("doingmov" + this.NUMMOVS + "_lhs_is_mem:");
            } else if (statement.rhs() instanceof AATMemory) {
                System.out.println("Encountered lhs is a memory, rhs case is a memory");
            } else if (statement.rhs() instanceof AATExpression) {
                emit("doingmov" + this.NUMMOVS + "lhs_is_reg_rhs_is_func:");
            } else {
                System.out.println("Not handling lhs is a register, rhs is unknown");
            }

            // Store lhs memory value into ACC
            ((AATMemory)statement.lhs()).mem().Accept(this);

            // Move acc onto esp (can't assume rhs won't use t1)
            emit("sw " + Register.ACC() + ", 0(" + Register.ESP() + ")");
            emit("addi " + Register.ESP() + ", " +Register.ESP() + ", -4");

            // Store rhs value into ACC
            statement.rhs().Accept(this);

            // Load lhs value from esp into t1
            emit("lw " + Register.Tmp1() + ", 4(" + Register.ESP() + ")");
            emit("addi " + Register.ESP() + ", " +Register.ESP() + ", 4");

            // Move acc into t1
            emit("sw " + Register.ACC() + ", 0(" + Register.Tmp1() + ")");

        } else if (statement.lhs() instanceof AATRegister) {

            if (statement.rhs() instanceof AATConstant) {
                emit("doingmov" + this.NUMMOVS + "_lhs_is_reg_" + ((AATRegister)statement.lhs()).register() + "_" + ((AATConstant) statement.rhs()).value() + ":");
            } else if (statement.rhs() instanceof AATRegister) {
                emit("doingmov" + this.NUMMOVS + "_lhs_is_reg_" + ((AATRegister)statement.lhs()).register() + "_" + ((AATRegister)statement.rhs()).register() + ":");
            } else if (statement.rhs() instanceof AATOperator) {
                emit("doingmov" + this.NUMMOVS + "_lhs_is_reg_" + ((AATRegister)statement.lhs()).register() + ":");
            } else if (statement.rhs() instanceof AATMemory) {
                emit("doingmov" + this.NUMMOVS + "lhs_is_reg_rhs_is_func:");
            } else {

            }

            // Get value of rhs into ACC
            statement.rhs().Accept(this);

            // Store ACC into Register denoted by lhs
            if (statement.rhs() instanceof AATConstant) {
                emit("li " + ((AATRegister)statement.lhs()).register() + ", " + ((AATConstant) statement.rhs()).value());
            } else if (statement.rhs() instanceof AATRegister) {
                emit("move " + ((AATRegister)statement.lhs()).register() + ", " + Register.ACC());
            } else if (statement.rhs() instanceof AATOperator) {
                emit("move " + ((AATRegister)statement.lhs()).register() + ", " + Register.ACC());
            } else if (statement.rhs() instanceof AATMemory) {
                emit("move " + ((AATRegister)statement.lhs()).register() + ", " + Register.ACC());
            } else {
                System.out.println("Not handling lhs is a register, rhs is unknown");
            }
        }
        emit("endmov" + this.NUMMOVS++ + ":");
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
    private int labeldepth;
    /* Feel Free to add more instance variables, if you like */
}
