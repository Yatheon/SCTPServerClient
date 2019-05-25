import com.sun.nio.sctp.SctpServerChannel;

import java.net.InetSocketAddress;

public class startSingleSCTPServer {


    public static void main(String[] args) throws Exception {
        new Thread(new SCTPSingleServer()).start();

    }
}
