import com.sun.nio.sctp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;


import static com.sun.nio.sctp.SctpStandardSocketOptions.*;

public class SCTPSingleServer extends Thread {
    static int SERVER_PORT = 4477;
    static int FILES_TO_SEND = 10;
    static String FILE_TO_SEND = "fish10";

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
        /*File[] files = new File[FILES_TO_SEND];
        long[] fileSize = new long[FILES_TO_SEND];
        int[] bytesLeft = new int[FILES_TO_SEND];
        float[] packetsToSend = new float[FILES_TO_SEND];
        double packetSizeDouble = 10240;
        int packetSize = 10240;

        for (int i = 0; i < FILES_TO_SEND; i++) {
            files[i] = new File( "FilesToSend/"+FILE_TO_SEND);
            fileSize[i] = files[i].length();
            bytesLeft[i] = (int) fileSize[i];
            packetsToSend[i] = (float) Math.ceil(fileSize[i] / packetSizeDouble);
        }*/
        File file = new File( "FilesToSend/"+FILE_TO_SEND);
        long fileSize = file.length();
        int bytesLeft = (int) fileSize;
        double packetSizeDouble = 10240;
        int packetSize = 10240;
        float packetsToSend = (float) Math.ceil(fileSize / packetSizeDouble);

        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);
        while (true){
            System.out.println("Waiting for connection");
            SctpChannel sc = ssc.accept();
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            bytesLeft = (int) fileSize;
            try {
                for (int j = 0; j < packetsToSend; j++) {
                    byte[] byteArray;
                    if (bytesLeft < packetSize) {
                        byteArray = new byte[bytesLeft];


                    } else {
                        byteArray = new byte[packetSize];

                    }
                    bis.read(byteArray, 0, byteArray.length);
                    ByteBuffer buf = ByteBuffer.wrap(byteArray);
                    if (bytesLeft < packetSize) {
                      //  System.out.println("Packet size: " + bytesLeft);
                        bytesLeft -= bytesLeft;
                    } else {
                      //  System.out.println("Packet size: " + packetSize);
                        bytesLeft -= packetSize;
                    }
                  //  System.out.println("Bytes left to send: " + bytesLeft + "\n");
                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                    sc.send(buf, messageInfo);


                    buf.clear();
                }

                sc.close();
                System.out.println("File sent");
            } catch (Exception e) {
                System.out.println("Connection closed");
            }
        }

    }

}
