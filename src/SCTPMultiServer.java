import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.SctpStandardSocketOptions;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedByInterruptException;
import java.time.Duration;
import java.time.Instant;

import static com.sun.nio.sctp.SctpStandardSocketOptions.SCTP_INIT_MAXSTREAMS;
import static com.sun.nio.sctp.SctpStandardSocketOptions.*;


public class SCTPMultiServer {
    private SctpChannel sc;
    private File myFile;
    static int SERVER_PORT = 4477;
    static String PATH_TO_FILES = "FilesToSend/";
    //static String FILE_TO_SEND = "fishy10";
    static int PACKET_SIZE = 8000;
    static double packetSizeDouble = 8000.0;

    public static void serverRun(String FILE_TO_SEND) throws IOException {

        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);
        ssc.setOption(SCTP_INIT_MAXSTREAMS, SctpStandardSocketOptions.InitMaxStreams.create(0, 100));
int counter = 0;

        while (true) {
            System.out.println("Waiting for connection");
            SctpChannel sc = ssc.accept();
			System.out.println("Connecton accepted! " + sc.getOption(SO_RCVBUF) +" : " +sc.getOption(SO_SNDBUF) );
            int FILES_TO_SEND = sc.association().maxOutboundStreams();
            System.out.println(counter++);
			
           int[] bytesLeft = new int[FILES_TO_SEND];
            int[] bytesSent = new int[FILES_TO_SEND];
            float[] packetsToSend = new float[FILES_TO_SEND];
		
			File file = new File(PATH_TO_FILES + FILE_TO_SEND);
			long fileSize = file.length();
			byte[] byteArray = new byte[(int) fileSize];
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            bufferedInputStream.read(byteArray, 0, byteArray.length);
         
		 for (int i = 0; i < FILES_TO_SEND; i++) {
                bytesLeft[i] = (int) fileSize;
				packetsToSend[i] = (float) Math.ceil(fileSize/ packetSizeDouble);
            }
            ByteBuffer buf = ByteBuffer.allocateDirect(PACKET_SIZE);
			Instant done;
			Instant now = Instant.now();
			System.out.println(FILES_TO_SEND);
            try {
                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);

                for (int i = 0; i < FILES_TO_SEND; i++) {
                    for (int j = 0; j < packetsToSend[i]; j++) {
                        if (bytesLeft[i] < PACKET_SIZE) {
                           ByteBuffer otherTest = ByteBuffer.wrap(byteArray, bytesSent[i], bytesLeft[i]);
                            messageInfo.streamNumber(i);
							messageInfo.complete(true);
                            sc.send(otherTest, messageInfo);
							break;
                        } else {
                            messageInfo.streamNumber(i);
                            ByteBuffer testingBuff = buf.get(byteArray, bytesSent[i], PACKET_SIZE).flip();
                            sc.send(testingBuff, messageInfo);
                            bytesSent[i] += PACKET_SIZE;
                            bytesLeft[i] -= PACKET_SIZE;
                        }
                        buf.clear();
                    }
                }
				done = Instant.now();
				System.out.println(Duration.between(now, done));
				sc.close();

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


}
