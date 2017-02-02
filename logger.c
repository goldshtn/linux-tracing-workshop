#include <unistd.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#define LARGE_WRITE 1048576
#define SMALL_WRITE 1024

void do_write(int fd, char *buf, int count) {
    write(fd, buf, count);
}

void write_flushed_data() {
    int fd;
    char *buf;
    buf = malloc(LARGE_WRITE);
    strcpy(buf, "Large data buffer follows.\n");
    fd = open("flush.data", O_CREAT | O_WRONLY | O_SYNC, 0644);
    do_write(fd, buf, LARGE_WRITE);
    close(fd);
    free(buf);
}

void *flusher(void *ignore) {
    while (1) {
        usleep(200000);
        write_flushed_data();
    }
    return NULL;
}

void logger() {
    int fd;
    char *buf;
    buf = malloc(SMALL_WRITE);
    strcpy(buf, "[*] INFO Writing a log message.\n");
    fd = open("log.data", O_CREAT | O_WRONLY | O_SYNC, 0644);
    while (1) {
        usleep(20000);
        do_write(fd, buf, SMALL_WRITE);
    }
    close(fd);
    free(buf);
}

int main() {
    pthread_t flusher_thr;
    pthread_create(&flusher_thr, NULL, flusher, NULL);
    logger();
    pthread_join(flusher_thr, NULL);
    return 0;
}
