set fileName=Client

javac -g %fileName%.java

jar cfm Client.jar MANIFEST.MF *.class

pause

