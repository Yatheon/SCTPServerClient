public class startMultiSCTPClient {
    public static void main(String[] args) {

               try {
                   SCTPMultiClient.clientRun(Integer.parseInt(args[0]));
               }catch (Exception e){
                   e.printStackTrace();
               }

    }
}
