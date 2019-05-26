import com.sun.nio.sctp.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import static com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent.COMM_UP;


public class SCTPMultiClient extends Thread {
    static int SERVER_PORT = 4477;
    static int FILES_TO_RECIEVE = 10;
    static String FILE_TO_RECIEVE = "fishRecieve";
    static String SERVER_ADDRESS = "169.254.235.209";


    public static void clientRun() throws IOException {
        File file = new File(FILE_TO_RECIEVE);
        Instant done;
        Duration timeForAllFiles;
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_ADDRESS,
                SERVER_PORT);

        Instant firstConnect = Instant.now();

        for (int i = 0; i < FILES_TO_RECIEVE; i++) {
            SctpChannel sc = SctpChannel.open();

            sc.connect(serverAddr, 0, 0);

            AssociationHandler assocHandler = new AssociationHandler();


            ByteBuffer buf = ByteBuffer.allocateDirect(8192);
            Instant starts = Instant.now();
            MessageInfo messageInfo = sc.receive(buf, System.out, assocHandler);

            while (messageInfo != null) {


                buf.flip();
                if (buf.remaining() > 0 && messageInfo.streamNumber() == 0) {

                    byte[] myBytes = new byte[buf.remaining()];
                    buf.get(myBytes, 0, myBytes.length);
                    try (FileOutputStream fos = new FileOutputStream(file, true)) {
                        fos.write(myBytes);
                    }
                }
                buf.clear();

                messageInfo = sc.receive(buf, System.out, assocHandler);


            }
            sc.close();
            Instant ends = Instant.now();
            Duration duration = Duration.between(starts, ends);
            System.out.println("\n\nFILE "+i);
            System.out.println("ReceiveTime: " + duration.toMillis());
            System.out.println("File lenght: " + file.length());

            float fish = (float) file.length() / duration.toMillis();
            file.delete();


        }

        done = Instant.now();
        timeForAllFiles = Duration.between(firstConnect, done);
        System.out.println("Time to get all files: "+ timeForAllFiles.toMillis()+ "ms");
    }

    static class AssociationHandler extends AbstractNotificationHandler<PrintStream> {
        public HandlerResult handleNotification(AssociationChangeNotification not,
                                                PrintStream stream) {

            if (not.event().equals(COMM_UP)) {
                int outbound = not.association().maxOutboundStreams();
                int inbound = not.association().maxInboundStreams();
              //   stream.printf("New association setup with %d outbound streams" + ", and %d inbound streams.\n", outbound, inbound);
            }

            return HandlerResult.CONTINUE;
        }

        public HandlerResult handleNotification(ShutdownNotification not,
                                                PrintStream stream) {
            // stream.printf("The association has been shutdown.\n");
            return HandlerResult.RETURN;
        }
    }
}
