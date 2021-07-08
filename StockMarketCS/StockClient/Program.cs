using System;
using System.IO;
using System.Net.Sockets;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

namespace StockClient
{
    class Client : IDisposable
    {
        private readonly int port = 8888;
        private int ClientID { get; set; }
        private bool HasStock { get; set; } = false;
        private bool GetList { get; set; } = false;
        private bool ShowStockOwner { get; set; } = false;
        private int? StockOwner { get; set; } = null;
        private StreamReader reader;
        private StreamWriter writer;
        private List<int> availableClients = new List<int>();
        private static object Lock = new object();

        public Client() 
        {
            using (TcpClient tcpClient = new TcpClient("localhost", port))
            {
                NetworkStream stream = tcpClient.GetStream();
                reader = new StreamReader(stream);
                writer = new StreamWriter(stream);
                writer.AutoFlush = true;
                string line = "";

                ClientID = int.Parse(reader.ReadLine());
                HasStock = bool.Parse(reader.ReadLine());
                Console.WriteLine($"[Client ID: {ClientID}, has stock {HasStock}]");
            
                new Thread(ServerConnection).Start(tcpClient);
                GetAvailableClients();
                Sleep();

                while(!line.Equals("exit", StringComparison.InvariantCultureIgnoreCase))
                {
                    Console.WriteLine("\nChoose between the following options: " + 
                        "\n[1] See all available clients " + 
                        "\n[2] Transfer a stock to a client " +
                        "\n[3] Get ID of client who has the stock " +
                        "\n[exit] Close the client" +
                        "\nEnter your option: ");

                    line = Console.ReadLine();
                    switch(line) 
                    {
                        case "1":
                            GetAvailableClients();
                            Sleep();
                            break;
                        case "2":
                            GetAvailableClients();
                            try 
                            {
                                Sleep();
                                Console.WriteLine("Enter client ID to give stock to: ");
                                int pickedClient = int.Parse(Console.ReadLine());
                                if (availableClients != null && availableClients.Contains(pickedClient)) 
                                {
                                    TradeStock(pickedClient);
                                    Sleep();
                                }
                                else
                                    Console.WriteLine("\nClient does not exit");
                            }
                            catch(FormatException) 
                            {
                                Console.WriteLine("\nInvalid arugment format");
                            }
                            break;
                        case "3":
                            GetStockOwner();
                            Sleep();
                            break;
                        case "exit":
                            break;
                        default:
                            Console.WriteLine("\nInvalid argument, try again!");
                            Sleep();
                            break;
                    }
                }
            }
        }

        public void Sleep() 
        {
            Thread.Sleep(50);
        }

        public void GetAvailableClients()
        {
            GetList = true;
            writer.WriteLine("clients");
        }

        public void TradeStock(int pickedClient)
        {
            writer.WriteLine($"transfer {pickedClient.ToString()}");
        }

        public void GetStockOwner() 
        {
            ShowStockOwner = true;
            writer.WriteLine("stock");
        }

        public void ServerConnection(object param) 
        {
            TcpClient tcpClient = (TcpClient) param;
            using (NetworkStream stream = tcpClient.GetStream())
            {
                StreamReader reader = new StreamReader(stream);
                try
                {
                    while(true) 
                    {
                        string serverResponse = reader.ReadLine();
                        if(GetList)
                        {
                            lock(Lock)
                            {
                                availableClients  = serverResponse.Split(',').Select(int.Parse).ToList();
                                Console.WriteLine($"\nClients available to trade with: {string.Join(", ", availableClients)}");
                                GetList = false;
                            }
                        }
                        else if(ShowStockOwner) 
                        {
                            StockOwner = int.Parse(serverResponse);
                            Console.WriteLine($"Current owner of stock: {StockOwner}");
                            ShowStockOwner = false;
                        }
                        else
                        {
                            Console.WriteLine($"Server replied: {serverResponse}");
                        }            
                    }
                }
                catch
                {
                    
                }
                finally
                {
                    reader.Close();
                }
            }  
        }

        public void Dispose() {
            Console.WriteLine("Client close communication");
            reader.Close();
            writer.Close();      
        }
    }

    class Program
    {
        static void Main(string[] args) 
        {
            new Client();
        }
    }
}
