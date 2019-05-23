import com.sun.nio.sctp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

//import static java.net.SocketOptions.SO_RCVBUF;

import static com.sun.nio.sctp.SctpStandardSocketOptions.*;
public class SCTPSingleServer extends Thread {
    static int SERVER_PORT = 4477;


    @Override
    public void run() {
        try {
            serverRun();
        } catch (Exception e) {
            System.out.println("Something went wrong with Server" + e);
            e.printStackTrace();
        }
    }

    public static void serverRun() throws IOException {

        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);
        while (true) {
            File myFile = new File("FileToSend.tar.gz");

            double packetSizeDouble = 10240;
            int packetSize = 10240;
            long fileSize = myFile.length();
            int bytesLeft;


            long fileLength = myFile.length();
            float packetsToSend = (float) Math.ceil(fileLength / packetSizeDouble);
            bytesLeft = (int)fileSize;
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));

            SctpChannel sc = ssc.accept();
            sc.setOption(SO_SNDBUF, 212992);
            for(int i = 0; i< packetsToSend; i++){
                byte[] byteArray;
                if(bytesLeft<packetSize) {
                    byteArray = new byte[bytesLeft];


                }
                else {
                    byteArray = new byte[packetSize];

                }
                bis.read(byteArray, 0, byteArray.length);
                ByteBuffer buf = ByteBuffer.wrap(byteArray);
                //System.out.println("Sending Packet : "+ (i+1));
                if(bytesLeft<packetSize) {
               //     System.out.println("Packet size: " + bytesLeft);
                    bytesLeft -= bytesLeft;
                }
                else {
                 //   System.out.println("Packet size: " + packetSize);
                    bytesLeft -= packetSize;
                }
              //  System.out.println("Bytes left to send: "+ bytesLeft +"\n");
                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                sc.send(buf, messageInfo);


                buf.clear();
            }

            sc.close();
        }
    }

}
