import com.sun.nio.sctp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static com.sun.nio.sctp.SctpStandardSocketOptions.*;

public class SCTPSingleServer {
    static int SERVER_PORT = 4477;
    static String FILE_TO_SEND = "fish1";


    public static void serverRun() throws IOException {

        File file = new File("FilesToSend/"+FILE_TO_SEND);
        long fileSize = file.length();
        int bytesLeft;
        int bytesSent = 0;
        double packetSizeDouble = 10000;
        int packetSize = 10000;
        float packetsToSend = (float) Math.ceil(fileSize / packetSizeDouble);
        ByteBuffer buf;


        byte[] byteArray = new byte[(int) fileSize];
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new 			 			FileInputStream(file));
            bufferedInputStream.read(byteArray, 0, byteArray.length);


        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);
        while (true) {
            System.out.println("Waiting for connection");
            SctpChannel sc = ssc.accept();
    
           	bytesLeft = (int) fileSize;
		bytesSent = 0;

            try {
                for (int j = 0; j < packetsToSend; j++) {
                    if (bytesLeft < packetSize) {
                        buf = ByteBuffer.wrap(byteArray, bytesSent, bytesLeft);
		
                        bytesSent += bytesLeft;
                        bytesLeft -= bytesLeft;
                    } else {
                        buf = ByteBuffer.wrap(byteArray, bytesSent, packetSize);
			
                        bytesSent += packetSize;
                        bytesLeft -= packetSize;
                    }

                    MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                    sc.send(buf, messageInfo);


                    buf.clear();
                }

                sc.close();
                System.out.println("File sent");
            } catch (Exception e) {
		e.printStackTrace();
                System.out.println("Connection closed");
            }
        }

    }

}
