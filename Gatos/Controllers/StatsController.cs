using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using System.Xml;
using Microsoft.AspNetCore.Mvc;

namespace Gatos.Controllers
{
    [Route("api/[controller]")]
    public class StatsController : Controller
    {
        private static readonly Random s_rand = new Random();

        [HttpPut]
        public void Put([FromBody] string stats)
        {
            if (s_rand.Next() % 17 == 0)
            {
                System.IO.File.WriteAllBytes("stats.bin", new byte[1000000]);
            }
            else
            {
                System.IO.File.WriteAllBytes("stats.bin", new byte[1000]);
            }
        }

        [HttpGet]
        public int Get()
        {
            XmlDocument doc = new XmlDocument();
            XmlElement root = doc.CreateElement("root");
            doc.AppendChild(root);
            for (int i = 0; i < 10000; ++i)
            {
                XmlElement childStat = doc.CreateElement($"stat{i}");
                root.AppendChild(childStat);
            }
            return doc.OuterXml.Length;
        }
    }
}
