import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

import java.net.InetSocketAddress;

public class start {
    static int SERVER_PORT = 4477;

    public static void main(String[] args) throws Exception {



        SctpServerChannel ssc = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(SERVER_PORT);
        ssc.bind(serverAddr);
        while (true) {
            Runnable SCTPSingleServer = new SCTPSingleServer(args, ssc.accept());
            SCTPSingleServer.run();
        }

    }
}
