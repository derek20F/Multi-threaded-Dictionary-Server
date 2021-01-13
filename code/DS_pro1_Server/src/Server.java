
/**
 *
 * @author: Chen-An Fan
 * @Student ID: 1087032
 * @email: chenanf@student.unimelb.edu.au
 */
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.SocketException;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;
import java.io.FileReader;

public class Server extends Thread {

    protected static Socket connectionSock;
    protected static ServerSocket serverSock;
    protected static int numberClient = 0;
    protected static JSONObject myDict;
    protected static int port;
    protected static String dictFile;

    public static void main(String[] args) {
        // Check the input arguments
        if (args.length != 2) {
            System.out.println("Wrong Command. Please use \"java –jar DictionaryServer.jar <port> <dictionary-file>\"");
            System.exit(1);
        }
        port = Integer.parseInt(args[0]);
        dictFile = args[1];

        // Set up connection and load the dictionary file from local drive.
        try {
            System.out.println("Waiting for a connection on port " + args[0]);
            Server.serverSock = new ServerSocket(port);
            loadDict(dictFile);
            while (true) {
                Server.connectionSock = serverSock.accept(); //This will bolck the server until the client connect to it.
                System.out.println(Server.connectionSock);
                Server newServer = new Server();
                Thread theThread = new Thread(newServer);
                numberClient++;
                System.out.println("number of client is: " + numberClient);
                theThread.start();

            }
        } catch (IOException e) {
            System.out.println("All client is disconnected");
            //numberClient = numberClient - 1;
            //System.out.println("number of client is: " + numberClient);
            //System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader clientInput = new BufferedReader(
                    new InputStreamReader(connectionSock.getInputStream(), "UTF-8")
            );
            BufferedWriter clientOutput = new BufferedWriter(
                    new OutputStreamWriter(connectionSock.getOutputStream(), "UTF-8")
            );

            String clientText = null;
            try {
                while ((clientText = clientInput.readLine()) != null) { //Block here, wait until data come in
                    System.out.println("Client asks the world: " + clientText);
                    String replyText = null;
                    /*
                    The beginning letter of each string from client will be treated as an command
                    `a`: for adding a new word to the dictionary
                    `d`: for deleting an existing word in the dictionary
                    `f`: for searching a word in the dictionary
                     */
                    String[] arrOfStr = clientText.split(" ", 2);
                    arrOfStr[0] = arrOfStr[0].toLowerCase();
                    if ((arrOfStr[0]).equals("a")) {
                        replyText = addWord(arrOfStr[1]);
                    } else if ((arrOfStr[0]).equals("d")) {
                        replyText = removeWord(arrOfStr[1]);
                    } else if ((arrOfStr[0]).equals("f")) {
                        replyText = getDefinition(arrOfStr[1]);
                    } else {
                        System.out.println("Wrong Command");
                    }

                    clientOutput.write(replyText + "\n");
                    clientOutput.flush();
                    System.out.println("Sent: " + replyText);

                }
            } catch (SocketException e) {
                //System.out.println("line 98");
                System.out.println("All clients are disconnected, turn off the server");
            }
            clientOutput.close();
            clientInput.close();
            //connectionSock.close();
        } catch (IOException e) {
            //System.out.println("line 104");
            System.out.println(e.getMessage());
        } finally {
            //System.out.println("line 120");
            System.out.println("One client is disconnected");
            numberClient = numberClient - 1;
            System.out.println("number of client is: " + numberClient);
            /*
            for counting number of clinet, if all clinet are disconnected, then save the dict
            */
            if (numberClient == 0) {

                saveDict(dictFile);
                if (Server.serverSock != null) {
                    try {
                        Server.serverSock.close();
                    } catch (IOException e) {
                        e.getMessage();
                    }
                }

            }
        }
    }

    /**
     * This method is used to load dictionary from computer
     *
     * @param dictFile: the file name of the dictionary in computer
     */
    public static void loadDict(String dictFile) {
        try {
            // parsing file "JSONExample.json" 
            Object obj = new JSONParser().parse(new FileReader(dictFile));
            // typecasting obj to JSONObject 
            Server.myDict = (JSONObject) obj;
        } catch (FileNotFoundException fe) {
            System.out.println("Can not find the json Dictionary file");
            fe.getMessage();
        } catch (IOException | ParseException ioe) {
            ioe.getMessage();
        }
    }

    /**
     * @param dictFile This function is used to write JSON to the local
     * dictionary in cwd
     */
    public static void saveDict(String dictFile) {
        try {
            PrintWriter pw = new PrintWriter(dictFile);
            pw.write(Server.myDict.toJSONString());
            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {
            e.getMessage();
        }
    }

    /**
     * This method is used to get definition of the word
     *
     * @param world
     * @return definition
     */
    public synchronized static String getDefinition(String world) {
        world = world.toLowerCase();
        String definition = (String) Server.myDict.get(world);
        if (definition == null) {
            definition = "Not Found";
        }
        return definition;
    }

    /**
     * This method is used to add world with definition in our dictionary
     *
     * @param worldDef
     * @return if the world has been succesfully added or not
     */
    public synchronized static String addWord(String worldDef) {
        String replyText;
        String[] worldAndDef = worldDef.split(" ", 2);
        //if (worldAndDef.length == 1) {
        //System.out.println(worldAndDef.length);
        // if the user do not give the definition, only give the world name
        if (worldAndDef[1].equals("")) {
            replyText = "World not added, plaese give the name/definition.";
        } else {
            String definition = getDefinition(worldAndDef[0]);
            if (definition.equals("Not Found")) {
                //addWord(worldAndDef[0], worldAndDef[1]);
                String world = worldAndDef[0].toLowerCase();
                Server.myDict.put(world, worldAndDef[1]);
                replyText = ("Added: " + world);
            } else {
                replyText = ("Failed: the word is already in the dictionary");
            }
        }
        return replyText;
    }

    /**
     * This method is for user to remove a word and its meaning in the
     * dictionary. A word been removed are not visible to all clients. After
     * remove, a notification will sent to client's GUI.
     *
     * @param word
     * @return if the word has been removed or not
     */
    public synchronized static String removeWord(String word) {
        word = word.toLowerCase();
        String definition = getDefinition(word);
        String result;
        if (definition.equals("Not Found")) {
            result = "this world is not exist in the dictionary";
            return result;
        } else {
            Server.myDict.remove(word);
            result = word + " has been removed";
            return result;
        }
    }
}
