package no.ntnu.datakomm.chat;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;

    // Hint: if you want to store a message for the last error, store it here
    private String lastError = null;

    private final List<ChatListener> listeners = new LinkedList<>();

    /**
     * Connect to a chat server.
     *
     * @param host host name or IP address of the chat server
     * @param port TCP port of the chat server
     * @return True on success, false otherwise
     */
    public boolean connect(String host, int port) {
        //
        // Hint: Remember to process all exceptions and return false on error
        // Hint: Remember to set up all the necessary input/output stream variables
        try {
            connection = new Socket(host,port);
            toServer = new PrintWriter(connection.getOutputStream(),true);
            fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            System.out.println("connected");
            System.out.println("host: "+host+"\nport: "+port);
            return true;
        }
        catch (IOException e){
        return false;}
    }

    /**
     * Close the socket. This method must be synchronized, because several
     * threads may try to call it. For example: When "Disconnect" button is
     * pressed in the GUI thread, the connection will get closed. Meanwhile, the
     * background thread trying to read server's response will get error in the
     * input stream and may try to call this method when the socket is already
     * in the process of being closed. with "synchronized" keyword we make sure
     * that no two threads call this method in parallel.
     */
    public synchronized void disconnect() {
        // Hint: remember to check if connection is active
        if (isConnectionActive()){
            try {
                for (ChatListener l : listeners) {
                    l.onDisconnect();
                }
                connection.close();
                connection=null;
            }
            catch (IOException e){ }
        }
    }

    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive() {
        return connection != null;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd) {
        // Hint: Remember to check if connection is active
        if (isConnectionActive()) {
            toServer.println(cmd);
            return true;


        }
        return false;
    }

    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        String cmd = "msg "+message;
        return sendCommand(cmd);
    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        // Hint: Reuse sendCommand() method
        System.out.println("trying to login: "+ username);
        if(sendCommand("login "+ username)) {
        }
    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
        sendCommand("users");
    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
            toServer.println("privmsg "+recipient+" "+message);
            return true;
    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        // Hint: Reuse sendCommand() method
        sendCommand("help");
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() {
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.
        String result;
        try {
            result = fromServer.readLine();
            System.out.println(result);
            return result;
        }
        catch (IOException e){ }

        return null;
    }

    /**
     * Get the last error message
     *
     * @return Error message or "" if there has been no error
     */
    public String getLastError() {
        if (lastError != null) {
            return lastError;
        } else {
            return "";
        }
    }

    /**
     * Start listening for incoming commands from the server in a new CPU thread.
     */
    public void startListenThread() {
        // Call parseIncomingCommands() in the new thread.
        Thread t = new Thread(() -> {
            parseIncomingCommands();
        });
        t.start();
    }

    /**
     * Read incoming messages one by one, generate events for the listeners. A loop that runs until
     * the connection is closed.
     */
    private void parseIncomingCommands() {
        while (isConnectionActive()) {
            // Hint: Reuse waitServerResponse() method
            // Hint: Have a switch-case (or other way) to check what type of response is received from the server
            // and act on it.
            // Hint: In Step 3 you need to handle only login-related responses.
            // Hint: In Step 3 reuse onLoginResult() method

                String msg = waitServerResponse();
                if (msg == null){return;}
                String[] serverResponse =  msg.split(" ");
                String[] modifiedResponse;
            switch (serverResponse[0]){
                case "loginok":
                    onLoginResult(true,"");
                    break;

                case "loginerr":
                    onLoginResult(false,serverResponse[1]);
                    break;

                case "users":
                    serverResponse[0]=null;
                    modifiedResponse = new String[serverResponse.length-1];

                    for (int i=0;i<serverResponse.length;i++) {
                        if (i!=0){
                            modifiedResponse[i-1]=serverResponse[i];
                        }
                    }
                    onUsersList(modifiedResponse);
                    break;

                case "msg":
                    onMsgReceived(false,serverResponse[1],serverResponse[2]);
                    break;

                case "privmsg":
                    onMsgReceived(true,serverResponse[1],serverResponse[2]);
                    break;

                case "msgerr":
                    onMsgError(serverResponse[1]);
                    break;

                case "cmderr":
                    onCmdError(serverResponse[1]);
                    break;
                case "supported":
                    serverResponse[0]=null;
                    modifiedResponse = new String[serverResponse.length-1];

                    for (int i=0;i<serverResponse.length;i++) {
                        if (i!=0){
                            modifiedResponse[i-1]=serverResponse[i];
                        }
                    }
                    onSupported(modifiedResponse);
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * Register a new listener for events (login result, incoming message, etc)
     *
     * @param listener
     */
    public void addListener(ChatListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener
     */
    public void removeListener(ChatListener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are all event-notificators - notify all the listeners about a specific event.
    // By "event" here we mean "information received from the chat server".
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Notify listeners that login operation is complete (either with success or
     * failure)
     *
     * @param success When true, login successful. When false, it failed
     * @param errMsg  Error message if any
     */
    private void onLoginResult(boolean success, String errMsg) {
        for (ChatListener l : listeners) {
            l.onLoginResult(success, errMsg);
        }
    }

    /**
     * Notify listeners that socket was closed by the remote end (server or
     * Internet error)
     */
    private void onDisconnect() {
        // Hint: all the onXXX() methods will be similar to onLoginResult()
        for (ChatListener l : listeners) {
            l.onDisconnect();
        }
    }

    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users) {
        for (ChatListener l : listeners) {
            l.onUserList(users);
        }
    }

    /**
     * Notify listeners that a message is received from the server
     *
     * @param priv   When true, this is a private message
     * @param sender Username of the sender
     * @param text   Message text
     */
    private void onMsgReceived(boolean priv, String sender, String text) {
        TextMessage message = new TextMessage(sender,priv,text);
        for (ChatListener l : listeners) {
            l.onMessageReceived(message);
        }
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg) {
        for (ChatListener l : listeners) {
            l.onMessageError(errMsg);
        }
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        for (ChatListener l : listeners) {
            l.onCommandError(errMsg);
        }
    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands) {
        for (ChatListener l : listeners) {
            l.onSupportedCommands(commands);
        }
    }
}
