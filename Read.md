Put Server.jar in dc01~dc03, Client.jar in dc04~08.
java -jar Server.jar 0 =>this mean run server 0
java -jar Client.jar 0 =>this mean run Client 1



Command: exit, enquiry, read, write command must end with ;
USAGE: [command] [filename] [times];
If you want to test same read/write several times, enter the number in [times].
ex:
enquiry;
read 0.txt;
read 0.txt 5;
write 1.txt 6;



