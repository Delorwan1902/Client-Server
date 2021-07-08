package Assignment01;

import javax.swing.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class StockClient extends JFrame implements AutoCloseable {
    private final int port = 8888;
    private int clientID;
    private boolean hasStock;

    private Socket socket;
    private ServerConnection sc;
    private PrintWriter out;
    private Scanner in;
    Scanner scanner;

    private void connectToServer() throws Exception {
        socket = new Socket("localhost", port);
        sc = new ServerConnection(socket, this);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new Scanner(socket.getInputStream());
    }

    public StockClient() throws Exception {
        connectToServer();
        scanner = new Scanner(System.in);
        String line = null;

        clientID = Integer.parseInt(in.nextLine()); //First line is always the client ID
        hasStock = Boolean.parseBoolean(in.nextLine());
        System.out.println("[Client ID: " + clientID + ", has stock: " + (hasStock ? "Yes" : "No") + "]");

        new Thread(sc).start();
        getAvailableClients();
        sleep();

        while (!"exit".equalsIgnoreCase(line)) {
                System.out.println("\nChoose between the following options: " +
                        "\n[1] See all available clients " +
                        "\n[2] Transfer stock to a client " +
                        "\n[3] Get ID of client who has the stock " +
                        "\n[exit] Close the client " +
                        "\nEnter your option: ");

                line = scanner.nextLine();
                switch (line) {
                    case "1":
                        getAvailableClients();
                        sleep();
                        break;
                    case "2":
                        getAvailableClients();
                        try {
                            sleep();
                            System.out.print("Enter client ID to give stock to: ");
                            int pickedClient = Integer.parseInt(scanner.nextLine());

                            if (sc.getClientList().contains(pickedClient)) {
                                tradeStock(pickedClient);
                                sleep();
                            } else
                                System.out.println("Client does not exit");
                        }
                        catch(NumberFormatException e) {
                            System.out.println("\nInvalid argument format");
                        }
                        break;
                    case "3":
                        getStockOwner();
                        sleep();
                        break;
                    case "exit":
                        break;
                    default:
                        System.out.println("Invalid argument, try again!");
                        sleep();
                        break;
                }
        }
    }

    public int getClientID() {
        return clientID;
    }

    public void setHasStock(boolean stock) {
        hasStock = stock;
    }

    public void tradeStock(int pickedClient) {
        out.println("transfer " + pickedClient);
    }

    public void getAvailableClients() {
        sc.checkForList();
        out.println("clients");
    }

    public void getStockOwner() {
        sc.checkForStockOwner();
        out.println("stock");
    }

    public void sleep() {
        try {
            Thread.sleep(50);
        }
        catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    //Method is called automatically when program is closed by user "exit"
    @Override
    public void close() {
        System.out.println("Client closed communication");
        out.close();
        in.close();
    }

    public static void main(String[] args) {
        try(StockClient client = new StockClient()) {

        }
        catch (Exception e) {
            System.out.println("StockClientProgram / " + e);
        }
    }
}
