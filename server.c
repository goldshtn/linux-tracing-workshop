#include <unistd.h>
#include <stdio.h>

void read_config() {
    int success = 0;
    while (!success) {
        FILE *fp;
        fp = fopen("/etc/tracing-server-example.conf", "r");
        if (fp) {
            success = 1;
            printf("[*] Read configuration successfully.\n");
            fclose(fp);
        } else {
            usleep(1);
        }
    }
}

int main() {
    printf("[*] Server starting...\n");
    read_config();
    printf("[*] Server started successfully.\n");
    while (1) {
        printf("[*] Processing request... ");
        sleep(1);
        printf("DONE.\n");
    }
    return 0;
}
