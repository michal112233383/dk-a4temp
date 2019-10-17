package no.ntnu.datakomm;

import java.io.*;
import java.net.Socket;

public class testClient {
    public static void main (String[] args){
        testClient t = new testClient();
        t.run();
    }

    private void run() {
        System.out.println("Client has started");

        try {
            //Establish connection
            Socket socket = new Socket("localhost",1234);
            System.out.println("Successfully connected!");

            //Send message
            String commandToSend = "Hello from Client!";
            OutputStream out = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(out, true);
            writer.println(commandToSend);
            writer.println("");

            //Get response
            InputStream in = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String oneResponseLine;
            do {
                oneResponseLine = reader.readLine();
                if (oneResponseLine != null) {
                    System.out.println(oneResponseLine);
                }
            }while (oneResponseLine != null);
        }
        catch (IOException e) {

        }
    }
}
