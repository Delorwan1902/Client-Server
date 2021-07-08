using System;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Text;
using System.Collections.Generic;
using System.Linq;

namespace StockServer
{
    class StockMarket
    {
        public int ClientID { get; set; }
        public bool ActiveInMarket { get; set; }
        public bool HasStock { get; set; }
        
        public StockMarket(int clientID) 
        {
            this.ClientID = clientID;
            ActiveInMarket = true;
            HasStock = false;
        }

        public string toString() 
        {
            StringBuilder sb = new StringBuilder();
            sb.Append($"Client ID: {ClientID}, has stock: {HasStock}");
            return sb.ToString();
        }
    }

    class StockClientHandler 
    {
        private StockMarket client;
        private List<StockClientHandler> handlers;
        private int clientID;
        public StreamWriter Writer { get; set; } = null;
        
        public StockClientHandler(StockMarket client, List<StockClientHandler> handlers)
        {
            this.client = client;
            this.clientID = client.ClientID;
            this.handlers = handlers;
        }

        public void TheThread(object param) 
        {
            TcpClient tcpClient = (TcpClient) param;
            using (Stream stream = tcpClient.GetStream())
            {
                Writer = new StreamWriter(stream);
                StreamReader reader = new StreamReader(stream);
                try
                {
                    Console.WriteLine($"Client {clientID} has connected");
                    Writer.WriteLine(clientID);
                    Writer.Flush();
                    Writer.WriteLine(client.HasStock);
                    Writer.Flush();

                    foreach (StockClientHandler sch in handlers) 
                    {
                        if(sch != this && sch.Writer != null) 
                        {
                            sch.Writer.WriteLine($"Client {clientID} has connected [{Program.GetClientIDs()}]");
                            sch.Writer.Flush();
                        }
                    }
                    
                    bool exit = false;
                    while (!exit)
                    {
                        string line = reader.ReadLine();
                        string[] substrings = line.Split(' ');
                        switch (substrings[0].ToLower()) 
                        {
                            case "clients":
                                Writer.WriteLine(Program.GetClientIDs());
                                Writer.Flush();
                                break;
                            case "transfer":
                                if(client.HasStock == true) 
                                {
                                    try 
                                    {
                                        StockMarket sm = Program.getClient(int.Parse(substrings[1]));
                                        client.HasStock = false;
                                        sm.HasStock = true;
                                        foreach (StockClientHandler sch in handlers) 
                                        {
                                            if(sch.client.ClientID == int.Parse(substrings[1]) && sch.Writer != null)
                                            {
                                                sch.Writer.WriteLine("You have now received the stock. You can now trade with other available clients");
                                                sch.Writer.Flush();
                                                Program.StockOwnerID = int.Parse(substrings[1]);
                                            }
                                        }
                                        Writer.WriteLine("SUCCESS");
                                        Writer.Flush();
                                    }
                                    catch(Exception) 
                                    {
                                        Writer.WriteLine("Client does not exit");
                                        Writer.Flush();
                                    }
                                }
                                else 
                                {
                                    Writer.WriteLine("You have no stock to trade with");
                                    Writer.Flush();
                                }
                                break;
                            case "stock":
                                Writer.WriteLine(Program.StockOwnerID.ToString());
                                Writer.Flush();
                                break;
                            case "exit":
                                Writer.WriteLine("exit");
                                Writer.Flush();
                                exit = true;
                                break;
                            default:
                                Writer.WriteLine("Invalid argument, try again!");
                                Writer.Flush();
                                break;    
                        }
                    }
                }
                catch
                {
                    if(client.HasStock == true) 
                    {
                        List<StockClientHandler> temp = new List<StockClientHandler>();
                        foreach (StockClientHandler sch in handlers)  
                        {
                            if(sch != this && sch.Writer != null)
                                temp.Add(sch);
                        }
                        if(temp.Any())
                        {
                            Random random = new Random();
                            StockClientHandler sch = temp[new Random().Next(temp.Count)];
                            sch.client.HasStock = true;
                            sch.Writer.WriteLine("You have now received the stock. You can now trade with other available clients");
                            sch.Writer.Flush();    
                        }
                        else {
                            Program.HasStock = true;
                            Program.StockOwnerID = null;
                            Console.WriteLine("No clients available, server has the stock temporarily");
                        }
                    }
                    
                    client.HasStock = false;
                    client.ActiveInMarket = false;

                    try
                    {
                        tcpClient.Close();
                    }
                    catch (Exception e)
                    {
                        Console.WriteLine($"StockClientHandler / {e}");
                    }
                }
                finally
                {
                    Console.WriteLine($"Client {clientID} disconnected.");
                    Writer.Close();
                    reader.Close();
                    Writer = null;
                }
            }  
        }
    }

    class Program
    {
        private static readonly int port = 8888;
        private static List<StockClientHandler> handlers = new List<StockClientHandler>();
        private static List<StockMarket> stockMarket = new List<StockMarket>();
        public static bool HasStock{ get; set; } = true;
        public static int? StockOwnerID { get; set; } = null;
        static object Lock = new object();

        public static StockMarket CreateClientID() {
            lock (Lock) {
                StockMarket sm;
                bool isEmpty = !stockMarket.Any();
                if (isEmpty) {
                    sm = new StockMarket(0);
                    stockMarket.Add(sm);
                    return sm; //Return 0 as no clients have connected until now
                }
                stockMarket.Sort((p, q) => p.ClientID.CompareTo(q.ClientID));
                sm = new StockMarket(stockMarket[stockMarket.Count - 1].ClientID + 1);
                stockMarket.Add(sm);
                return sm;
            }
        }

        public static string GetClientIDs() 
        {
            List<string> clientIDs = new List<string>();
            for(int i = 0; i < stockMarket.Count; i++) 
            {
                if(stockMarket[i].ActiveInMarket == true) 
                    clientIDs.Add(stockMarket[i].ClientID.ToString());  
            }
            string clientsToString = string.Join(",", clientIDs);
            return clientsToString;
        }

        public static StockMarket getClient(int clientID) 
        {
            for(int i = 0; i < stockMarket.Count; i++) {
                if(stockMarket[i].ClientID == clientID)
                    return stockMarket[i];
            }
            throw new Exception("Client could not be found");
        }
        
        static void Main(string[] args)
        {
            TcpListener listener  = new TcpListener(IPAddress.Loopback, port);
            listener.Start();
            Console.WriteLine("Waiting for connections...");
            while(true) 
            {
                TcpClient tcpClient = listener.AcceptTcpClient();
                StockMarket newClient = CreateClientID();

                lock(Lock) 
                {
                    if(HasStock == true) 
                    {
                        newClient.HasStock = true;
                        StockOwnerID = newClient.ClientID;
                        HasStock = false;
                    }
                }

                StockClientHandler sch = new StockClientHandler(newClient, handlers);
                handlers.Add(sch);
                new Thread(sch.TheThread).Start(tcpClient);
            }
        }
    }
}
