import java.io.*;
import java.net.*;
import java.util.*;
public class clientsarq{
    Socket socket;
    int i = 0;
    BufferedReader br;
    PrintWriter out;
    Timer[] timers;
    boolean[] ackReceived;
    public clientsarq() {
        try {
            System.out.println("sending request to server...!");
            socket = new Socket("127.0.0.1", 7779);
            System.out.println("connection done.....!");
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            startReading();
            startWriting();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void startReading() {
        Runnable r1 = () -> {
            System.out.println("reader started");
            try {
                while (true) {
                    String message = br.readLine();
                    if (message.equals("--")) {
                        System.out.println("main terminated ....!!");
                        socket.close();
                        break;
                    }

                    System.out.println("server: " + message);

                    if (message.startsWith("resend-acks:")) {
                        handleResendAcks(message);
                    } else if (message.contains("frame ack from server :")) {
                        updateAckReceived(message);
                    }
                }
            } catch (Exception e) {
               
            }
        };
        new Thread(r1).start();
    }

    private void handleResendAcks(String resendAcksMessage) {
        String[] parts = resendAcksMessage.split(":");
        if (parts.length == 2) {
            String[] frameNumbers = parts[1].split(",");
            for (String frameNumberStr : frameNumbers) {
                try {
                    int frameNumber = Integer.parseInt(frameNumberStr.trim());
                    if (frameNumber >= 0 && frameNumber < ackReceived.length && !ackReceived[frameNumber]) {
                        String idd = "" + frameNumber + "";
                        out.println(idd + "`" + "RESEND_FRAME");
                        ackReceived[frameNumber] = false;
                    }
                } catch (NumberFormatException e) {
                    
                }
            }
        }
    }

    private void updateAckReceived(String ackMessage) {
        String[] parts = ackMessage.split(":");
        if (parts.length == 2) {
            try {
                int frameNumber = Integer.parseInt(parts[1].trim());
                if (frameNumber >= 0 && frameNumber < ackReceived.length) {
                    ackReceived[frameNumber] = true;
                    System.out.println("ACK received for FRAME: " + frameNumber);
                }
            } catch (NumberFormatException e) {
                
            }
        }
    }

    public void startWriting() {
        Runnable r2 = () -> {
            System.out.println("writer started");

            try {
                while (!socket.isClosed()) {
                    BufferedReader br1 = new BufferedReader(new InputStreamReader(System.in));
                    String content = br1.readLine();
                    Random random = new Random();

                    if (content.startsWith("arq")) {
                        String[] ml = content.split(" ");
                        int rn = random.nextInt(ml.length) + 1;
                        timers = new Timer[ml.length]; // Initialize timers array
                        ackReceived = new boolean[ml.length];

                        for (int zz = 0; zz < ml.length; zz++) {
                            if (zz == rn) {
                                continue;
                            } else {
                                String idd = "" + zz + "";
                                out.println(idd + "`" + ml[zz]);
                                ackReceived[zz] = false;

                                timers[zz] = new Timer();
                                scheduleTimerTask(timers[zz], ackReceived, zz, "FRAME:" + (zz), 5000);
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    
                                }
                            }
                        }

                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                           
                        }

                        for (int zz = 0; zz < ml.length; zz++) {
                            if (!ackReceived[zz]) {
                                System.out.println("ACK not received for FRAME: " + zz);
                                out.println(zz + "`" + "RESEND_FRAME");
                            }
                            timers[zz].cancel();
                        }

                        sendUnreceivedAcks();

                        System.out.println("Program exiting.");
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

    private void sendUnreceivedAcks() {
        StringBuilder unreceivedAcks = new StringBuilder();
        for (int zz = 0; zz < ackReceived.length; zz++) {
            if (!ackReceived[zz]) {
                unreceivedAcks.append(zz).append(",");
            }
        }
        if (unreceivedAcks.length() > 0) {
            unreceivedAcks.deleteCharAt(unreceivedAcks.length() - 1); // Remove the last comma
            out.println("resend-acks:" + unreceivedAcks.toString());
            out.flush();
        }
    }
    private void scheduleTimerTask(Timer timer, boolean[] ackReceived, int frameNumber, String taskName, long delay) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println(taskName + " timer ended after : " + delay + " milliseconds.");

                if (!ackReceived[frameNumber]) {
                    System.out.println("ACK not received for " + taskName);
                    out.println(frameNumber + "`" + "RESEND ");
                }
            }
        };
        simulateWorkOrChange(taskName);
        ackReceived[frameNumber] = true;
        timer.schedule(task, delay);
    }
    private static void simulateWorkOrChange(String c) {
        System.out.println(c + " is sent :");
    }
    public static void main(String[] args) {
        new clientsarq();
    }
}
