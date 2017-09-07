using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;

namespace Gatos
{
    public class Cat
    {
        public string Name { get; set; }
        public int Age { get; set; }
        public int Weight { get; set; }
    }

    public interface ICatAvailabilityService
    {
        IEnumerable<Cat> GetAvailableCats(int maxWeight, int maxAge);
        void UpdateCatRepository();
    }

    public class CatAvailabilityService : ICatAvailabilityService
    {
        private readonly object _syncLock = new object();

        public IEnumerable<Cat> GetAvailableCats(int maxWeight, int maxAge)
        {
            lock (_syncLock)
            {
                Thread.Sleep(100);  // Simulate external service call
                return new List<Cat>
                {
                    new Cat { Name = "Jefferson", Age = maxAge, Weight = maxWeight },
                    new Cat { Name = "Diego", Age = 1, Weight = 3 },
                    new Cat { Name = "Bertha", Age = maxAge, Weight = 10 }
                };
            }
        }

        public void UpdateCatRepository()
        {
            throw new ApplicationException("Cat repository is not available");
        }
    }
}
