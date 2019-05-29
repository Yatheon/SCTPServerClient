import com.sun.nio.sctp.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedByInterruptException;
import java.time.Duration;
import java.time.Instant;

import static com.sun.nio.sctp.SctpStandardSocketOptions.*;
import static com.sun.nio.sctp.AssociationChangeNotification.AssocChangeEvent.COMM_UP;

public class SCTPMultiServer {
    private SctpChannel sc;
    private File myFile;
    static int SERVER_PORT = 4477;
    static String PATH_TO_FILES = "FilesToSend/";

    static int PACKET_SIZE = 20000;
    static double packetSizeDouble = 20000.0;

    public static void serverRun(String FILE_TO_SEND) throws IOException {
		File file = new File(PATH_TO_FILES + FILE_TO_SEND);
		long fileSize = file.length();
		SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);
        ssc.setOption(SCTP_INIT_MAXSTREAMS, SctpStandardSocketOptions.InitMaxStreams.create(0, 100));
		int counter = 0;



        while (true) {
            System.out.println("Waiting for connection");
            SctpChannel sc = ssc.accept();
			Instant done;
			Instant now = Instant.now();
			AssociationHandler assocHandler = new AssociationHandler();
			MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);

		   int FILES_TO_SEND = sc.association().maxOutboundStreams();
			
			System.out.println("Streams: "+FILES_TO_SEND);
            System.out.println(counter++);
			
			int[] bytesLeft = new int[FILES_TO_SEND];
            int[] bytesSent = new int[FILES_TO_SEND];
            int[] packetsToSend = new int[FILES_TO_SEND];
			byte[] byteArray = new byte[(int) fileSize];
			
			
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            bufferedInputStream.read(byteArray, 0, byteArray.length);
         
		 for (int i = 0; i < FILES_TO_SEND; i++) {
                bytesLeft[i] = (int) fileSize;
				float temp = (float) Math.ceil(fileSize/ packetSizeDouble);
				packetsToSend[i]  = (int)temp;
            }
            ByteBuffer buf = ByteBuffer.allocateDirect(PACKET_SIZE);

	
            try {
         

                for (int i = 0; i < FILES_TO_SEND; i++) {
				
                    for (int j = 0; j < packetsToSend[i]; j++) {
						buf.clear();
                        if (bytesLeft[i] <= PACKET_SIZE) {
							buf = ByteBuffer.wrap(byteArray, bytesSent[i], bytesLeft[i]);
                            messageInfo.streamNumber(i);
                            sc.send(buf, messageInfo);
	
							break;
                        } else {
							buf = ByteBuffer.wrap(byteArray, bytesSent[i], PACKET_SIZE);
							messageInfo.streamNumber(i);
							sc.send(buf, messageInfo);
                            bytesSent[i] += PACKET_SIZE;
                            bytesLeft[i] -= PACKET_SIZE;
		
                        }
                        buf.clear();
                    }
                }

				sc.shutdown();
				sc.close();
				done = Instant.now();
				System.out.println(Duration.between(now, done));
				//sc.close();

                System.out.println("Files sent");
             /*  for (int i = 0; i < FILES_TO_SEND; i++){
                   files[i].delete();
                }*/

            } catch (ClosedChannelException cce) {
                cce.printStackTrace();
                System.out.println("Connection closed");
                break;
            } catch (Exception e) {
                e.printStackTrace();
				sc.close();
                System.out.println("Connection closed");
            }

        }

    }

    public static void deleteFiles(File f, int nrOfFiles){

    }
    public static void splitFile(File f, int nrOfFiles) throws IOException {
        int partCounter = 0;
        double fish = Math.ceil((double) f.length() / (double) nrOfFiles);
        int sizeOfFiles = (int) fish;
        byte[] buffer = new byte[sizeOfFiles];

        String fileName = f.getName();

        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(f);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                //write each chunk of data into separate file with different number in name
                String filePartName = String.format("%s.%d", fileName, partCounter++);
                File newFile = new File(f.getParent(), filePartName);
                try (FileOutputStream out = new FileOutputStream(newFile)) {
                    out.write(buffer, 0, bytesAmount);
                }
            }
        }
    }
	static class AssociationHandler extends AbstractNotificationHandler<PrintStream> {
        public HandlerResult handleNotification(AssociationChangeNotification not,
                                                PrintStream stream) {

            if (not.event().equals(COMM_UP)) {
                int outbound = not.association().maxOutboundStreams();
                int inbound = not.association().maxInboundStreams();
                //stream.printf("New association setup with %d outbound streams" + ", and %d inbound streams.\n", outbound, inbound);
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
