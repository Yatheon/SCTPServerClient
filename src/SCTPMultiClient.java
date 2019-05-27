import com.sun.nio.sctp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import static com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent.COMM_UP;
import static com.sun.nio.sctp.SctpStandardSocketOptions.*;


public class SCTPMultiClient {
    static int SERVER_PORT = 4477;
    // static int FILES_TO_RECIEVE = 2;
    // static int STREAMS_TO_OPEN = 3;
    static String FILE_TO_RECIEVE = "fishRecieve";
    static String SERVER_ADDRESS = "169.254.10.219";


    public static void clientRun(int FILES_TO_RECIEVE, int STREAMS_TO_OPEN) throws Exception {
        Duration durNoLoss = Duration.ZERO;
		Duration durWithLoss = Duration.ZERO;
        int filesCounted = 0;
        BufferedWriter resultOut = new BufferedWriter(new FileWriter("MultiClientTimes.txt"));
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_ADDRESS,
                SERVER_PORT);

        for (int j = 0; j < FILES_TO_RECIEVE; j++) {

            SctpChannel sc = SctpChannel.open();
			sc.setOption(SO_RCVBUF, sc.getOption(SO_RCVBUF)*2 );
	
			sc.connect(serverAddr, 0, STREAMS_TO_OPEN);

            AssociationHandler assocHandler = new AssociationHandler();

            ByteBuffer buf = ByteBuffer.allocateDirect(16384);

        
            Instant starts = Instant.now();
			int filesRecieved = 0;
				
				MessageInfo messageInfo = sc.receive(buf, System.out, assocHandler);
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
			durWithLoss = durWithLoss.plus(duration);
            if(!(duration.toNanos()>10.0*durNoLoss.toNanos()/filesCounted)) {
				if(j!=0){
                durNoLoss = durNoLoss.plus(duration);
                filesCounted++;
				}
            }
            long test = duration.toMillis();
            resultOut.write(test + "\n");

            System.out.println(j +" ReceiveTime: " + duration.toMillis() + "ms\n");

            for (int k = 0; k < STREAMS_TO_OPEN; k++) {
                File deleteFile = new File(FILE_TO_RECIEVE + "." + k);
                deleteFile.delete();
            }

        }
        resultOut.close();
		System.out.println("Files counted: "+filesCounted);
        double totalFileSizeMB = (100000.0 * FILES_TO_RECIEVE)/1000000.0;
        double totalDurationSec = durNoLoss.toNanos()/1000000000.0;
        double megaBytePerSec = totalFileSizeMB / totalDurationSec;
		
        totalDurationSec = durWithLoss.toNanos()/1000000000.0;
        double megaBytePerSecLoss = totalFileSizeMB / totalDurationSec;
		
		System.out.println("Total file size: "+totalFileSizeMB+" MB");
		
		System.out.println(durNoLoss.toMillis()+"ms to recieve all files no loss");
		System.out.println(durWithLoss.toMillis()+"ms to recieve all files with loss");
		
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
