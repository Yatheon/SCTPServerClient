import com.sun.nio.sctp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.nio.channels.FileChannel;

import static com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent.COMM_UP;
import static com.sun.nio.sctp.SctpStandardSocketOptions.*;


public class SCTPMultiClient {
    static int SERVER_PORT = 4477;
    static String FILE_TO_RECIEVE = "fishRecieve";
    static String SERVER_ADDRESS = "169.254.10.219";


    public static void clientRun(int FILES_TO_RECIEVE, int STREAMS_TO_OPEN) throws Exception {
		double FILESIZE_TO_RECIEVE = 100000.0;
         ByteBuffer buf = ByteBuffer.allocate(1000);
		Duration onlyRecieve = Duration.ZERO;
    
		long[] dataRemaining = new long[STREAMS_TO_OPEN];
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_ADDRESS,
                SERVER_PORT);

            SctpChannel sc = SctpChannel.open();
			sc.setOption(SCTP_FRAGMENT_INTERLEAVE, 2);
			sc.connect(serverAddr, 0, STREAMS_TO_OPEN);

			AssociationHandler assocHandler = new AssociationHandler();
			
			Instant inst = Instant.now();
			
			Duration getTime = Duration.ZERO;
			MessageInfo messageInfo = sc.receive(buf, null, assocHandler);
			ByteBuffer cont = ByteBuffer.allocateDirect(9000);
	
			buf.flip();
			for (int i = 0; i < STREAMS_TO_OPEN; i++) {
						if (buf.remaining() > 0 && messageInfo.streamNumber() == i) {
							try (FileChannel fos = new FileOutputStream(FILE_TO_RECIEVE + "." + i, true).getChannel()) {
								fos.write(buf);
								fos.close();
							}
							break;
						}
			}
			
			for (int j = 0; j < FILES_TO_RECIEVE; j++) {
			
			if(j != 0){
			sc = SctpChannel.open();
            System.out.println();
			inst = Instant.now();
			sc.connect(serverAddr, 0, STREAMS_TO_OPEN);
			assocHandler = new AssociationHandler();
			getTime = Duration.ZERO;
			}
			

			int round = 0;
			do{
				messageInfo = sc.receive(cont, null, assocHandler);
                cont.flip();
				if(cont.remaining()>0){
					for (int i = 0; i < STREAMS_TO_OPEN; i++) {
						if (messageInfo.streamNumber() == i) {
						  try (FileChannel fos = new FileOutputStream(FILE_TO_RECIEVE + "." + i, true).getChannel()) {
							  
								fos.write(cont);
								fos.close();
							}
							break;
						}
					}
				}
                cont.clear();
            } while (messageInfo != null) ;
            sc.close();
			Instant last = Instant.now();
			getTime = Duration.between(inst,last);
			onlyRecieve = onlyRecieve.plus(getTime);		
			
            long test = getTime.toMillis();
  
			System.out.println("Receive time: "+getTime.toMillis());

		if(j == 0){
			FILESIZE_TO_RECIEVE = new File(FILE_TO_RECIEVE + "." + 0).length();
		}
            for (int k = 0; k < STREAMS_TO_OPEN; k++) {
                File deleteFile = new File(FILE_TO_RECIEVE + "." + k);
               deleteFile.delete();
            }

        }

 
        double totalFileSizeMB = (FILESIZE_TO_RECIEVE * FILES_TO_RECIEVE*STREAMS_TO_OPEN) / 1000000.0;
        double totalDurationSec = onlyRecieve.toNanos() / 1000000000.0;
        double megaBytePerSec = totalFileSizeMB / totalDurationSec;
		
        System.out.println("Total file size: " + totalFileSizeMB + " MB");
        System.out.println(onlyRecieve.toMillis() + "ms");
		System.out.println(megaBytePerSec + " MB/sec");


    }

    static class AssociationHandler extends AbstractNotificationHandler<PrintStream> {
        public HandlerResult handleNotification(AssociationChangeNotification not,
                                                PrintStream stream) {

            if (not.event().equals(COMM_UP)) {
                int outbound = not.association().maxOutboundStreams();
                int inbound = not.association().maxInboundStreams();
                  // stream.printf("New association setup with %d outbound streams" + ", and %d inbound streams.\n", outbound, inbound);
            }

            return HandlerResult.CONTINUE;
        }

        public HandlerResult handleNotification(ShutdownNotification not,
                                                PrintStream stream) {
        //     stream.printf("The association has been shutdown.\n");
            return HandlerResult.RETURN;
        }
    }
}
