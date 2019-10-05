set fileName=Server

javac -g %fileName%.java

jar cfm Server.jar ServerMANIFEST.MF *.class

pause

