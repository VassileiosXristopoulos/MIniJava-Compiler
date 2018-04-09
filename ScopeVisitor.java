import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;
import java.io.*;


public class ScopeVisitor extends GJDepthFirst<String,Map<String,MyType>>{
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
   public String visit(MainClass n, Map<String,MyType>arg) throws Exception {
      String _ret=null;
      String class_name=n.f1.accept(this,MySymbolTable.emptymap);
      MySymbolTable.parent_names.put(class_name,class_name);
      MySymbolTable.classStack.push(class_name);
     if(n.f14.present()){ //if we have variable declarations
     	Map<String,MyType> mymap=new LinkedHashMap<String,MyType>();
     	MySymbolTable.varSymTable.put(class_name, mymap);
     	n.f14.accept(this, mymap);
  	 }
  	 MySymbolTable.classStack.pop();
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
   public String visit(ClassDeclaration n, Map<String,MyType>arg) throws Exception {
      String _ret=null;
	  String class_name=n.f1.f0.toString();
	  MySymbolTable.classStack.push(class_name);
	  MySymbolTable.parent_names.put(class_name,class_name);
      if(n.f3.present()){// if we have variable declarations
      	Map<String,MyType>mymap=new LinkedHashMap<String,MyType>();
	 	MySymbolTable.varSymTable.put(class_name,mymap );
     	n.f3.accept(this, mymap);
  	  }
      if(n.f4.present()){//if we have method declarations
      	Map<String,MyType>mymap=new LinkedHashMap<String,MyType>();
      	MySymbolTable.methodSymTable.put(class_name,mymap);
      	n.f4.accept(this, mymap);
      }
      MySymbolTable.myPrint(class_name,_ret);
      MySymbolTable.classStack.pop();
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
   public String visit(ClassExtendsDeclaration n, Map<String,MyType>arg) throws Exception { //regular storing at symbol table, without superclass info
       String _ret=null;
       String class_name=n.f1.f0.toString();
       String super_name=n.f3.f0.toString();
       MySymbolTable.parent_names.put(class_name,super_name);
       MySymbolTable.classStack.push(super_name);
       MySymbolTable.classStack.push(class_name);
       if(n.f5.present()){// if we have variable declarations
      	Map<String,MyType>mymap=new LinkedHashMap<String,MyType>();
	 	MySymbolTable.varSymTable.put(class_name,mymap );
     	String type=n.f5.accept(this, mymap);  	    }
      if(n.f6.present()){//if we have method declarations
      	Map<String,MyType>mymap=new LinkedHashMap<String,MyType>();
      	MySymbolTable.methodSymTable.put(class_name,mymap);
      	n.f6.accept(this, mymap);
        }
        MySymbolTable.myPrint(class_name,super_name);
        MySymbolTable.classStack.pop();
        MySymbolTable.classStack.pop();
       return _ret;
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
   public String visit(MethodDeclaration n, Map<String,MyType>mymap)throws Exception {
     int num;
     String _ret=null;
     String type=n.f1.accept(this,MySymbolTable.emptymap);
     String identifier= n.f2.accept(this, MySymbolTable.emptymap);
     String parent=MySymbolTable.classStack.get(0);
     String class_type=MySymbolTable.classStack.get(0);
     if(MySymbolTable.classStack.size()>1) class_type=MySymbolTable.classStack.get(1);

     Funct t=new Funct(type,parent,8,class_type);
     mymap.put(identifier,t);
      if(n.f4.present()){
     	n.f4.accept(this, t.args);
  	  }  
	  if(n.f7.present()){
     	n.f7.accept(this, t.map);
  	  }      
  	  if(n.f8.present()){
  	  	n.f8.accept(this,t.map);
  	  }
  	  MySymbolTable.methStack.push(mymap);
      return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public String visit(VarDeclaration n, Map<String,MyType>mymap)throws Exception {
     String _ret=null;
     String type= n.f0.accept(this, MySymbolTable.emptymap);
     String identifier= n.f1.accept(this, MySymbolTable.emptymap);
     MyType temp=mymap.get(identifier);
     if(temp != null){
     	throw new Exception("previously declared variable "+identifier);
     }
     mymap.put(identifier,new MyType(type,type,MySymbolTable.returnFieldsize(type)));
     return _ret;
   }
   /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
   public String visit(Type n, Map<String,MyType>mymap)throws Exception {
	  String var= n.f0.accept(this,MySymbolTable.emptymap);
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
	  String var= n.f0.toString();
      return var;
   }


  /*   /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, Map<String,MyType>mymap)throws Exception {
      String var= n.f0.toString();
      return var;
   }
   /**
    * f0 -> FormalParameter()
    * f1 -> FormalParameterTail()
    */
   public String visit(FormalParameterList n, Map<String,MyType>mymap) throws Exception{
      n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      return null;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, Map<String,MyType>mymap) throws Exception{
     String _ret=null;
     String type=n.f0.accept(this, mymap);
     String var= n.f1.accept(this, mymap);
     mymap.put(var,new MyType(type,MySymbolTable.classStack.get(0),0));
      return null;
   }

   /**
    * f0 -> ( FormalParameterTerm() )*
    */
   public String visit(FormalParameterTail n, Map<String,MyType>mymap) throws Exception{
       n.f0.accept(this, mymap);
      return null;
   }

   /**
    * f0 -> ","
    * f1 -> FormalParameter()
    */
   public String visit(FormalParameterTerm n,Map<String,MyType>mymap)throws Exception {
      String _ret=null;
      n.f0.accept(this, mymap);
      n.f1.accept(this, mymap);
      return null;
   }
  

}
