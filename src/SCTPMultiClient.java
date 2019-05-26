import com.sun.nio.sctp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import static com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent.COMM_UP;
import static com.sun.nio.sctp.SctpStandardSocketOptions.SCTP_INIT_MAXSTREAMS;


public class SCTPMultiClient {
    static int SERVER_PORT = 4477;
    // static int FILES_TO_RECIEVE = 2;
    // static int STREAMS_TO_OPEN = 3;
    static String FILE_TO_RECIEVE = "fishRecieve";
    static String SERVER_ADDRESS = "localhost";


    public static void clientRun(int FILES_TO_RECIEVE, int STREAMS_TO_OPEN) throws IOException {
        Duration durAccTest = Duration.ZERO;
        int filesCounted = 0;
        BufferedWriter out = new BufferedWriter(new FileWriter("MultiClientTimes.txt"));
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_ADDRESS,
                SERVER_PORT);

        for (int j = 0; j < FILES_TO_RECIEVE; j++) {
            SctpChannel sc = SctpChannel.open();

            sc.connect(serverAddr, 0, STREAMS_TO_OPEN);

            AssociationHandler assocHandler = new AssociationHandler();

            ByteBuffer buf = ByteBuffer.allocateDirect(8192);

            MessageInfo messageInfo = sc.receive(buf, System.out, assocHandler);
            Instant starts = Instant.now();
            while (messageInfo != null) {
                buf.flip();
                for (int i = 0; i < STREAMS_TO_OPEN; i++) {
                    if (buf.remaining() > 0 && messageInfo.streamNumber() == i) {

                        byte[] myBytes = new byte[buf.remaining()];
                        buf.get(myBytes, 0, myBytes.length);
                        try (FileOutputStream fos = new FileOutputStream(FILE_TO_RECIEVE + "." + i, false)) {
                            fos.write(myBytes);
                        }
                        break;
                    }
                }
                buf.clear();

                messageInfo = sc.receive(buf, System.out, assocHandler);
            }
            Instant ends = Instant.now();
            sc.close();
            Duration duration = Duration.between(starts, ends);
            if(!(duration.toNanos()>1.5*durAccTest.toNanos()/filesCounted)) {
                durAccTest = durAccTest.plus(duration);
                filesCounted++;
            }
            long test = duration.toMillis();
            out.write(test + "\n");

            System.out.println("ReceiveTime: " + duration.toMillis() + "ms\n");

            for (int k = 0; k < STREAMS_TO_OPEN; k++) {
                File deleteFile = new File(FILE_TO_RECIEVE + "." + k);
                deleteFile.delete();
            }

        }
        out.close();
        double fish = 100000 * FILES_TO_RECIEVE;
        double nano = durAccTest.toNanos();
        double nanoPerByte = fish / nano;
        nanoPerByte = nanoPerByte * 10000;
        System.out.println(nanoPerByte + " MB/sec");

        System.out.println(durAccTest.toMillis()+"ms to recieve all files");
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
