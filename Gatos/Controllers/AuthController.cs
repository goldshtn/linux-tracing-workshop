using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Cryptography.KeyDerivation;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.ModelBinding;

namespace Gatos.Controllers
{
    public class AuthParameters
    {
        [BindRequired] public string Username { get; set; }
        [BindRequired] public string Password { get; set; }
    }

    [Route("api/[controller]")]
    public class AuthController : Controller
    {
        [HttpPost]
        public void Post([FromBody] AuthParameters auth)
        {
            string hashed = Convert.ToBase64String(
                    KeyDerivation.Pbkdf2(auth.Password, new byte[] {1,2,3,4},
                                         KeyDerivationPrf.HMACSHA1, 10000, 64));
            // Actually doing something with the hash now
        }
    }
}
