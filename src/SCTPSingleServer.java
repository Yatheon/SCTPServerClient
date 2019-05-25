import com.sun.nio.sctp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import static com.sun.nio.sctp.SctpStandardSocketOptions.*;

public class SCTPSingleServer {
    static int SERVER_PORT = 4477;
    static int FILES_TO_SEND = 10;
    static String FILE_TO_SEND = "fishy10";


    public static void serverRun() throws IOException {

        File file = new File(FILE_TO_SEND);
        long fileSize = file.length();
        int bytesLeft;
        int bytesSent = 0;
        double packetSizeDouble = 61582;
        int packetSize = 61582;
        float packetsToSend = (float) Math.ceil(fileSize / packetSizeDouble);
        ByteBuffer buf;


        byte[] byteArray = new byte[(int) fileSize];

        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);
        while (true) {
            System.out.println("Waiting for connection");
            SctpChannel sc = ssc.accept();
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            bis.read(byteArray, 0, byteArray.length);
            bytesLeft = (int) fileSize;

            try {
                for (int j = 0; j < packetsToSend; j++) {
                    if (bytesLeft < packetSize) {
                        buf = ByteBuffer.wrap(byteArray, bytesSent, bytesSent + bytesLeft);
                        bytesSent += bytesLeft;
                        bytesLeft -= bytesLeft;
                    } else {
                        buf = ByteBuffer.wrap(byteArray, bytesSent, bytesSent + packetSize);
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
                System.out.println("Connection closed");
            }
        }

    }

}
