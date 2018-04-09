import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import java.io.*;

public class TypeChecker extends GJDepthFirst<String,Map<String,MyType>>{
	static Stack<Map<String,MyType>>myStack=new Stack<Map<String,MyType>>();
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
   public String visit(MainClass n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      String class_name=n.f1.f0.toString();
      mymap = MySymbolTable.varSymTable.get(class_name);
      myStack.push(mymap); //i put all varDeclarations in Stack
      MySymbolTable.classStack.push(class_name);
      if(n.f14.present())n.f14.accept(this,mymap);
      if(n.f15.present())n.f15.accept(this,mymap); //visit statements
      myStack.pop(); //i pop the current scope
  	  
     return _ret;
   }
  /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
   public String visit(ClassDeclaration n, Map<String,MyType>mymap)throws Exception {
      String _ret=null;
	  String class_name=n.f1.f0.toString();
	  MySymbolTable.classStack.push(class_name);
	  mymap = MySymbolTable.varSymTable.get(class_name); //get the symbol table for the specific class 
	  myStack.push(mymap); //send in stack all the classes fields
	  mymap = MySymbolTable.methodSymTable.get(class_name);
	  if(n.f3.present())n.f3.accept(this,mymap);
      if(n.f4.present()){
      	 n.f4.accept(this, mymap); //give the symbol table of methods to the method, in order to get in it's scope
      }
      myStack.pop();
      return _ret;
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
   public String visit(ClassExtendsDeclaration n,Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      Map<String,MyType>newmap=new HashMap<String,MyType>();
      n.f0.accept(this, mymap);
      String class_name=n.f1.f0.toString();
      String super_name=n.f3.f0.toString();
      MySymbolTable.classStack.push(class_name);
      if((MySymbolTable.classStack.search(super_name))==-1){
      	throw new Exception("error at class declaration!!");
      }
      mymap=MySymbolTable.varSymTable.get(super_name);
      myStack.push(mymap);
      Map<String,MyType>supermap=MySymbolTable.methodSymTable.get(super_name);
      mymap=MySymbolTable.varSymTable.get(class_name);
      myStack.push(mymap);
      mymap=MySymbolTable.methodSymTable.get(class_name);
      if((mymap!=null) &&(supermap!=null)){ 
	      MySymbolTable.checkMethods(supermap,mymap);
	      newmap.putAll(supermap);
	      newmap.putAll(mymap);

	      n.f2.accept(this, mymap);
	      n.f4.accept(this, mymap);
	     if(n.f5.present()) n.f5.accept(this,newmap);
	      if(n.f6.present()){
	      	n.f6.accept(this, newmap);
	  	  }
	  	  myStack.pop();
	  	  myStack.pop();
	  	 // MySymbolTable.classStack.pop();
	      n.f7.accept(this, mymap);
  	 }
      return _ret;
   }
      /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public String visit(VarDeclaration n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      n.f1.accept(this, mymap);
      n.f2.accept(this, mymap);
      return _ret;
   }
 /**
    * f0 -> "public"returnType
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
   public String visit(MethodDeclaration n,Map<String,MyType>mymap) throws Exception { //mymap is a symbol table with all classes methods
      String _ret=null;
      String type=n.f1.accept(this, mymap);
      String name=n.f2.f0.toString();

      MyType obj=mymap.get(name);
	  Funct o=(Funct)mymap.get(name);
	  Map<String,MyType>newmap=new HashMap<String,MyType>();
	  newmap.putAll(o.args);
	  newmap.putAll(o.map); //local variables shadow the arguments AND local variables shadow outter variables because we check mymap's variables first at the identifier visitor!
	  if(n.f7.present()) n.f7.accept(this,newmap);
      if(n.f8.present())n.f8.accept(this, newmap); //i give the symbol table of the method's variable declarations and arguments
	  n.f4.accept(this, newmap);
	  n.f9.accept(this, mymap);
	  String retexp=n.f10.accept(this, newmap); 
	  if(!(retexp.equals(type))){
		 throw new Exception("error at return type");
	  }
      return _ret;
   }
   /**
    * f0 -> Block()
    *       | AssignmentStatement()
    *       | ArrayAssignmentStatement()
    *       | IfStatement()
    *       | WhileStatement()
    *       | PrintStatement()
    */
   public String visit(Statement n, Map<String,MyType>mymap) throws Exception {
      return n.f0.accept(this, mymap);
   }
    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   public String visit(AssignmentStatement n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      int position;
      String id_type=n.f0.accept(this, mymap); // the identifier's visitor returns his type
      n.f0.accept(this,mymap);
      String exp_type=n.f2.accept(this,mymap);
      n.f3.accept(this,mymap);
      if(id_type.equals(exp_type)){
      	return _ret;
      }
      else if((MySymbolTable.classStack.search(id_type)!=-1)&&(exp_type.equals("this"))){ // classObj=this case
      	return _ret;
      }
      else{
      	throw new Exception("error at types-assignment statement "+ id_type+" "+exp_type);
      }

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
   public String visit(Expression n, Map<String,MyType>mymap) throws Exception {
      return n.f0.accept(this, mymap);
   }

    /**
    * f0 -> NotExpression()
    *       | PrimaryExpression()
    */
   public String visit(Clause n, Map<String,MyType>mymap) throws Exception {
      return n.f0.accept(this, mymap);
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
   public String visit(PrimaryExpression n, Map<String,MyType>mymap) throws Exception {
      return n.f0.accept(this, mymap);
   }

     /**
    * f0 -> <INTEGER_LITERAL>
    */
   public String visit(IntegerLiteral n, Map<String,MyType>mymap) throws Exception {
      return "int";
   }

   /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
   public String visit(Type n, Map<String,MyType>mymap)throws Exception {
	  String var= n.f0.accept(this,mymap);
      return var;
   }

   /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
   public String visit(ArrayType n, Map<String,MyType>mymap)throws Exception {
      String _ret=null;
      String s1=n.f0.toString();
      String s2=n.f1.toString();
      String s3=n.f2.toString();
      return s1+s2+s3;
   }

   /**
    * f0 -> "boolean"
    */
   public String visit(BooleanType n, Map<String,MyType>mymap)throws Exception {
	  String var= n.f0.toString();
      return var;
   }

   /**
    * f0 -> "int"
    */
   public String visit(IntegerType n, Map<String,MyType>mymap)throws Exception{
   		return n.f0.toString();
   }
   /**
    * f0 -> "this"
    */
   public String visit(ThisExpression n, Map<String,MyType>mymap) throws Exception {
      return n.f0.toString();
   }
    /*   /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, Map<String,MyType>mymap)throws Exception {
      String identifier= n.f0.toString();

      if(mymap!=null){
     	if(mymap.containsKey(identifier)){ //if current scope has the identifier 
      		MyType obj=mymap.get(identifier);
      		String type=obj.type;
      		if((type.equals("int"))||(type.equals("boolean"))||(type.equals("int[]"))) return type;
      		return MySymbolTable.parent_names.get(type);
      	}
      	else{
      		return MySymbolTable.searchInStack(identifier);
      	}
      }
      	else{ 
     		return MySymbolTable.searchInStack(identifier);
      }
   }
     /**
    * f0 -> PrimaryExpression() //must return class name type or "this"
    * f1 -> "."
    * f2 -> Identifier() //must be method of programm
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   public String visit(MessageSend n, Map<String,MyType>mymap) throws Exception {
    String _ret=null;
    String type=n.f0.accept(this, mymap);
    n.f1.accept(this,mymap);
    String reference=MySymbolTable.checkClassName(type);
    /*reference is the variable's type casted to it's superclass-if it has one */
    if(reference!=null){ // reference type is valid
		n.f1.accept(this,mymap);
	    String meth_name=n.f2.f0.toString(); //codes method name
		String meth_type=n.f2.accept(this, mymap); //codes methods type
		/* I START CHECKING WETHER THE ARGUMENTS OF THE FUNCTION CALLED ARE VALID AND WETHER THE REFERENCE TYPE AND THE METHOD ARE OF THE SAME CLASS */
		int size=MySymbolTable.methStack.size();  
	    for( int i=MySymbolTable.methStack.size()-1 ; i>=0 ; i-- ){	
	     	Map<String,MyType> tempMap=MySymbolTable.methStack.get(i);
		   	 	if(tempMap!=null) {
		      		if(tempMap.containsKey(meth_name)){ //i found the methods symbol table, time to check the arguments
		      			Funct obj=(Funct)tempMap.get(meth_name); //obj is the DEFINITIONS information
		      			if((obj.parent.equals(type))||(type.equals("this"))||((obj.decl_type).equals(type))){
			      			 if(n.f4.present()){
			      			 	myStack.push(mymap);
			    	 			n.f4.accept(this, obj.args);
			    	 			myStack.pop(); /* i do this because i pass only the calls arguments to exprlist and i lose the methods scope */
			 	 			}
		 	 			return meth_type;
		 			}
		   		}
		   	}
	    }	
	}			     
				
     
      return _ret;
   }

 /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
   /*here I use the a stack to gather all arguments from ExpressionTail--merge them with Expression() and then check with the definition's types*/
   public String visit(ExpressionList n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      String type= n.f0.accept(this, mymap);
      MySymbolTable.argumentStack.push(type);
      n.f1.accept(this, mymap);
      if(MySymbolTable.argumentStack.size() != mymap.size()){
      	throw new Exception("error at argument number");
      }
      else{
      	List<MyType> l = new ArrayList<MyType>(mymap.values());
	    MyType obj;
      	for(int i=0;i< MySymbolTable.argumentStack.size() ;i++){
      		 obj=l.get(i);
      		String call_type=MySymbolTable.argumentStack.get(i); //the CODES type
      		String decl_type=obj.type; //the DEFINITION'S type
      		if(!((call_type.equals(decl_type))||(call_type.equals(obj.parent)))){
      			if(call_type.equals("this")){ //if not equal I check whether we have "this" expression
      				String temp=MySymbolTable.classStack.get(MySymbolTable.classStack.size()-1);
      				if((decl_type.equals(temp))||decl_type.equals(MySymbolTable.parent_names.get(temp))){
      					MySymbolTable.argumentStack.removeAllElements();
      				 return _ret; 
      				}
      			}
      			throw new Exception("error at call's arguments");
      		}
      	}
      }
      	MySymbolTable.argumentStack.removeAllElements();

      return _ret;
   }

   /**
    * f0 -> ( ExpressionTerm() )*
    */
   public String visit(ExpressionTail n, Map<String,MyType>mymap) throws Exception {
   		if(n.f0.present()){
   		 return n.f0.accept(this, mymap);
   		}
      	return null;
   }

   /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public String visit(ExpressionTerm n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      String type= n.f1.accept(this, mymap);
      MySymbolTable.argumentStack.push(type);
      return _ret;
   }


     /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n,  Map<String,MyType>mymap) throws Exception {
         String _ret=null;
	     String name=n.f1.f0.toString();
	     /*I check if we have a valid class name */
	    	String temp=MySymbolTable.checkClassName(name);
	    	if(temp!=null) return MySymbolTable.parent_names.get(temp);
	     throw new Exception("error in New statement");
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      String exp1=n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      String exp2= n.f2.accept(this, mymap);
      if((!exp1.equals("int"))||(!exp2.equals("int"))){
      	throw new Exception("error at comparing");
      }
      return "boolean";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      String exp1=n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      String exp2= n.f2.accept(this, mymap);
      if((!exp1.equals("int"))||(!exp2.equals("int"))){
      	throw new Exception("error at adding");
      }
      return "int";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n,Map<String,MyType>mymap) throws Exception {
       String _ret=null;
      String exp1=n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      String exp2= n.f2.accept(this, mymap);
      if((!exp1.equals("int"))||(!exp2.equals("int"))){
      	throw new Exception("error at substracting");
      }
      return "int";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, Map<String,MyType>mymap) throws Exception {
       String _ret=null;
      String exp1=n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      String exp2= n.f2.accept(this, mymap);
      if((!exp1.equals("int"))||(!exp2.equals("int"))){
      	throw new Exception("error at multiplying");
      }
      return "int";
   }

   /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
   public String visit(AndExpression n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      String cl1=n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      String cl2=n.f2.accept(this, mymap);
      if(!cl1.equals(cl2)) throw new Exception("error at andExpression");
      if(!cl1.equals("boolean")) throw new Exception("error at andExpression");
      return cl1;
   }
   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      n.f0.toString();
      String cl=n.f1.accept(this, mymap);
      if(!cl.equals("boolean"))throw new Exception("error at notClause");
      return "boolean";
   }
   /**
    * f0 -> "new"
    * f1 -> "int"argumentStack
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(ArrayAllocationExpression n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      n.f2.accept(this, mymap);
      String expr=n.f3.accept(this, mymap);
      n.f4.accept(this, mymap);
      if(!expr.equals("int")) throw new Exception("error at array allocation");
      return "int[]";
   }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public String visit(BracketExpression n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      n.f0.accept(this, mymap);
      String s1=n.f1.accept(this, mymap);
      n.f2.accept(this, mymap);
      return s1;
   }
     /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public String visit(ArrayLookup n,Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      String id=n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      String expr=n.f2.accept(this, mymap);
      n.f3.accept(this, mymap);
      if(id.indexOf("[]")==-1){
        throw new Exception("error at arrayLookup");
      }
      else if(!expr.equals("int")) {
      	throw new Exception("error at arrayLookup");
      }
      return "int";
   }

      /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      String id=n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      n.f2.accept(this, mymap);
      if(id.indexOf("[]")==-1) throw new Exception("error at array arrayLength");
      return "int";
   }

/**
    * f0 -> "{"
    * f1 -> ( Statement() )*
    * f2 -> "}"
    */
   public String visit(Block n, Map<String,MyType>mymap) throws Exception {
      String t=null;
      if(n.f1.present()) t= n.f1.accept(this, mymap);
      return t;
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
   public String visit(ArrayAssignmentStatement n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      String arr=n.f0.accept(this, mymap);
      if(arr.indexOf("[]")==-1) throw new Exception("error at array arrayAsignment");
      n.f1.accept(this, mymap);
      String exp1=n.f2.accept(this, mymap);
      if(!exp1.equals("int")) throw new Exception("error at array arrayAsignment");
      n.f3.accept(this, mymap);
      n.f4.accept(this, mymap);
      String exp2=n.f5.accept(this, mymap);
      if(!exp2.equals("int")) throw new Exception("error at array arrayAsignment");
      n.f6.accept(this, mymap);
      return _ret;
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
   public String visit(IfStatement n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      String exp=n.f2.accept(this, mymap);
      if(!exp.equals("boolean")) throw new Exception("error at ifStatement");
      n.f3.accept(this, mymap);
      n.f4.accept(this, mymap);
      n.f5.accept(this, mymap);
      n.f6.accept(this, mymap);
      return _ret;
   }

   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public String visit(WhileStatement n,Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      String exp=n.f2.accept(this, mymap);
      if(!exp.equals("boolean")) throw new Exception("error at ifStatement");
      n.f3.accept(this, mymap);
      n.f4.accept(this, mymap);
      return _ret;
   }

   /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public String visit(PrintStatement n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      String exp=n.f2.accept(this, mymap);
      if(!exp.equals("int")) throw new Exception("error at println");
      n.f3.accept(this, mymap);
      n.f4.accept(this, mymap);
      return _ret;
   }

    /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
   public String visit(FormalParameterList n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      n.f1.accept(this,mymap);    
      return _ret;
   }

   /**
    * f0 -> ( FormalParameterTerm() )*
    */
   public String visit(FormalParameterTail n, Map<String,MyType>mymap) throws Exception {
      n.f0.accept(this, mymap);
      return null;
   }

   /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
   public String visit(FormalParameterTerm n, Map<String,MyType>mymap) throws Exception {
      String _ret=null;
      n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      return _ret;
   }

     /**
    * f0 -> "true"
    */
   public String visit(TrueLiteral n, Map<String,MyType>mymap) throws Exception {
      return "boolean";
   }

   /**
    * f0 -> "false"
    */
   public String visit(FalseLiteral n, Map<String,MyType>mymap) throws Exception {
      return "boolean";
   }
}