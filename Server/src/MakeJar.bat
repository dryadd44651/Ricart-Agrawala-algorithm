set fileName=Server

javac -g %fileName%.java

jar cfm Server.jar MANIFEST.MF *.class

pause

