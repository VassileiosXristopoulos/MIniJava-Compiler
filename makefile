all: compile

compile:
	java -jar jtb132di.jar -te miniJava.jj
	java -jar javacc5.jar miniJava-jtb.jj
	javac Main.java  MySymbolTable.java MyType.java Funct.java
	

clean:
	rm -f *.class *~
