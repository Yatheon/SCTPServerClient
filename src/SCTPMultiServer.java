import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import com.sun.nio.sctp.SctpStandardSocketOptions;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.time.Instant;

import static com.sun.nio.sctp.SctpStandardSocketOptions.SCTP_INIT_MAXSTREAMS;
import static com.sun.nio.sctp.SctpStandardSocketOptions.SO_SNDBUF;


public class SCTPMultiServer {
    private SctpChannel sc;
    private File myFile;
    static int SERVER_PORT = 4477;
    static String PATH_TO_FILES = "FilesToSend/";
    static String FILE_TO_SEND = "fish100";
    static int PACKET_SIZE = 10000;


    public static void serverRun() throws IOException {

        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);
        ssc.setOption(SCTP_INIT_MAXSTREAMS, SctpStandardSocketOptions.InitMaxStreams.create(0, 100));

        while (true) {
            System.out.println("Waiting for connection");
            SctpChannel sc = ssc.accept();
            int FILES_TO_SEND = sc.association().maxOutboundStreams();
            System.out.println("Out streams: " + sc.association().maxOutboundStreams());
            splitFile(new File(PATH_TO_FILES + FILE_TO_SEND), FILES_TO_SEND);
            File[] files = new File[FILES_TO_SEND];
            long[] fileSize = new long[FILES_TO_SEND];
            int[] bytesLeft = new int[FILES_TO_SEND];
            int[] bytesSent = new int[FILES_TO_SEND];
            float[] packetsToSend = new float[FILES_TO_SEND];
            byte[][] byteArray = new byte[FILES_TO_SEND][];
            double packetSizeDouble = 10240;


            for (int i = 0; i < FILES_TO_SEND; i++) {
                files[i] = new File(PATH_TO_FILES + FILE_TO_SEND + "." + i);
                fileSize[i] = files[i].length();
                byteArray[i] = new byte[(int) fileSize[i]];
                BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(files[i]));
                bufferedInputStream.read(byteArray[i], 0, byteArray[i].length);
                bytesLeft[i] = (int) fileSize[i];
                packetsToSend[i] = (float) Math.ceil(fileSize[i] / packetSizeDouble);

            }

            ByteBuffer buf;

            try {
                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                for (int i = 0; i < FILES_TO_SEND; i++) {
                    Instant start = Instant.now();
                    bytesLeft[i] = (int) fileSize[i];
                    // bytesSent[i] = 0;

                    for (int j = 0; j < packetsToSend[i]; j++) {

                        if (bytesLeft[i] < PACKET_SIZE) {
                            buf = ByteBuffer.wrap(byteArray[i], bytesSent[i], bytesLeft[i]);

                            bytesSent[i] += bytesLeft[i];
                            bytesLeft[i] -= bytesLeft[i];
                        } else {
                            buf = ByteBuffer.wrap(byteArray[i], bytesSent[i], PACKET_SIZE);

                            bytesSent[i] += PACKET_SIZE;
                            bytesLeft[i] -= PACKET_SIZE;
                        }


                        messageInfo.streamNumber(i);

                        sc.send(buf, messageInfo);


                        buf.clear();
                    }
                    Instant end = Instant.now();
                    System.out.println("Time to process: "+Duration.between(start,end));
                }
                sc.close();
                System.out.println("Files sent");
                for (int i = 0; i < FILES_TO_SEND; i++){
                   files[i].delete();
                }

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
