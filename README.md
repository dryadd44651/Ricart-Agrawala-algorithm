<h1>Distributed System based on Ricart-Agrawala-algorithm</h1>

<pre>
- Java based Document Distributed System
-	Implemented Java Socket (TCP/IP) for Ricart-Agrawala-algorithm


MakeJar.bat: compile the Server.java and Client.java
RunJar.bat: run the Server.jar and Client.java


//for Client.jar
javac -g Client.java
jar cfm Client.jar ClientMANIFEST.MF *.class
//for Server.jar
javac -g Server.java
jar cfm Server.jar ClientMANIFEST.MF *.class

dc01~dc08 are 8 different machines
Log in dc01~dc03(for Server.jar), dc04~08(for Client.jar).
java -jar Server.jar 0 =>this mean run server 0 (dc01)
java -jar Server.jar 1 =>this mean run server 1 (dc02)
java -jar Server.jar 2 =>this mean run server 1 (dc03)
java -jar Client.jar 0 =>this mean run Client 0 (dc04)
...



Command: exit, enquiry, read, write command must end with ;
USAGE: [command] [filename] [times];
If you want to test same read/write several times, enter the number in [times].
ex:
enquiry;
read 0.txt;
read 0.txt 5;
write 1.txt 6;
test 2.txt; (randomly test read/write 20 times)

for more detail please check <a href="https://github.com/dryadd44651/Ricart-Agrawala-algorithm/blob/master/CS6378_Fall2019_Project1_.pdf"> CS6378_Fall2019_Project1_.pdf</a>


</pre>





