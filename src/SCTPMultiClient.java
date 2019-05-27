import com.sun.nio.sctp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;

import static com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent.COMM_UP;
import static com.sun.nio.sctp.SctpStandardSocketOptions.*;


public class SCTPMultiClient {
    static int SERVER_PORT = 4477;
    // static int FILES_TO_RECIEVE = 2;
    // static int STREAMS_TO_OPEN = 3;
    static String FILE_TO_RECIEVE = "fishRecieve";
    static String SERVER_ADDRESS = "192.168.1.228";

    public static void clientRun(int FILES_TO_RECIEVE, int STREAMS_TO_OPEN) throws Exception {
        FILES_TO_RECIEVE= 10;
         STREAMS_TO_OPEN = 10;
        double FILESIZE_TO_RECIEVE = 100000.0;
        Duration durNoLoss = Duration.ZERO;
        Duration durWithLoss = Duration.ZERO;
        int filesCounted = 0;
        BufferedWriter resultOut = new BufferedWriter(new FileWriter("MultiClientTimes.txt"));
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_ADDRESS,
                SERVER_PORT);

        for (int j = 0; j < FILES_TO_RECIEVE; j++) {

            SctpChannel sc = SctpChannel.open();
            System.out.println(sc.getOption(SO_RCVBUF));
            sc.setOption(SO_RCVBUF, sc.getOption(SO_RCVBUF) * 2);
            sc.setOption(SO_SNDBUF, sc.getOption(SO_SNDBUF) * 2);


            sc.connect(serverAddr, 0, STREAMS_TO_OPEN);

            AssociationHandler assocHandler = new AssociationHandler();


            ByteBuffer buf = ByteBuffer.allocateDirect(100000);

            Instant starts = Instant.now();
            int filesRecieved = 0;
            int streamNumber = 0;
            MessageInfo messageInfo = sc.receive(buf, System.out, assocHandler);
            while (messageInfo != null) {

                buf.flip();
                if (buf.remaining() > 0) streamNumber = messageInfo.streamNumber();

                for (int i = 0; i < STREAMS_TO_OPEN; i++) {
                    if (buf.remaining() > 0 && streamNumber == i) {

                        byte[] myBytes = new byte[buf.remaining()];
                        buf.get(myBytes, 0, myBytes.length);

                        try (FileOutputStream fos = new FileOutputStream(FILE_TO_RECIEVE + "." + i, true)) {
                            fos.write(myBytes);

                        }
                        break;
                    }
                }
                buf.clear();

                Instant inst = Instant.now();
               // sc.send(ByteBuffer.wrap(new byte[2]), MessageInfo.createOutgoing(null, 0));
                messageInfo = sc.receive(buf, System.out, assocHandler);
                Instant last = Instant.now();
                System.out.println(Duration.between(inst, last));
            }


            Instant ends = Instant.now();

            sc.close();
            Duration duration = Duration.between(starts, ends);
            durWithLoss = durWithLoss.plus(duration);
            if (!(duration.toNanos() > 10.0 * durNoLoss.toNanos() / filesCounted)) {
                if (j != 0) {
                    durNoLoss = durNoLoss.plus(duration);
                    filesCounted++;
                }
            }
            long test = duration.toMillis();
            resultOut.write(test + "\n");

            System.out.println(j + " ReceiveTime: " + duration.toMillis() + "ms\n");

            for (int k = 0; k < STREAMS_TO_OPEN; k++) {
                File deleteFile = new File(FILE_TO_RECIEVE + "." + k);
                deleteFile.delete();
            }

        }
        resultOut.close();
        System.out.println("Files counted: " + filesCounted);
        double totalFileSizeMB = (FILESIZE_TO_RECIEVE * FILES_TO_RECIEVE) / 1000000.0;
        double totalDurationSec = durNoLoss.toNanos() / 1000000000.0;
        double megaBytePerSec = totalFileSizeMB / totalDurationSec;

        totalDurationSec = durWithLoss.toNanos() / 1000000000.0;
        double megaBytePerSecLoss = totalFileSizeMB / totalDurationSec;

        System.out.println("Total file size: " + totalFileSizeMB + " MB");

        System.out.println(durNoLoss.toMillis() + "ms to recieve all files no loss");
        System.out.println(durWithLoss.toMillis() + "ms to recieve all files with loss");

        System.out.println(megaBytePerSec + " MB/sec no loss");
        System.out.println(megaBytePerSecLoss + " MB/sec with loss");


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
