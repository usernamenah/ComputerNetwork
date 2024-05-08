import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class serversarq {

    ServerSocket server;
    Socket socket;
    BufferedReader br;
    PrintWriter out;

    public serversarq() {
        try {
            server = new ServerSocket(7779);
            System.out.println("server is waiting for connection");
            System.out.println("waiting to connect.......!!");
            socket = server.accept();

            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            startReading();
            startWriting();
        } catch (Exception e) {
            
        }
    }

    private void handleReceivedMessage(String[] frch) {
        try {
            if (frch[0].equals("0")) {
                int framecount = Integer.parseInt(frch[1]);
                System.out.println("FRAME :" + frch[0] + " received ::" + (framecount - 1));
            } else {
                System.out.println(frch[0] + " FRAME received as :" + frch[1]);
                out.println(frch[0] + "  frame ack from server :");
                System.out.println(frch[0] + " ACK IS SENT  ");
                out.flush();
            }
        } catch (Exception e) {
            
        }
    }

    public void startReading() {
        Runnable r1 = () -> {
            System.out.println("reader started");
            try {
                while (true) {
                    String message = br.readLine();

                    if (message.equals("--")) {
                        System.out.println("child terminated ....!!");
                        socket.close();
                        break;
                    }

                    if (message.contains("`")) {
                        String frch[] = message.split("`");
                        handleReceivedMessage(frch);
                    } else {
                        
                            System.out.println("client :" + message);
                        
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        new Thread(r1).start();
    }

    public void startWriting() {
        Runnable r2 = () -> {
            System.out.println("writer started");
            try {
                while (!socket.isClosed()) {
                    BufferedReader br1 = new BufferedReader(new InputStreamReader(System.in));
                    String content = br1.readLine();

                    if (content.startsWith("arq")) {
                        sendArqContent(content);
                    } else {
                        out.println(content);
                        out.flush();
                    }
                }
            } catch (Exception e) {
                
            }
        };
        new Thread(r2).start();
    }

    private void sendArqContent(String content) {
        try {
            String[] ml = content.split(" ");
            int id = 0;
            for (int zz = 1; zz < ml.length; zz++) {
                String idd = "" + id + "";
                out.println(idd + "," + ml[zz]);
                Thread.sleep(1000);
                id++;
            }
        } catch (Exception e) {
           
        }
    }

    public static void main(String[] args) {
        new serversarq();
    }
}
