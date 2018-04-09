import java.util.*;

public class Funct extends MyType{
	public String decl_type;// the class in which the method belongs -- if class B extends A{ public int foo() then decl_type for foo is B and parent is A
	public Map<String,MyType>map;
	public Map<String,MyType>args;
	Funct( String type,String parent,int size,String d){
		super(type,parent,size);
		map=new LinkedHashMap<String,MyType>();
		args=new LinkedHashMap<String,MyType>();
		decl_type=d;
	}
}
