import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.sun.nio.sctp.SctpStandardSocketOptions.SCTP_DISABLE_FRAGMENTS;

//import static java.net.SocketOptions.SO_RCVBUF;

public class SCTPMultiStreamServer implements Runnable {
    private SctpChannel sc;
    private File myFile;

    public SCTPMultiStreamServer(String[] args, SctpChannel sc) {
        this.myFile = new File(args[0]);
        this.sc = sc;

       try {
           sc.setOption(SCTP_DISABLE_FRAGMENTS, true);
       }catch (Exception e){
       }
    }

    @Override
    public void run() {
        try {
            serverRun();
        } catch (Exception e) {
            System.out.println("Something went wrong with Server" + e);
            e.printStackTrace();
        }
    }

    public void serverRun() throws IOException {


        double packetSizeDouble = 10240;
        int packetSize = 10240;
        long fileSize = myFile.length();
        int bytesLeft;


        long fileLength = myFile.length();
        float packetsToSend = (float) Math.ceil(fileLength / packetSizeDouble);
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));


        bytesLeft = (int) fileSize;
        try {


            for (int i = 0; i < packetsToSend; i++) {
                byte[] byteArray;
                if (bytesLeft < packetSize) {
                    byteArray = new byte[bytesLeft];


                } else {
                    byteArray = new byte[packetSize];

                }
                bis.read(byteArray, 0, byteArray.length);
                ByteBuffer buf = ByteBuffer.wrap(byteArray);
                System.out.println("Sending Packet : "+ (i+1));
                if (bytesLeft < packetSize) {
                         System.out.println("Packet size: " + bytesLeft);
                    bytesLeft -= bytesLeft;
                } else {
                       System.out.println("Packet size: " + packetSize);
                    bytesLeft -= packetSize;
                }
                  System.out.println("Bytes left to send: "+ bytesLeft +"\n");
                MessageInfo messageInfo = MessageInfo.createOutgoing(null, 0);
                sc.send(buf, messageInfo);


                buf.clear();
            }

            sc.close();
        } catch (Exception e) {
            System.out.println("Connection closed");
        }

    }

}
