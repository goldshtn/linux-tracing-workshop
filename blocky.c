#include <pthread.h>
#include <unistd.h>
#include <stdio.h>

volatile long work_counter;
int req_count;
pthread_mutex_t q_lock;

void grab_lock() {
    pthread_mutex_lock(&q_lock);
}

void ungrab_lock() {
    pthread_mutex_unlock(&q_lock);
}

void do_work() {
    for (int i = 0; i < 50000; ++i)
        ++work_counter; // Burn CPU
}

void * request_processor(void *dummy) {
    printf("[*] Request processor initialized.\n");
    while (1) {
        grab_lock();
        do_work();
        ungrab_lock();
    }
    return NULL;
}

void flush_work() {
    usleep(10000); // 10ms
}

void * backend_handler(void *dummy) {
    printf("[*] Backend handler initialized.\n");
    while (1) {
        grab_lock();
        ++req_count;
        if (req_count % 1000 == 0) {
            printf("[-] Handled %d requests.\n", req_count);
        }
        if (req_count % 37 == 0) {
            flush_work();
        }
        ungrab_lock();
    }
    return NULL;
}

#define NTHREADS 2

int main() {
    pthread_t req_processors[NTHREADS];
    pthread_t backend;

    pthread_mutex_init(&q_lock, NULL);
    for (int i = 0; i < NTHREADS; ++i) {
        pthread_create(&req_processors[i], NULL, request_processor, NULL);
    }
    pthread_create(&backend, NULL, backend_handler, NULL);

    printf("[*] Ready to process requests.\n");
    pthread_join(backend, NULL);
    printf("[*] Exiting.\n");

    return 0;
}
