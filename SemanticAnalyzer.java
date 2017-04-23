import java.util.Vector;

public class SemanticAnalyzer implements ASTVisitor {

    private FunctionEnvironment functionEnv;
    private TypeEnvironment typeEnv;
    private VariableEnvironment variableEnv;
    private boolean DEBUG_OUTPUT = false;
    private AATBuildTree bt;

    // NEW STUFF ================================ //
    private int stack_size = 5000;

    // For putting declarations on the stack
    private int STACK_POINTER = 5000;
    private int HEAP_POINTER = 0;

    private int GLOBAL_VAR_OFFSET;
    private Label GLOBAL_FUNC_LABEL;
    private Label GLOBAL_FUNC_END_LABEL;
    private Type GLOBAL_FUNC_RETRUN_TYPE;

    public SemanticAnalyzer() {
        functionEnv = new FunctionEnvironment();
        functionEnv.addBuiltinFunctions();
        typeEnv = new TypeEnvironment();
        variableEnv = new VariableEnvironment();
        bt = new AATBuildTree();
        // DEBUG_OUTPUT = true;
    }

    public Object VisitArrayVariable(ASTArrayVariable array) {

        TypeClass type_base = (TypeClass) array.base().Accept(this);
        TypeClass type_index = (TypeClass) array.index().Accept(this);

        if(type_index.type() != IntegerType.instance()){
            CompError.message(array.line(), "Cannot access array with non-integer index, got " + splitTypeName(type_index.type()));
            return null;
        }

        AATExpression base = type_base.value();
        AATExpression index = type_index.value();
        AATExpression AAT = bt.arrayVariable(base, index, MachineDependent.WORDSIZE);

        if (type_base.type() instanceof ArrayType) {
            return new TypeClass(((ArrayType) type_base.type()).type(), AAT);
        } else {
            return new TypeClass(type_base.type(), AAT);
        }
    }

    public Object VisitAssignmentStatement(ASTAssignmentStatement assign) {

        ASTVariable v1 = assign.variable();
        ASTExpression v2 = assign.value();

        TypeClass t1 = (TypeClass) v1.Accept(this);
        TypeClass t2 = null;

        boolean isIncrement = false;

        // If increment statement, don't call Accept again, which could lead to double error output
        if (v1 instanceof ASTBaseVariable) {
            if (v2 instanceof ASTOperatorExpression) {
                if (((ASTOperatorExpression) v2).left() instanceof ASTVariableExpression){
                    if(((ASTVariableExpression) ((ASTOperatorExpression) v2).left()).variable() instanceof ASTBaseVariable){

                        ASTBaseVariable n1 = (ASTBaseVariable) v1;
                        ASTBaseVariable n2 = (ASTBaseVariable) ((ASTVariableExpression) ((ASTOperatorExpression) v2).left()).variable();

                        // x = y + n
                        if(n1.name().equals(n2.name())) { // x.equals y
                            if(((ASTOperatorExpression) v2).operator() == 1) {  // operator is +
                                // System.out.println("GOT HERE");

                                if (((ASTOperatorExpression) v2).right() instanceof ASTIntegerLiteral
                                        && ((ASTIntegerLiteral)(((ASTOperatorExpression) v2).right())).value() == 1) {  // n = 1

                                    isIncrement = true;

                                    if (DEBUG_OUTPUT) {
                                        System.out.println("Caught ++ or --");
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }

//        if (!isIncrement) {
//            t2 = (TypeClass) v2.Accept(this);
//        } else {
//            t2 = new TypeClass(IntegerType.instance(), bt.constantExpression(1));
//        }

        t2 = (TypeClass) v2.Accept(this);

        // System.out.println(t1 + " " + t2);
        if(t1.type() != null && !(t1.type().equals(t2.type()))){
            CompError.message(assign.line(), "Types mismatch; attempting to assign " + t2.type() + " to " + t1.type());
            return null;
        } else {
            // System.out.println("attempting to assign " + t2 + " to " + t1);
        }

        return new AATMove(t1.value(), t2.value());
        // return t1;
    }

    public String splitTypeName(Type name){
        if(name != null) {
            return name.toString().split("@")[0];
        }

        return "NULL";
    }


    public Object VisitBaseVariable(ASTBaseVariable base) {
        VariableEntry varentry = variableEnv.find(base.name());

        if (DEBUG_OUTPUT) System.out.println("Hit base of " + base.name() + " with typeEnv value " + varentry.type());
        if (varentry == null) {
            CompError.message(base.line(), "Variable " + base.name() + " is not defined in this scope.");
            return new TypeClass(IntegerType.instance(), null);
        } else {
            return new TypeClass(varentry.type(), bt.baseVariable(varentry.offset()));
        }
    }

    public Object VisitBooleanLiteral(ASTBooleanLiteral boolliteral) {
        return new TypeClass(BooleanType.instance(), bt.constantExpression(boolliteral.value() ? 1 : 0));
    }

    public Object VisitClass(ASTClass classs) {

        String name = classs.name();
        if (DEBUG_OUTPUT) System.out.println("$$ New class definition: " + name);

        // Have variabledefs create the VariableEnvironment required by ClassType()
        VariableEnvironment variables = (VariableEnvironment) classs.variabledefs().Accept(this);
        if (DEBUG_OUTPUT) System.out.println("$$ Class " + name + " has " + variables.size() + " instance members");

        // Create new ClassType
        ClassType newClass = new ClassType(variables);

        // Add new class type to the type environment
        if (DEBUG_OUTPUT) System.out.println("$$ " + name + ", " + newClass + " added to typeEnv");
        typeEnv.insert(name, newClass);

        return bt.emptyStatement();
    }

    public Object VisitClasses(ASTClasses classes) {
        for(int i = 0; i < classes.size(); i++) {
            classes.elementAt(i).Accept(this);
        }
        return bt.emptyStatement();
    }

    public Object VisitClassVariable(ASTClassVariable classvariable) {

        ASTVariable base = classvariable.base();
        String variable = classvariable.variable();
        TypeClass type = (TypeClass) base.Accept(this);

        // Check if base is a ClassType
        if(!(type.type() instanceof ClassType)) {
            CompError.message(classvariable.line(), "Accessing instance member " + variable + " of a " + type.type() + " type");
            return null;
        }

        VariableEnvironment variables = ((ClassType) type.type()).variables();
        VariableEntry entry = variables.find(variable);

        if(entry == null){
            CompError.message(classvariable.line(), "Class has no instance " + variable);
            return null;
        }

        return new TypeClass(entry.type(), bt.classVariable(type.value(), entry.offset()));
    }

    public Object VisitDoWhileStatement(ASTDoWhileStatement dowhile) {
        TypeClass test = (TypeClass) dowhile.test().Accept(this);

        if(test.type() != BooleanType.instance()){
            CompError.message(dowhile.line(), "DoWhile test must be of type boolean, got " + test);
        }

        AATStatement dowhilebody = (AATStatement) dowhile.body().Accept(this);

        return bt.dowhileStatement(test.value(), dowhilebody);
        // return test;
    }

    public Object VisitEmptyStatement(ASTEmptyStatement empty) {
        return bt.emptyStatement();
    }

    public Object VisitForStatement(ASTForStatement forstmt) {


        AATStatement init = (AATStatement) forstmt.initialize().Accept(this);
        TypeClass test = (TypeClass) forstmt.test().Accept(this);

        if(test.type() != BooleanType.instance()){
            CompError.message(forstmt.line(), "ForStatement test must be of type boolean, got " + test);
        }
        AATStatement increment = (AATStatement) forstmt.increment().Accept(this);
        AATStatement body = (AATStatement) forstmt.body().Accept(this);

        return bt.forStatement(init, test.value(), increment, body);
    }

    // GOOD FUNCTION
    public Object VisitFormal(ASTFormal formal) {
        String type = formal.type();
        String name = formal.name();
        int arraydimension = formal.arraydimension();

        Type type_actual = HandleArrayTypeAgainstTypeEnvironment(type, arraydimension);

        variableEnv.insert(name, new VariableEntry(type_actual, GLOBAL_VAR_OFFSET));

        return bt.emptyStatement();
    }

    public Object VisitFormals(ASTFormals formals) {

        Vector _formals = new Vector(5);
        GLOBAL_VAR_OFFSET = 0;

        for(int i = 0; i < formals.size(); i++) {
            GLOBAL_VAR_OFFSET = (i + 1) * MachineDependent.WORDSIZE;
            // Formals are placed at SP + 4, SP + 8, etc
            ASTFormal formal = (ASTFormal) formals.elementAt(i);
            formal.Accept(this);
            _formals.addElement(formal);

        }
        return _formals;
    }

    public Object VisitFunction(ASTFunction function) {

        Type type = typeEnv.find(function.type());
        String name = function.name();
        Vector formals;
        FunctionEntry prototype = functionEnv.find(name);

        Label startLabel = null;
        Label endLabel = null;



        // Keep track of which variables we declare in this function
        variableEnv.beginScope();

        formals = (Vector) function.formals().Accept(this);

        if (prototype != null){

            startLabel = prototype.startlabel();
            endLabel = prototype.endlabel();

            Vector prototype_formals = prototype.formals();

            if(prototype.result() != type){
                CompError.message(function.line(), "  " + function.name() + " does not match prototype return type");
            }

            for(int i = 0; i < formals.size(); i++){
                ASTFormal formal = (ASTFormal) formals.elementAt(i);
                ASTFormal prototype_formal = (ASTFormal) prototype_formals.elementAt(i);

                Type formal_type = typeEnv.find(formal.type());
                Type p_type = typeEnv.find(prototype_formal.type());

                if(formal_type != p_type){
                    CompError.message(function.line(), " " + formal.type() + " " + formal.name() +
                            " does not match prototype formal of " + function.name());
                }
            }
        } else {
            startLabel = new Label(name);
            endLabel = new Label(name + "end");
            functionEnv.insert(name, new FunctionEntry(type, formals,
                    startLabel, endLabel));
        }

        GLOBAL_FUNC_END_LABEL = endLabel;
        GLOBAL_FUNC_RETRUN_TYPE = type;

        if (function.body() instanceof ASTStatements) {
            GLOBAL_VAR_OFFSET = 0;
            for(int i = 0; i < ((ASTStatements) function.body()).size(); i++){
                ASTStatement statement = ((ASTStatements) function.body()).elementAt(i);
                if(statement instanceof ASTReturnStatement){
                    GLOBAL_FUNC_LABEL = startLabel;
                }
                statement.Accept(this);
            }
        }

        AATStatement body = (AATStatement) function.body().Accept(this);

        // TODO is this really needed?
        if (type == VoidType.instance()) {
            body = bt.sequentialStatement(body, bt.returnStatement(bt.constantExpression(0), endLabel));
        }

        int framesize = (formals.size() + variableEnv.size()  + 3) * MachineDependent.WORDSIZE;

        // Goodbye variables which we declared in this function
        variableEnv.endScope();

        return bt.functionDefinition(body, framesize, startLabel, endLabel);
    }

    public Object VisitFunctionCallExpression(ASTFunctionCallExpression functioncall) {

        String name = functioncall.name();
        Vector aat_formals = new Vector(5);
        FunctionEntry entry = functionEnv.find(name);

        if (entry == null) {
            CompError.message(functioncall.line(), " function " + name + " is undeclared");
            return null;
        } else {
            // Get function formals from function environment
            Vector prototype_formals = entry.formals();

            if(functioncall.size() != prototype_formals.size()){
                CompError.message(functioncall.line(), " number of parameters (" + functioncall.size() +
                        ") does not match number of prototype parameters (" + prototype_formals.size() + ")");
                return entry.result();
            }

            for(int i = 0; i < functioncall.size(); i++) {

                // Get formal
                ASTExpression e = functioncall.elementAt(i);
                ASTFormal formal = (ASTFormal) prototype_formals.elementAt(i);

                TypeClass t = (TypeClass) e.Accept(this);

                // Double check that formal matches prototype
                String key = formal.type();
                if(formal.arraydimension() > 0){
                    key += "$" + String.valueOf(formal.arraydimension());
                }
                if(typeEnv.find(key) != (t).type()) {
                    CompError.message(functioncall.line(), " Type mismatch on parameter " + i + " in function " + name +
                            " expected " + typeEnv.find(formal.type()) + " got " + (Type) e.Accept(this));
                    return entry.result();
                }
                aat_formals.addElement(t.value());
            }

            Type type = entry.result();
            // Return type
            return new TypeClass(type, bt.callExpression(aat_formals, entry.startlabel()));
        }
    }

    public Object VisitFunctionCallStatement(ASTFunctionCallStatement functioncall) {
        String name = functioncall.name();

        Vector actuals = new Vector();

        for(int i = 0; i < functioncall.size(); i++) {

            //TODO take code from above function

            TypeClass actual = (TypeClass) functioncall.elementAt(i).Accept(this);
            actuals.add(actual.value());
        }

        FunctionEntry entry = functionEnv.find(name);

        Type t = entry.result();

        if(!(t instanceof VoidType)){
            CompError.message(functioncall.line(), " " + name + " is not a void function");
        }

        return bt.callStatement(actuals, entry.startlabel());
    }

    public Object VisitIfStatement(ASTIfStatement ifsmt) {
        TypeClass test = (TypeClass) ifsmt.test().Accept(this);

        if(test.type() != BooleanType.instance()){
            CompError.message(ifsmt.line(), "IfStatement test must be of type boolean, got " + test);
        }

        AATStatement ifbody = (AATStatement) ifsmt.thenstatement().Accept(this);

        if(ifsmt.elsestatement() != null){
            AATStatement elsebody = (AATStatement) ifsmt.elsestatement().Accept(this);

            return bt.ifStatement(test.value(), ifbody, elsebody);
        }

        return bt.ifStatement(test.value(), ifbody, bt.emptyStatement());
    }

    public Object VisitIntegerLiteral(ASTIntegerLiteral literal) {
        return new TypeClass(IntegerType.instance(), bt.constantExpression(literal.value()));
    }

    public Object VisitInstanceVariableDef(ASTInstanceVariableDef variabledef) {

        String type = variabledef.type();
        String name = variabledef.name();
        int arraydimension = variabledef.arraydimension();

        String key = arrayToString(type, arraydimension);
        Type type_actual = HandleArrayTypeAgainstTypeEnvironment(type, arraydimension);
        return type_actual;
    }

    public Object VisitInstanceVariableDefs(ASTInstanceVariableDefs variabledefs) {

        VariableEnvironment _variableEnv = new VariableEnvironment();
        ASTInstanceVariableDef curr = null;
        Type curr_type = null;

        for(int i = 0; i < variabledefs.size(); i++) {
            curr = variabledefs.elementAt(i);
            curr_type = (Type) curr.Accept(this);

            GLOBAL_VAR_OFFSET = -MachineDependent.WORDSIZE * i;

            String key = curr.name();
            _variableEnv.insert(key, new VariableEntry(curr_type, GLOBAL_VAR_OFFSET));
        }
        GLOBAL_VAR_OFFSET = 0;

        return _variableEnv;
    }

    public String arrayToString(String type, int arraydimension) {
        String builder = type;
        for(int i = 0; i < arraydimension; i++){
            builder += "[]";
        }
        return builder;
    }

    public Object VisitNewArrayExpression(ASTNewArrayExpression newarray) {


        String type = newarray.type();
        TypeClass elems = (TypeClass) newarray.elements().Accept(this);
        int arraydimension = newarray.arraydimension();


        // Make sure type is already in environment
        Type t = typeEnv.find(type);
        if(t == null) {
            CompError.message(newarray.line(), " assigning new array of undeclared type " + type);
        }

        String key = type;
        if (arraydimension > 0) { //Wait what why wouldn't this be greater than 0 rofl
            key += "$" + String.valueOf(arraydimension);
        }
        // System.out.println("Key: " + key);

        AATExpression offset;
        if (elems.value() instanceof AATConstant) {
            offset = bt.constantExpression(((AATConstant) elems.value()).value() * MachineDependent.WORDSIZE);
        } else {
            offset = bt.operatorExpression(elems.value(),
                    new AATConstant(MachineDependent.WORDSIZE),
                    AATOperator.MULTIPLY);
        }


        return new TypeClass(typeEnv.find(key), bt.allocate(offset));
    }

    public Object VisitNewClassExpression(ASTNewClassExpression newclass) {
        String type = newclass.type();

        // Does type exist? Cuz it should
        Type t = typeEnv.find(type);
        if (t == null) {
            CompError.message(newclass.line(), "Class not defined.");
            return new TypeClass(t, null);
        }

        int offset = ((ClassType) t).variables().size() * MachineDependent.WORDSIZE;
        return new TypeClass(t, bt.allocate(bt.constantExpression(offset)));
        // return t;
    }

    public Object VisitOperatorExpression(ASTOperatorExpression opexpr) {

        // Ex x < 3

        // Make sure left side is proper
        TypeClass left = (TypeClass) opexpr.left().Accept(this);

        // Make sure right side is proper
        TypeClass right = (TypeClass) opexpr.right().Accept(this);

        AATExpression leftv = left.value();
        AATExpression rightv = right.value();

        // Check if operator will return boolean type
        switch (opexpr.operator()){
            case 0:
                //Bad operator
            case 1:
            case 2:
            case 3:
            case 4:
                // arithmetic operators
                if(!(left.type() instanceof IntegerType) || !(right.type() instanceof IntegerType)){
                    CompError.message(opexpr.line(), " Cannot conduct arithmetic operation on non-integer expression");
                }
                return new TypeClass(IntegerType.instance(), bt.operatorExpression(leftv, rightv, opexpr.operator()));
            case 9:
            case 10:
            case 11:
            case 12:
                // arithmetic operators
                if(!(left.type() instanceof IntegerType) || !(right.type() instanceof IntegerType)){
                    CompError.message(opexpr.line(), " Cannot conduct arithmetic operation on non-integer expression");
                }
                return new TypeClass(BooleanType.instance(), bt.operatorExpression(leftv, rightv, opexpr.operator()));
            default:
                // 5 through 12 are logical operators
                return new TypeClass(BooleanType.instance(), bt.operatorExpression(leftv, rightv, opexpr.operator()));
        }
    }

    public Object VisitProgram(ASTProgram program) {
        program.classes().Accept(this);
        AATStatement statements = (AATStatement) program.functiondefinitions().Accept(this);

        return statements;
    }

    public Object VisitFunctionDefinitions(ASTFunctionDefinitions functiondefinitions) {

        boolean set = false;

        AATStatement toReturn = null;

        for(int i = 0; i < functiondefinitions.size(); i++) {
            Object o = functiondefinitions.elementAt(i).Accept(this);
            if(o != null){
                if(set){
                    toReturn = bt.sequentialStatement(toReturn, (AATStatement) o);
                }else{
                    set = true;
                    toReturn = (AATStatement) o;
                }
            }
        }

        return toReturn;
    }

    public Object VisitPrototype(ASTPrototype prototype) {
        Type result = typeEnv.find(prototype.type());
        Vector formals = (Vector) prototype.formals().Accept(this);

        if (result == null){
            CompError.message(prototype.line(), "Type " + prototype.type() + " is not defined");
            result = IntegerType.instance();
        }

        functionEnv.insert(prototype.name(), new FunctionEntry(result, formals, new Label(prototype.name()), new Label(prototype.name() + "end")));
        return bt.emptyStatement();
    }

    public Object VisitReturnStatement(ASTReturnStatement ret) {

        ASTExpression value = ret.value();
        if (value != null) {
            // return ret.value().Accept(this);
            TypeClass value1 = (TypeClass) ret.value().Accept(this);

            if(value1.type() != null && splitTypeName(GLOBAL_FUNC_RETRUN_TYPE) != "VoidType" && GLOBAL_FUNC_RETRUN_TYPE != value1.type()){
                CompError.message(ret.line(), " return statement type (" + GLOBAL_FUNC_RETRUN_TYPE +
                        ") does not match function type (" + value1.type() + ")");
            }

            return bt.returnStatement(value1.value(), GLOBAL_FUNC_END_LABEL);
        }
        return bt.returnStatement(bt.constantExpression(0), GLOBAL_FUNC_END_LABEL);
    }

    public Object VisitStatements(ASTStatements statements) {
        // variableEnv.beginScope();

        boolean set = false;

        AATStatement statement = null; //(AATStatement) statements.elementAt(0).Accept(this);

        for(int i = 0; i < statements.size(); i++) {
            Object o  = statements.elementAt(i).Accept(this);
            if(o != null) {
                if(!set){
                    statement = (AATStatement) statements.elementAt(i).Accept(this);
                    set = true;
                } else {
                    statement = bt.sequentialStatement(statement, (AATStatement) o);
                }
            }
        }

        // variableEnv.endScope();
        return statement;
    }

    public Object VisitUnaryOperatorExpression(ASTUnaryOperatorExpression operator) {
        int line = operator.operand().line();
        int actual_line = operator.operand().line();

        TypeClass t = (TypeClass) operator.operand().Accept(this);

        AATExpression value = (AATExpression) t.value();

        if(t.type() != BooleanType.instance()){
            CompError.message(operator.operand().line(), "Cannot negate (!) a non-boolean type, got " + t);
        }
        int op = operator.operator();

        int newValue = 1;

        System.out.println(AATConstant.class.isInstance(value));

//        if(value.value() == 1){
//            newValue = 0;
//        } else {
//            newValue = 1;
//        }

        //TODO RETURN STATEMENT

        return new TypeClass(BooleanType.instance(), bt.operatorExpression(value, null, AATOperator.NOT));
    }

    public Object VisitVariableDefStatement(ASTVariableDefStatement vardef) {

        String type = vardef.type();
        String name = vardef.name();

        // Check for type in type environment
        Type checkClassType = typeEnv.find(type);
        if (checkClassType == null){
            CompError.message(vardef.line(), "Cannot create new instance, Type " + type + " is not defined");
        }

        int arraydimension = vardef.arraydimension();
        Type type_actual = HandleArrayTypeAgainstTypeEnvironment(type, arraydimension);
        String key = arrayToString(type, arraydimension);

        VariableEntry entry = new VariableEntry(type_actual, GLOBAL_VAR_OFFSET);
        GLOBAL_VAR_OFFSET -= MachineDependent.WORDSIZE;
        variableEnv.insert(name, entry);

        ASTExpression init = vardef.init();
        if (init == null) {
            // Variable initialization without declaration value
            return bt.emptyStatement();
        } else {
            TypeClass t = (TypeClass) init.Accept(this);
            return bt.assignmentStatement(bt.baseVariable(entry.offset()), t.value());
        }
    }

    public Type HandleArrayTypeAgainstTypeEnvironment(String base, int arraydimension) {

        // Base case, non-array
        if (arraydimension == 0) {
            return typeEnv.find(base);
        }

        Type predecessor;
        Type me = null;
        String key  = base + "$" + String.valueOf(arraydimension);

        //Check if I exist in type env
        me  = typeEnv.find(key);
        // If I do, return whatever you find in the environment
        if(me != null) {
            return me;
        }
        // If I don't,

        // Get ArrayType of predecessor from a recusrive call of HANDLE(base, --arraydimension)
        predecessor = HandleArrayTypeAgainstTypeEnvironment(base, --arraydimension);
        //make a new ArrayType of type predecessor
        me = new ArrayType(predecessor);
        //Insert into environment
        if (DEBUG_OUTPUT) System.out.println("TYPE[] ENV INSERT: " + key + ", " + me);
        typeEnv.insert(key, me);
        //return that new ArrayType
        return me;

    }

    public Object VisitVariableExpression(ASTVariableExpression variableexpression) {

        // Subsequent recursive Accepts should check if we can derive a boolean
        return variableexpression.variable().Accept(this);
    }

    public Object VisitWhileStatement(ASTWhileStatement whilestatement) {
        TypeClass test = (TypeClass) whilestatement.test().Accept(this);

        if(test.type() != BooleanType.instance()){
            CompError.message(whilestatement.line(), "WhileStatement test must be of type boolean, got " + test);
        }

        AATStatement whilebody = (AATStatement) whilestatement.body().Accept(this);

        return bt.whileStatement(test.value(), whilebody);
    }

    class TypeClass {
        public TypeClass(Type type, AATExpression value) {
            type_ = type;
            value_ = value;
        }
        public Type type() {
            return type_;
        }
        public AATExpression value() {
            return value_;
        }
        public void settype(Type type) {
            type_ = type;
        }
        public void setvalue(AATExpression value) {
            value_ = value;
        }
        private Type type_;
        private AATExpression value_;
    }
}
