import syntaxtree.*;
import visitor.*;
import java.io.*;
import java.util.*;
import java.lang.*;


class Main {

    public static void main (String [] args)throws Exception{
    	MySymbolTable.emptymap.put("this cannot be key in any case",new MyType("this cannot be a type in any case","never",0)); 

		for(int i=0; i<args.length ;i++){
			System.out.println("\n****** FILE "+(i+1)+" ******");
			FileInputStream fis = null;
			try{
			    fis = new FileInputStream(args[i]);
			    MiniJavaParser parser = new MiniJavaParser(fis);
			    ScopeVisitor eval = new ScopeVisitor();
			    TypeChecker eval1= new TypeChecker();
			    Goal root = parser.Goal();
			    root.accept(eval, MySymbolTable.emptymap);
			    root.accept(eval1, MySymbolTable.emptymap); 

			    int pos = args[i].indexOf(".java");
			    if(pos==-1) throw new Exception();
			    String name = args[i].substring(0, pos);
			    String filename= name+".ll";
			    File dir = new File(filename);
			    FileWriter fileWriter=null;
			    fileWriter = new FileWriter(dir);
			    LLVMvisitor conv= new LLVMvisitor(fileWriter);
			    root.accept(conv,null);
			    try {
			    	 conv.myClose();

					if (fileWriter != null)
						fileWriter.close();

				} 
				catch (IOException ex) {
					System.out.println("error at closing file writer");
				}

			}
			catch(ParseException ex){
			    System.out.println(ex.getMessage());
			}
			catch(FileNotFoundException ex){
			    System.err.println(ex.getMessage());
			}
			finally{
			    try{
				if(fis != null) fis.close();
			    }
			    catch(IOException ex){
				System.err.println(ex.getMessage());
			    }
			}
			MySymbolTable.emptyAll();
		}
    }

}


