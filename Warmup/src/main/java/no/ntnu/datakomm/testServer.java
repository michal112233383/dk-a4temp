package no.ntnu.datakomm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class testServer {
    private static final int PORT = 1234;

    public static void main(String[] args){
        testServer s = new testServer();
        s.run();
    }

    private void run() {
        try {
            ServerSocket welcomeSocket = new ServerSocket(PORT);
            System.out.println("Server started on port "+ PORT);

            boolean mustRun = true;
            while (mustRun) {
                Socket clientSocket = welcomeSocket.accept();

                InputStreamReader reader = new InputStreamReader(clientSocket.getInputStream());
                BufferedReader bufReader = new BufferedReader(reader);

                String clientInput = bufReader.readLine();
                System.out.println("Client sent: " + clientInput);
                String[] parts = clientInput.split(" ");
                String response;
                //We expect 3 words
                if (parts.length == 3) {
                    response = (parts[0]+" "+parts[1].toUpperCase()+" "+parts[2]);
                } else{
                    response = "ERROR";
                }


                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                writer.println(response);
                System.out.println("Sent to Client: " + response);
                //close connection to the particular client
                clientSocket.close();
            }
            //close the listening socket, allow other services to listen on this port
            welcomeSocket.close();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }
}
