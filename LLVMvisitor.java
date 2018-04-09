import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import java.io.*;

public class LLVMvisitor extends GJDepthFirst<Node,String>{
	private BufferedWriter writer;
	private int curVar=-1;
	private int curLab=-1;
	private String curClass;
	private String mainClass;
	private String currMethod;
	private String currObjType; //the class in which an object belongs--for MessageSend
	private Stack<Node>argStack;

	public LLVMvisitor(FileWriter fw){
		writer=new BufferedWriter(fw);
		argStack=new Stack<>();
	}

	public String getVar(){
		this.curVar+=1;
		return "%_"+this.curVar;
	}
	public String getLabel(){
		this.curLab+=1;
		return "label_"+curLab;
	}

	public void emit(String context){
		try{
			writer.write(context);
		}
		catch (IOException e){
			System.out.println("error at emiting");
		}
	}
	public void myClose(){
		try{
			writer.close();
		}
		catch(IOException e){
			System.out.println("error at closing BufferedWriter");
		}
	}
	private String convert(String type,String side){
		String ret;
		if(type.equals("int")) ret= "i32";
		else if(type.equals("int[]")) ret= "i32*";
		else if(type.equals("boolean")) ret= "i1";
		else ret ="i8*";
		if(side!=null)if(side.equals("left")) return ret+"*";
		return ret;
	}

	private Node getLocalVar(String name,String argu){
		Map<String,MyType>tempMap;
		if(MySymbolTable.methodSymTable.containsKey(curClass)) {
			tempMap = MySymbolTable.methodSymTable.get(curClass);
			Funct obj;
			if ( (tempMap != null) && (tempMap.containsKey(currMethod)) ){
				obj = (Funct) (tempMap.get(currMethod));
				/* search if it is method's local variable */
				if (obj.map != null) {
					if (obj.map.containsKey(name)) {
						currObjType=obj.map.get(name).type;
						return new Node("%" + name, convert(obj.map.get(name).type, argu));
					}
					/* search if it is method's argument */

					if (obj.args.containsKey(name)) {
						MyType t = obj.args.get(name);
						currObjType=t.type;
						return new Node("%" + name, convert(t.type, argu));
					}
				}
			}
		}
		/*this is the case we have a local variable of main */
		/*this case is not caught above (local variables) because the main method is not in the ST */
		if(curClass.equals(mainClass)){
			String type=MySymbolTable.varSymTable.get(curClass).get(name).type;
			currObjType=type;
			return new Node("%"+name,convert(type,argu));
		}

		/* if not the above, it is field */
		String var1=getVar();
		String var2=getVar();
		String temp;
		if(MySymbolTable.varSymTable.isEmpty()) {
			String parent=MySymbolTable.parent_names.get(curClass);
			temp=MySymbolTable.varSymTable.get(parent).get(name).type;
		}
		else if(!MySymbolTable.varSymTable.containsKey(curClass)){
			String parent=MySymbolTable.parent_names.get(curClass);
			temp=MySymbolTable.varSymTable.get(parent).get(name).type;
		}
		else{
			temp = MySymbolTable.varSymTable.get(curClass).get(name).type;
		}
		currObjType=temp;
		String type=convert(temp,"right");
		int offset=8+MySymbolTable.allOfsets.get(curClass).returnVariable(name);
		emit(var1+" = getelementptr i8, i8* %this, i32 "+offset+"\n"+
		var2+" = bitcast i8* "+var1+" to "+type+"*\n");
		return new Node(var2,convert(temp,argu));

	}

	private void vTableDecl(){
		String []classes=new String[MySymbolTable.classStack.size()];
		int size=MySymbolTable.classStack.size();
		int temp=size-1;

		while(!MySymbolTable.classStack.empty()){ //transfer all class names in an array in order to iterate it
			classes[temp]=MySymbolTable.classStack.pop();
			temp--;
		}
		String to_paste;
		emit("@."+classes[0]+"_vtable = global [1 x i8*] [i8* bitcast (i32 ()* @main to i8*)]\n");
		for(int i=1;i<size;i++){ //for each class

			String parent=MySymbolTable.parent_names.get(classes[i]);
			Map<String,MyType> mymap=new LinkedHashMap<>();
			if(MySymbolTable.methodSymTable.containsKey(parent)){
				mymap=MySymbolTable.methodSymTable.get(parent);
			}
			if(MySymbolTable.methodSymTable.containsKey(classes[i])) {
				mymap.putAll(MySymbolTable.methodSymTable.get(classes[i]));
			}
			int t;
			if(!mymap.isEmpty()){t=mymap.size();}
			else t=0;
			emit("@."+classes[i]+"_vtable = global [" +t+" x i8*] [");
			if(t>0) {
				for (Map.Entry<String, MyType> entry1 : mymap.entrySet()) {//for each method of the class
					t--;
					String fun = entry1.getKey();
					String ret = mymap.get(fun).type;
					to_paste = "i8* bitcast (" + convert(ret, "right") + "(i8*,";
					Funct obj = (Funct) mymap.get(fun);
					Map<String, MyType> arguments = obj.args;

					for (Map.Entry<String, MyType> entry : arguments.entrySet()) { //for each argument of the method
						String argument = entry.getKey();
						String arg_type = arguments.get(argument).type;
						to_paste = to_paste + convert(arg_type, "right") + ",";
					}

					to_paste = to_paste.substring(0, to_paste.length() - 1); //remove the last comma
					to_paste = to_paste + ")* @" + obj.decl_type + "." + fun + " to i8*)";
					if (t == 0) emit(to_paste);
					else emit(to_paste + ",");
				}
			}
			emit("]\n");
		}
	}



	/**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
   public Node visit(MainClass n,String arg) throws Exception {
   	curClass=n.f1.f0.toString();
   	mainClass=curClass;
    vTableDecl();
    emit("declare i8* @calloc(i32, i32)"+"\n"+
    	"declare i32 @printf(i8*, ...)"+"\n"+
    	"declare void @exit(i32)"+"\n\n"+
    	"@_cint = constant [4 x i8] c\"%d\\0a\\00\" "+ "\n"+
    	"@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\""+"\n"+
    	"define void @print_int(i32 %i) {"+"\n"+
    	"%_str = bitcast [4 x i8]* @_cint to i8*"+"\n"+
    	"call i32 (i8*, ...) @printf(i8* %_str, i32 %i)"+"\n"+
    	"ret void"+"\n } \n\n"+
    	"define void @throw_oob() { \n"+
    	" %_str = bitcast [15 x i8]* @_cOOB to i8*"+"\n"+
    	" call i32 (i8*, ...) @printf(i8* %_str)"+"\n"+
    	"call void @exit(i32 1)"+"\n"+
    	"ret void"+"\n } \n"
    );

    emit("define i32 @main() {\n");
    n.f14.accept(this,arg);
    n.f15.accept(this,arg);
    emit("ret i32 0\n}\n\n");
    return null;
   }
	/**
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "{"
	 * f3 -> ( VarDeclaration() )*
	 * f4 -> ( MethodDeclaration() )*
	 * f5 -> "}"
	 */
	public Node visit(ClassDeclaration n,String argu) throws Exception {
		curClass=n.f1.f0.toString();
		n.f4.accept(this, argu);

		return null;
	}
	/**
	 * f0 -> "class"
	 * f1 -> Identifier()
	 * f2 -> "extends"
	 * f3 -> Identifier()
	 * f4 -> "{"
	 * f5 -> ( VarDeclaration() )*
	 * f6 -> ( MethodDeclaration() )*
	 * f7 -> "}"
	 */
	public Node visit(ClassExtendsDeclaration n, String argu) throws Exception {
		curClass=n.f1.f0.toString();
		n.f6.accept(this, argu);
		return null;
	}
	/**
	 * f0 -> "public"
	 * f1 -> Type()
	 * f2 -> Identifier()
	 * f3 -> "("
	 * f4 -> ( FormalParameterList() )?
	 * f5 -> ")"
	 * f6 -> "{"
	 * f7 -> ( VarDeclaration() )*
	 * f8 -> ( Statement() )*
	 * f9 -> "return"
	 * f10 -> Expression()
	 * f11 -> ";"
	 * f12 -> "}"
	 */
	public Node visit(MethodDeclaration n, String argu) throws Exception {
		Node o=n.f1.accept(this,null);
		String ret=o.type;
		String name=n.f2.f0.toString();
		currMethod=name;
		emit("define "+ret+ " @"+curClass+"."+name);
		emit("(i8* %this");
		n.f4.accept(this, argu);
		emit(")");
		emit("{\n");
		Funct obj=(Funct)MySymbolTable.methodSymTable.get(curClass).get(currMethod);
		if(obj.args!=null) {
		/*         store every argument at normal variable before doing anything         */
			for (Map.Entry<String, MyType> entry : obj.args.entrySet()) {
				MyType var = entry.getValue();
				String var_name = entry.getKey();
				emit("%" + var_name+" = alloca " + convert(var.type,"right") + "\n" +
						"store " + convert(var.type,"right") + " %." + var_name + ", " + convert(var.type,"right") +
						"* " + "%" + var_name + "\n");
			}
		}
		n.f7.accept(this, argu);
		emit("\n");
		n.f8.accept(this, argu);
		emit("\n");
		Node object=n.f10.accept(this, argu);
		emit("ret " + object.type+ " "+object.name+"\n");

		emit("}\n");
		return null;
	}
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "."
	 * f2 -> Identifier()
	 * f3 -> "("
	 * f4 -> ( ExpressionList() )?
	 * f5 -> ")"
	 */
	public Node visit(MessageSend n, String argu) throws Exception {
		Node object=n.f0.accept(this, argu);
		String method=n.f2.f0.toString();
		int offset=0;
		Funct m=null;
		String ret_type=null; //no problem initializing the above because we will definitely execute the if stmt
	//	for (Map.Entry<String, Map<String,MyType>> entry : MySymbolTable.methodSymTable.entrySet()) {
		Map<String,MyType>mymap;
		String className;
			if(MySymbolTable.methodSymTable.containsKey(currObjType)){
				mymap=MySymbolTable.methodSymTable.get(currObjType);
				className=currObjType;
			}
			else{
				mymap=MySymbolTable.methodSymTable.get(MySymbolTable.parent_names.get(currObjType));
				className=MySymbolTable.parent_names.get(currObjType);
			}
			if(mymap.containsKey(method)) {
				offset= (MySymbolTable.allOfsets.get(className).returnMethod(method)) / 8;
				m=(Funct)MySymbolTable.methodSymTable.get(className).get(method);
				ret_type=convert(m.type,"right");
				currObjType=m.type;
			}

		///}


		String t1=getVar();
		String t2=getVar();
		String t3=getVar();
		String t4=getVar();
		String t5=getVar();
		emit(t1+" = bitcast i8* "+object.name+" to i8***\n" +
		t2+" = load i8**, i8***"+t1+"\n"+
		t3+" = getelementptr i8*,i8** "+t2+",i32 "+offset+"\n"+
		t4+" = load i8*,i8** "+t3+"\n"+
		t5+" = bitcast i8* "+t4+" to "+ret_type+"(i8*");
		/*now starts the procedure of emmiting the argument types*/
		if(m.args.isEmpty()) {
			emit(")*\n");
		}
		else{
			for (Map.Entry<String, MyType> entry : m.args.entrySet()) {
				emit(","+convert(entry.getValue().type,"right"));
			}
			emit(")*\n");
		}
		String t6=getVar();

		if(n.f4.present()) n.f4.accept(this, argu);
		emit(t6+" = call "+ret_type+" "+t5+" (i8* "+object.name);
		LinkedList<Node>mylist=new LinkedList<>();
		while(!argStack.empty()){
			mylist.add(argStack.pop());
		}
		if(mylist.size()>0) {
			for (int i = mylist.size(); i > 0; i--) {
				emit("," + mylist.get(i - 1).type + " " + mylist.get(i - 1).name);
			}
		}
		mylist.clear();
		emit(")\n");
		return new Node(t6,ret_type);
	}
	/**
	 * f0 -> Expression()
	 * f1 -> ExpressionTail()
	 */
	public Node visit(ExpressionList n, String argu) throws Exception {
		Node obj=n.f0.accept(this, "right");
		argStack.push(obj);
		n.f1.accept(this, "right");
		return null;
	}

	/**
	 * f0 -> ( ExpressionTerm() )*
	 */
	public Node visit(ExpressionTail n, String argu) throws Exception {
		if(n.f0.present()) {
			n.f0.accept(this, argu);
		}
		return null;
	}

	@Override
	public Node visit(Block n, String argu) throws Exception {
		if(n.f1.present()) n.f1.accept(this,argu);
		return null;
	}

	/**
	 * f0 -> ","
	 * f1 -> Expression()
	 */
	public Node visit(ExpressionTerm n, String argu) throws Exception {
		Node obj=n.f1.accept(this, argu);
		argStack.push(obj);
		return null;
	}

	/**
	 * f0 -> "new"
	 * f1 -> Identifier()
	 * f2 -> "("
	 * f3 -> ")"
	 */
	public Node visit(AllocationExpression n, String argu) throws Exception {
		String s=n.f1.f0.toString();
		currObjType=s;
		String t1=getVar();
		String t2=getVar();
		String t3=getVar();
		String parent=MySymbolTable.parent_names.get(s);
		Map<String,MyType> mymap=new LinkedHashMap<>();
		if(MySymbolTable.methodSymTable.containsKey(parent)){
			mymap=MySymbolTable.methodSymTable.get(parent);
		}
		if(MySymbolTable.methodSymTable.containsKey(s)) {
			mymap.putAll(MySymbolTable.methodSymTable.get(s));
		}
		int vtableSize;
		if(!mymap.isEmpty())vtableSize=mymap.size();
		else vtableSize=0;
		int size=8;
		if(MySymbolTable.varSymTable.containsKey(s)) {
			for (Map.Entry<String, MyType> entry : MySymbolTable.varSymTable.get(s).entrySet()) {
				size += entry.getValue().size;
			}
		}

		emit(t1+" = call i8* @calloc(i32 1, i32 "+size+")\n"+
		t2+" = bitcast i8* "+t1+" to i8***\n"+
		t3+" = getelementptr ["+vtableSize+"x i8*], "+"["+vtableSize+"x i8*]* "+
		"@."+s+"_vtable, i32 0,i32 0\n"+
		"store i8** "+t3+",i8***"+t2+"\n");
		return new Node(t1,"i8*");
	}
	/**
	 * f0 -> "while"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> Statement()
	 */
	public Node visit(WhileStatement n, String argu) throws Exception {
		String top=getLabel();
		String end=getLabel();
		String body=getLabel();
		emit("br label %"+top+"\n"+top+":\n");
		Node condition=n.f2.accept(this, argu);
		emit("br i1 "+condition.name+", "+"label %"+body+", label %"+end+"\n"+
		body+":\n");
		n.f4.accept(this, argu);
		emit("br label %"+top+"\n"+
		end+":\n");

		return null;
	}
	/**
	 * f0 -> "new"
	 * f1 -> "int"
	 * f2 -> "["
	 * f3 -> Expression()
	 * f4 -> "]"
	 */
	public Node visit(ArrayAllocationExpression n, String argu) throws Exception {
		String var=getVar();
		String var1=getVar();
		String var2=getVar();
		Node object=n.f3.accept(this, argu);
		emit(var+" = add i32 "+object.name+", 1\n"+
		var1+" = call i8* @calloc(i32 4,i32 "+var+")\n"+
		var2+" = bitcast i8* "+var1+" to i32*\n"+
		"store i32 "+object.name+", i32* "+var2+"\n");
		return new Node(var2,object.type+"*");
	}
	/**
	 * f0 -> Identifier()
	 * f1 -> "["
	 * f2 -> Expression()
	 * f3 -> "]"
	 * f4 -> "="
	 * f5 -> Expression()
	 * f6 -> ";"
	 */
	public Node visit(ArrayAssignmentStatement n, String argu) throws Exception {
		Node l1=n.f0.accept(this, argu);
		Node br=n.f2.accept(this, argu);
		Node object=n.f5.accept(this, argu); //type returned is i32*
		String l=getLabel();
		String l2=getLabel();
		String end=getLabel();
		String t1=getVar();
		String t2=getVar();
		String t3=getVar();
		String t4=getVar();
		String t5=getVar();
		emit(t1+" = load i32*, i32** "+l1.name+"\n"+
		t2+" = load i32,i32* "+t1+"\n"+
		t3+" = icmp slt i32 "+br.name+", "+t2+"\n"+
		"br i1 "+t3+",label %"+l+", label %"+l2+"\n"+
		l+":\n"+
		t4+" = add i32 "+br.name+", 1\n"+
		t5+" = getelementptr i32,i32* "+t1+",i32 "+t4+"\n"+
		"store i32 "+object.name+",i32* "+t5+"\n"+
		"br label %"+end+"\n"+
		l2+":\n"+
		"call void @throw_oob()\nbr label %"+end+"\n"+
		end+":\n");
		return null;
	}
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "["
	 * f2 -> PrimaryExpression()
	 * f3 -> "]"
	 */
	public Node visit(ArrayLookup n, String argu) throws Exception {
		emit(";start the lookup\n");
		Node obj=n.f0.accept(this, argu);
		Node brackets=n.f2.accept(this, argu);
		emit(";got the lookup\n");
		String l=getLabel();
		String l2=getLabel();
		String end=getLabel();
		String t1=getVar();
		String t2=getVar();
		String t3=getVar();
		String t4=getVar();
		String t5=getVar();
		emit(t1+" = load i32, i32* "+obj.name+"\n"+
		t3+" = icmp slt i32 "+brackets.name+", "+t1+"\n"+
		"br i1 "+t3+",label %"+l+", label %"+l2+"\n"+
		l+":\n"+
		t5+" = add i32 "+brackets.name+",1\n"+
		t4+"= getelementptr i32,i32* "+obj.name+",i32 "+t5+"\n"+
		t2+" = load i32 ,i32*"+t4+"\n"+
		"br label %"+end+"\n"+
		l2+":\n"+
		"call void @throw_oob()\nbr label %"+end+"\n"+
		end+":\n"
		);
		return new Node(t2,"i32");
	}
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "."
	 * f2 -> "length"
	 */
	public Node visit(ArrayLength n, String argu) throws Exception {
		Node obj=n.f0.accept(this, argu);
		String t1=getVar();
		emit(t1+" = load i32,i32* "+obj.name+"\n");
		return new Node(t1,"i32");
	}
	/**
	 * f0 -> "System.out.println"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> ";"
	 */
	public Node visit(PrintStatement n, String argu) throws Exception {
		Node object=n.f2.accept(this, argu);
		emit("call void(i32) @print_int(i32 "+object.name+")\n");
		return null;
	}
	/**
	 * f0 -> FormalParameter()
	 * f1 -> FormalParameterTail()
	 */
	public Node visit(FormalParameterList n, String argu) throws Exception {
		n.f0.accept(this, argu);
		n.f1.accept(this, argu);

		return null;
	}

	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 */
	public Node visit(FormalParameter n, String argu) throws Exception {
		n.f0.accept(this, argu);
		Node obj=n.f1.accept(this, argu);
		emit(","+obj.type+" %."+n.f1.f0.toString());
		return null;
	}

	/**
	 * f0 -> ( FormalParameterTerm() )*
	 */
	public Node visit(FormalParameterTail n, String argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	/**
	 * f0 -> <IDENTIFIER>
	 */
	public Node visit(Identifier n, String argu) throws Exception {
		String name=n.f0.toString();
		/*--------this is the case we have variable of class type----------*/
		if(argu!=null) if(argu.equals("TypeVisitor")){
			return new Node("%"+name,convert(name,argu));
		}
		/*--------search if it is method's local variable-----------*/

		if(argu!=null) if(argu.equals("PrimaryExpression")) {
			String s1=getVar();
			Node object =getLocalVar(name,"right");
			//Node object2=getLocalVar(name,"left");
			emit(s1+" = load "+object.type+", "+ " "+ object.type+"* "+object.name+"\n");
			return new Node(s1,object.type);
		}

		return getLocalVar(name,argu);


	}
	/**
	 * f0 -> "if"
	 * f1 -> "("
	 * f2 -> Expression()
	 * f3 -> ")"
	 * f4 -> Statement()
	 * f5 -> "else"
	 * f6 -> Statement()
	 */
	public Node visit(IfStatement n, String argu) throws Exception {
		Node object1=n.f2.accept(this, argu);
		String v1=getVar();
		String l1=getLabel();
		String l2=getLabel();
		String l3=getLabel();
		emit("br i1 "+object1.name+" ,"+"label %"+l1+", "+"label %"+l2+"\n");
		emit(l1+":\n");
		n.f4.accept(this, argu);
		emit("br label "+"%"+l3+"\n");
		emit(l2+":\n");
		n.f6.accept(this, argu);
		emit("br label "+"%"+l3+"\n");
		emit(l3+":\n");

		return null;
	}

	/**
	 * f0 -> Identifier()
	 * f1 -> "="
	 * f2 -> Expression()
	 * f3 -> ";"
	 */
	public Node visit(AssignmentStatement n, String argu) throws Exception {
		Node l1=n.f0.accept(this, "left");
		Node l2=n.f2.accept(this, "right");
		emit("store "+l2.type+" "+l2.name+" , "+l1.type+" "+l1.name+"\n\n");
		return null;
	}
	/**
	 * f0 -> AndExpression()
	 *       | CompareExpression()
	 *       | PlusExpression()
	 *       | MinusExpression()
	 *       | TimesExpression()
	 *       | ArrayLookup()
	 *       | ArrayLength()
	 *       | MessageSend()
	 *       | Clause()
	 */
	public Node visit(Expression n, String argu) throws Exception {
		return n.f0.accept(this, argu);
	}
	/**
	 * f0 -> IntegerLiteral()
	 *       | TrueLiteral()
	 *       | FalseLiteral()
	 *       | Identifier()
	 *       | ThisExpression()
	 *       | ArrayAllocationExpression()
	 *       | AllocationExpression()
	 *       | BracketExpression()
	 */
	public Node visit(PrimaryExpression n, String argu) throws Exception {
		return n.f0.accept(this, "PrimaryExpression");
	}
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "+"
	 * f2 -> PrimaryExpression()
	 */
	public Node visit(PlusExpression n, String argu) throws Exception {
		Node r1=n.f0.accept(this, "right");
		Node r2=n.f2.accept(this, "right");
		String l=getVar();
		emit(l+" = add "+ r1.type+" "+r1.name+", "+r2.name+"\n");
		return new Node(l,"i32");
	}
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "-"
	 * f2 -> PrimaryExpression()
	 */
	public Node visit(MinusExpression n, String argu) throws Exception {
		Node r1=n.f0.accept(this, "right");
		Node r2=n.f2.accept(this, "right");
		String l=getVar();
		emit(l+" = sub "+ r1.type+" "+r1.name+", "+r2.name+"\n");
		return new Node(l,"i32");
	}
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "*"
	 * f2 -> PrimaryExpression()
	 */
	public Node visit(TimesExpression n, String argu) throws Exception {
		Node r1=n.f0.accept(this, "right");
		Node r2=n.f2.accept(this, "right");
		String l=getVar();
		emit(l+" = mul "+ r1.type+" "+r1.name+", "+r2.name+"\n");
		return new Node(l,"i32");
	}
	/**
	 * f0 -> Clause()
	 * f1 -> "&&"
	 * f2 -> Clause()
	 */
	public Node visit(AndExpression n, String argu) throws Exception {
		Node o1=n.f0.accept(this, argu);
		String l1=getLabel();
		String l2=getLabel();
		String r=getVar();
		String v2=getVar();
		String v3=getVar();
		emit(r+" = "+"alloca i1\n"); //I alocate memory space for r
		emit("store i1 "+o1.name+", i1* "+r+"\n"+  // r = t1
		v2+" = "+"icmp eq i1 "+o1.name+", 0\n"+
		"br i1 "+v2+" ,"+"label %"+l1+", "+"label %"+l2+"\n"); //if(t1 ==1)
		emit(l2+":\n");
		Node o2=n.f2.accept(this, argu); //then
		emit("store i1 "+o2.name+", i1* "+r+"\n"+ //r = t2
		"br "+"label %"+l1+"\n");
		emit(l1+":\n");
		emit(v3+" = load i1, i1* "+r+"\n");
		return new Node(v3,"i1");
	}
	/**
	 * f0 -> PrimaryExpression()
	 * f1 -> "<"
	 * f2 -> PrimaryExpression()
	 */
	public Node visit(CompareExpression n, String argu) throws Exception {
		Node object1=n.f0.accept(this, argu);
		Node object2=n.f2.accept(this, argu);
		String t1=getVar();
		emit(t1+" = "+"icmp slt i32 "+object1.name+ ", "+ object2.name+"\n");
		return new Node(t1,"i1");
	}
	/**
	 * f0 -> Type()
	 * f1 -> Identifier()
	 * f2 -> ";"
	 */
	public Node visit(VarDeclaration n, String argu) throws Exception {
		n.f0.accept(this, argu);
		Node var=n.f1.accept(this, argu);
		emit(var.name+" = alloca "+var.type+"\n");
		return null;
	}

	/**
	 * f0 -> ArrayType()
	 *       | BooleanType()
	 *       | IntegerType()
	 *       | Identifier()
	 */
	public Node visit(Type n, String argu) throws Exception {
		return n.f0.accept(this, "TypeVisitor");
	}
	/**
	 * f0 -> "int"
	 * f1 -> "["
	 * f2 -> "]"
	 */
	public Node visit(ArrayType n, String argu) throws Exception {
		return new Node("null",convert("int[]",argu));
	}

	/**
	 * f0 -> "boolean"
	 */
	public Node visit(BooleanType n, String argu) throws Exception {
		return  new Node("null",convert("boolean",argu));
	}

	/**
	 * f0 -> "int"
	 */
	public Node visit(IntegerType n, String argu) throws Exception {
		return  new Node("null",convert("int",argu));
	}
	/**
	 * f0 -> <INTEGER_LITERAL>
	 */
	public Node visit(IntegerLiteral n, String argu) throws Exception {
		return new Node(n.f0.toString(),"i32");
	}

	@Override
	public Node visit(TrueLiteral n, String argu) throws Exception {
		return new Node("1","i1");
	}

	@Override
	public Node visit(FalseLiteral n, String argu) throws Exception {
		return new Node("0","i1");
	}

	@Override
	public Node visit(ThisExpression n, String argu) throws Exception {
		currObjType=curClass;
		return new Node("%this","i8*");
	}
	/**
	 * f0 -> "!"
	 * f1 -> Clause()
	 */
	public Node visit(NotExpression n, String argu) throws Exception {
		Node object=n.f1.accept(this, argu);
		String v=getVar();
		emit(v+" = "+"xor "+object.type+" "+object.name+", 1\n");
		return new Node(v,"i1");
	}
	/**
	 * f0 -> NotExpression()
	 *       | PrimaryExpression()
	 */
	public Node visit(Clause n, String argu) throws Exception {
		return n.f0.accept(this, argu);
	}

	@Override
	public Node visit(Statement n, String argu) throws Exception {
		emit("\n");
		return n.f0.accept(this,argu);
	}
	/**
	 * f0 -> "("
	 * f1 -> Expression()
	 * f2 -> ")"
	 */
	public Node visit(BracketExpression n, String argu) throws Exception {
		return n.f1.accept(this, argu);
	}
}