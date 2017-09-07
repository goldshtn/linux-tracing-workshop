using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.ModelBinding;

namespace Gatos.Controllers
{
    public class CatRequest
    {
        [BindRequired] public int MaxWeight { get; set; }
        [BindRequired] public int MaxAge { get; set; }
    }

    public class CatResponse
    {
        public int NumberOfCatsAvailable { get; set; }
        public string BestCatName { get; set; }
    }

    [Route("api/[controller]")]
    public class CatController : Controller
    {
        private readonly ICatAvailabilityService _cas;

        public CatController(ICatAvailabilityService cas)
        {
            _cas = cas;
        }

        [HttpPost]
        public IActionResult CatsAvailable([FromBody] CatRequest request)
        {
            if (!ModelState.IsValid)
                return BadRequest();

            var cats = _cas.GetAvailableCats(request.MaxWeight, request.MaxAge)
                           .ToList();

            return Json(new CatResponse
                        {
                            NumberOfCatsAvailable = cats.Count,
                            BestCatName = cats.FirstOrDefault()?.Name
                        });
        }

        [HttpPost("update")]
        public void UpdateRepository()
        {
            Task.Run(() => _cas.UpdateCatRepository());
        }
    }
}
