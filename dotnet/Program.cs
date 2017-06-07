using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Buggy
{
    class Locker
    {
        public string Name;
    }

    class Program
    {
        static List<string> _systemStateSendAttempts = new List<string>();

        static void ProcessResult(string result)
        {
            string s = "";
            for (int i = 0; i < result.Length; ++i)
                s += i.ToString() + result[i];
        }

        static void Fetch(string url)
        {
            var client = new HttpClient();
            for (int i = 0; i < 1000; ++i)
            {
                if (i % 10 == 0) Console.Write(".");
                string result = client.GetStringAsync(url).Result;
                ProcessResult(result);
            }
        }

        static void LoadConfig(string configFile)
        {
            string config = File.ReadAllText(configFile);
            Console.WriteLine("Configuration loaded successfully.");
        }

        static void Initialize()
        {
            while (true)
            {
                Console.WriteLine("Opening configuration file...");
                try { LoadConfig("/etc/buggy.conf"); return; }
                catch (Exception) { Thread.Sleep(1000); }
            }
        }

        static bool PushSystemState(string url)
        {
            var client = new HttpClient();
            try
            {
                _systemStateSendAttempts.Add(Encoding.ASCII.GetString(new byte[1000000]));
                client.PostAsync(url, new FormUrlEncodedContent(new [] {
                    new KeyValuePair<string, string>("state", "OK")
                })).Wait();
            }
            catch (Exception) { return false; }
            return true;
        }

        static void Push(string pushTargetUrl)
        {
            while (!PushSystemState(pushTargetUrl))
                Thread.Sleep(500);
        }

        static void Reconfigure()
        {
            var txLocker = new Locker { Name = "Transaction" };
            var configLocker = new Locker { Name = "Config" };
            lock (txLocker)
            {
                Task.Run(() => {
                    lock (configLocker)
                    {
                        lock (txLocker)
                        {
                        }
                    }
                });
                Thread.Sleep(1000);
                Console.WriteLine("Attempting configuration update...");
                lock (configLocker)
                {
                }
            }
        }

        static void UpdateAsync()
        {
            if (new Random().Next() % 3 == 0)
                throw new InvalidOperationException("The update operation cannot be completed.");
        }

        static void HandleUnhandledExceptions(object sender, EventArgs e)
        {
            throw new ApplicationException("Task crashed!");
        }

        static void Update()
        {
            TaskScheduler.UnobservedTaskException += HandleUnhandledExceptions;
            Task.Run(() => UpdateAsync());
        }

        static void Main(string[] args)
        {
            switch (args[0])
            {
                case "fetch": Fetch(args[1]); break;
                case "initialize": Initialize(); break;
                case "push": Push(args[1]); break;
                case "reconfigure": Reconfigure(); break;
                case "update": Update(); break;
            }
            Thread.Sleep(1000);
            GC.Collect();
            GC.WaitForPendingFinalizers();
            Thread.Sleep(1000);
        }
    }
}
