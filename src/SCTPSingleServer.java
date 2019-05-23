import com.sun.nio.sctp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

//import static java.net.SocketOptions.SO_RCVBUF;

import static com.sun.nio.sctp.SctpStandardSocketOptions.*;
public class SCTPSingleServer implements Runnable {
    static int SERVER_PORT = 4477;
    private File myFile;

    public SCTPSingleServer(String[] args){
        this.myFile = new File(args[0]);
    }
    @Override
    public void run() {
        try {
            serverRun();
        } catch (Exception e) {
            System.out.println("Something went wrong with Server" + e);
            e.printStackTrace();
        }
    }

    public void serverRun() throws IOException {

        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);

        double packetSizeDouble = 10240;
        int packetSize = 10240;
        long fileSize = myFile.length();
        int bytesLeft;


        long fileLength = myFile.length();
        float packetsToSend = (float) Math.ceil(fileLength / packetSizeDouble);
        bytesLeft = (int)fileSize;
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));

        while (true) {
            //File myFile = new File("FileToSend.tar.gz");
            bytesLeft = (int)fileSize;
            try {


                SctpChannel sc = ssc.accept();
                for (int i = 0; i < packetsToSend; i++) {
                    byte[] byteArray;
                    if (bytesLeft < packetSize) {
                        byteArray = new byte[bytesLeft];


                    } else {
                        byteArray = new byte[packetSize];

                    }
                    bis.read(byteArray, 0, byteArray.length);
                    ByteBuffer buf = ByteBuffer.wrap(byteArray);
                    //System.out.println("Sending Packet : "+ (i+1));
                    if (bytesLeft < packetSize) {
                        //     System.out.println("Packet size: " + bytesLeft);
                        bytesLeft -= bytesLeft;
                    } else {
                        //   System.out.println("Packet size: " + packetSize);
                        bytesLeft -= packetSize;
                    }
                    //  System.out.println("Bytes left to send: "+ bytesLeft +"\n");
                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                    sc.send(buf, messageInfo);


                    buf.clear();
                }

                sc.close();
            }catch (Exception e){
                System.out.println("Connection closed");
            }
        }
    }

}
