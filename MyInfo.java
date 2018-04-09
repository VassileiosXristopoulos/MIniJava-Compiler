import java.util.*;
public class MyInfo{
	Map<String,Integer> variables;
	Map<String,Integer> methods;
	MyInfo(){
		variables=new LinkedHashMap<String,Integer>();
		methods=new LinkedHashMap<String,Integer>();
	}
	public void addVariable(String s,int o){
		variables.put(s,o);
	}
	public void addMethod(String s,int o){
		methods.put(s,o);
	}
	public int returnMethod(String name){
		int _ret;
		if(methods.containsKey(name)) _ret=methods.get(name);
		else _ret=-1;
		return _ret;
	}
	public int returnVariable(String name){ /* -------- for hw3 -------- */
		int _ret;
		if(variables.containsKey(name)) _ret=variables.get(name);
		else _ret=-1;
		return _ret;
	}

}