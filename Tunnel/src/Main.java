public class Main {
    public static void main(String[] args) {
        TunnelClient client = new TunnelClient(args[0], Integer.parseInt(args[1]));
        client.start();

        try {
            client.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}