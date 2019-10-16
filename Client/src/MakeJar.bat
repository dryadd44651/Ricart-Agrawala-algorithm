set fileName=Client
javac -g %fileName%.java
jar cfm Client.jar ClientMANIFEST.MF *.class
del *.class
pause

