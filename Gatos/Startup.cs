using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace Gatos
{
    public class ShutdownException : Exception
    {
        public UnobservedTaskExceptionEventArgs Event { get; private set; }

        public ShutdownException(UnobservedTaskExceptionEventArgs e) : base()
        {
            Event = e;
        }
    }

    public class Startup
    {
        public Startup(IConfiguration configuration)
        {
            Configuration = configuration;
        }

        public IConfiguration Configuration { get; }

        // This method gets called by the runtime. Use this method to add services to the container.
        public void ConfigureServices(IServiceCollection services)
        {
            services.AddMvc();
            services.AddSingleton<ICatAvailabilityService, CatAvailabilityService>();

            TaskScheduler.UnobservedTaskException += HandleUnhandledExceptions;
        }

        // This method gets called by the runtime. Use this method to configure the HTTP request pipeline.
        public void Configure(IApplicationBuilder app, IHostingEnvironment env)
        {
            if (env.IsDevelopment())
            {
                app.UseDeveloperExceptionPage();
            }

            app.UseMvc();
        }

        private static void HandleUnhandledExceptions(object o, UnobservedTaskExceptionEventArgs e)
        {
            throw new ShutdownException(e);
        }
    }
}
