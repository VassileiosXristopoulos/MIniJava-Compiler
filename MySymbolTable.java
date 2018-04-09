import java.util.*;

public class MySymbolTable{
	/*  Map to store the superclass name for every class--if no superclass value is itself*/
	static Map<String,String>parent_names=new LinkedHashMap<String,String>();
	/* Map to store the previous sum at counting fields for offsets*/
	static Map<String,Integer> field_previous=new LinkedHashMap<String,Integer>();
	/* Map to store the previous sum at counting methods for offsets*/
	static Map<String,Integer> method_previous=new LinkedHashMap<String,Integer>();
	/* an empty map just for my convenience*/
	static Map<String,MyType>emptymap=new LinkedHashMap<String,MyType>(); 
	/*this map has Class name for Key and contains a Map which has field names for Key and MyType is it's type */
	static Map<String,Map<String,MyType>>varSymTable=new LinkedHashMap<String,Map<String,MyType>>();
	/* this map has Class name for Key and cointains a Map which has method names for Key and contains MyType (aka Func) as values*/
	static Map<String,Map<String,MyType>>methodSymTable=new LinkedHashMap<String,Map<String,MyType>>();
	/* this stack has the until now declared classes while type-checking, is used to get the class that we are into and to stop if an "extends" is not valid*/
	static Stack<String>classStack=new Stack<String>();
	/* this stack saves ALL methods (all methods are public) so as to access them when needed*/
	static Stack<Map<String,MyType>>methStack=new Stack<Map<String,MyType>>();
	/* this stack is used when checking method-message_send arguments */
	static Stack<String> argumentStack=new Stack<String>();
	/* this map has keys class names and an object with 2 maps, one for variables and one for methods */
	static Map<String,MyInfo>allOfsets=new LinkedHashMap<String,MyInfo>();


	static void emptyAll(){
		parent_names.clear();
		field_previous.clear();
		method_previous.clear();
		varSymTable.clear();
		methodSymTable.clear();
		classStack.removeAllElements();
		methStack.removeAllElements();
		argumentStack.removeAllElements();
		TypeChecker.myStack.removeAllElements();
		allOfsets.clear();
	}
	/*this function gets 2 symbol tables, map1 is the superclass and map2 the subclass and check if there are same names, if yes then checks definitions  */
	static void checkMethods(Map<String,MyType>map1,Map<String,MyType>map2)throws Exception{
		for (Map.Entry<String, MyType> entry1 : map1.entrySet()){
			for(Map.Entry<String,MyType>entry2 : map2.entrySet()){
    			if((entry1.getKey()).equals(entry2.getKey())){ 
    				Funct o1=(Funct)(map1.get(entry1.getKey()));
    				Funct o2=(Funct)(map2.get(entry2.getKey()));
    				if(((o1.type).equals(o2.type))||((o1.parent).equals(o2.type))){ //return types are the same
    					if((o1.args.size())==(o2.args.size())){  //i pass the arguments to lists in order to iterate easier
    						List<MyType> types1 = new LinkedList<MyType>();
	    					List<MyType> types2 = new LinkedList<MyType>();
    						for (Map.Entry<String, MyType> entry3 : o1.args.entrySet()){
								types1.add(entry3.getValue());
							}
							for(Map.Entry<String,MyType>entry4 : o2.args.entrySet()){
								types2.add(entry4.getValue());
							}
							for(int i=0;i<types1.size();i++){
	    						if(! ((types1.get(i).type).equals(types2.get(i).type))) {
	    							throw new Exception("error at function arguments");
	    						}
	    					}
    					}

    				}
    				else throw new Exception("error at functions return type");
    			} 

			}
		}
	}



	/*this function is only called by Identifier() visit at MyTypeChecker IF the identifier is NOT in scope*/
	/*We search the stack for an identifiers type, if we find it and it is primitive type, we return it, else we return superclass type */
	/*if we didn't find identifier with such name, we might be in case such MyClass func(....) and we want to return MyClass so we call ckekClassName  */
	public static String searchInStack(String identifier)throws Exception{ 
		Stack<Map<String,MyType>>newStack=new Stack<Map<String,MyType>>();
		newStack.addAll(methStack);
		newStack.addAll(TypeChecker.myStack);
		int size=newStack.size();
	      	for( int i=newStack.size()-1 ; i>=0 ; i-- ){ //go through all the stack
	      		Map<String,MyType> tempMap=newStack.get(i);
	      		if(tempMap!=null) {
		      		if(tempMap.containsKey(identifier)){ //i found it!!
		      			MyType obj=tempMap.get(identifier);
		      			String type=obj.type;
		      			if(!((type.equals("int"))||(type.equals("boolean"))||(type.equals("int[]")))) return parent_names.get(type) ;
		      			return type;
		      		}
		      	}
	      	}
	      	//did'nt find name of identifier, there is the case of return type class Name for Type visitor so I check for this
	      	String type=checkClassName(identifier);
	      	if(type==null){
	      		throw new Exception("couldn't find "+identifier);
	      	}
	      	return parent_names.get(type);
	      	
	}
	/*this function returns if the class name we search is valid */
	public static String checkClassName(String identifier){
    	if((varSymTable.containsKey(identifier))||(methodSymTable.containsKey(identifier))||(identifier.equals("this"))){
    		return identifier;
    	}
    	else if((varSymTable.containsKey(parent_names.get(identifier)))||(methodSymTable.containsKey(parent_names.get(identifier)))){
    		return parent_names.get(identifier);
    	}
    	else{
    		return null;
    	}

	}
	/*this function is only for offsets and sets the size of each object (function, variable of each type) */
	public static int returnFieldsize(String type){
		if(type.equals("int")) return 4;
		if(type.equals("boolean")) return 1;
		/* if continued we have either int[] or class_type */
		return 8;
	} 
	/*this function operates the printing of the offsets */
	/*takes arguments subclass-superclass | superclass might be null because this class has not superclass */
	/*if we dont have superclass we iterate through the symbol table and print*/
	/*if we have superclass (after we check the case it has no fields-methods) we check for same fields-methods so as not to count them */
	public static void myPrint(String subclass, String superclass){
		MyInfo myinfo= new MyInfo();

		System.out.println("-------------- CLASS "+ subclass +" ----------------");
		System.out.println("---Variables---");
		int previous=0;
		if(superclass==null){
			Map<String,MyType>mymap=varSymTable.get(subclass);
			if(mymap!=null){
				for (Map.Entry<String, MyType> entry1 : mymap.entrySet()){
					MyType obj= entry1.getValue();
					String name=entry1.getKey();
					System.out.println(subclass+"."+name+":"+previous);
					myinfo.addVariable(name,previous);
					previous+=obj.size;
				}
			}
			field_previous.put(subclass,previous);
			previous=0;
			System.out.println("---Methods---");
			mymap=methodSymTable.get(subclass);
			if(mymap!=null){
				for (Map.Entry<String, MyType> entry1 : mymap.entrySet()){
					MyType obj= entry1.getValue();
					String name=entry1.getKey();
					System.out.println(subclass+"."+name+":"+previous);
					myinfo.addMethod(name,previous);
					previous+=obj.size;
				}
				method_previous.put(subclass,previous);
			}
		}
		else{
			Map<String,MyType>varSubMap=varSymTable.get(subclass); //all classes variable declarations
			Map<String,MyType>methSubMap=methodSymTable.get(subclass); //all methods
			Map<String,MyType>methSuperMap=methodSymTable.get(superclass); //all superclasses methods
			int flag=0;;
			
			if(varSubMap!=null){ //we don't override class fields
				previous=0;
				for (Map.Entry<String, MyType> entry1 : varSubMap.entrySet()){
					MyType obj= entry1.getValue();
					String name=entry1.getKey();
					System.out.println(subclass+"."+name+":"+previous);
					myinfo.addVariable(name,previous);
					previous+=obj.size;	
				}	
				/*------write in st the fields of superclass */			
			} 
				/* ------------------ counted all fields ------------------------ */
				System.out.println("---Methods---");
				if(methSubMap!=null){
					if(methSuperMap!=null){
						previous=method_previous.get(superclass);
						for (Map.Entry<String, MyType> entry1 : methSubMap.entrySet()){
							for (Map.Entry<String, MyType> entry2 : methSuperMap.entrySet()){
								if((entry1.getKey()).equals(entry2.getKey())){
								 flag=1; /*this field (entry1.getKey()) is already counted*/
								 /*----write in st the same method -----*/
								 String name=entry1.getKey(); //method's name
								 int offset=allOfsets.get(superclass).returnMethod(name);
								 myinfo.addMethod(name,offset);
								}
							}
							if(flag==0){ /*---write it's own methods---- */
								MyType obj= entry1.getValue();
								String name=entry1.getKey();
								System.out.println(subclass+"."+name+":"+previous);
								myinfo.addMethod(name,previous);
								previous+=obj.size;
							}
							flag=0;
						}
					}
					else{/*---write it's own methods */
						previous=0;
						methSubMap=methodSymTable.get(subclass);
						for (Map.Entry<String, MyType> entry1 : methSubMap.entrySet()){
							MyType obj= entry1.getValue();
							String name=entry1.getKey();
							System.out.println(subclass+"."+name+":"+previous);
							myinfo.addMethod(name,previous);
							previous+=obj.size;
						}
					}
				}
			}
			allOfsets.put(subclass,myinfo); // put information in Map
	}
	


}




