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
import static com.sun.nio.sctp.SctpStandardSocketOptions.*;


public class SCTPSingleClient extends Thread {
    static int SERVER_PORT = 4477;

    @Override
    public void run() {

        try {
            clientRun();
        } catch (Exception e) {
            System.out.println("Something went wrong with client");
            e.printStackTrace();
        }
    }

    public static void clientRun() throws IOException {
        File file = new File("FileToReceive.tar.gz");

        InetSocketAddress serverAddr = new InetSocketAddress("localhost",
                SERVER_PORT);
        ByteBuffer buf = ByteBuffer.allocateDirect(212992);
        SctpChannel sc = SctpChannel.open(serverAddr, 0, 0);

        AssociationHandler assocHandler = new AssociationHandler();

        System.out.println(sc.getOption(SO_RCVBUF));
        sc.setOption(SO_RCVBUF, 212992);
        System.out.println(sc.getOption(SO_RCVBUF));
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
        Instant ends = Instant.now();
        Duration duration = Duration.between(starts, ends);
        System.out.println("ReceiveTime: " + duration.toMillis());
        sc.close();
    }

    static class AssociationHandler extends AbstractNotificationHandler<PrintStream> {
        public HandlerResult handleNotification(AssociationChangeNotification not,
                                                PrintStream stream) {

            if (not.event().equals(COMM_UP)) {
                int outbound = not.association().maxOutboundStreams();
                int inbound = not.association().maxInboundStreams();
                //  stream.printf("New association setup with %d outbound streams" + ", and %d inbound streams.\n", outbound, inbound);
            }

            return HandlerResult.CONTINUE;
        }

        public HandlerResult handleNotification(ShutdownNotification not,
                                                PrintStream stream) {
            //  stream.printf("The association has been shutdown.\n");
            return HandlerResult.RETURN;
        }
    }
}