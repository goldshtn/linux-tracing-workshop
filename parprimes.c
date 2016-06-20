#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

int is_prime(int n) {
        int i;

        if (n <= 2) return 1;
        if (n % 2 == 0) return 0;

        for (i = 3; i < n; i += 2) {
                if (n % i == 0) return 0;
        }

        return 1;
}

struct search_range {
        int start;
        int end;
        int count;
        pthread_mutex_t *mutex;
};

void primes_loop(struct search_range *range)
{
        int i;

        for (i = range->start; i < range->end; ++i)
        {
                if (is_prime(i))
                {
                        pthread_mutex_lock(range->mutex);
                        ++(range->count);
                        pthread_mutex_unlock(range->mutex);
                }
        }
}

void *primes_thread(void *ctx) {
        struct search_range *range;

        range = (struct search_range *)ctx;
        primes_loop(range);

        return NULL;
}

int main(int argc, char* argv[]) {
        int nthreads, max;
        pthread_t *threads;
        struct search_range *ranges;
        int i;
        pthread_mutex_t mutex;
        
        puts("Hit ENTER to start.");
        getchar();

        pthread_mutex_init(&mutex, NULL); 

        if (argc >= 3) {
                nthreads = atoi(argv[1]);
                max = atoi(argv[2]);
        }
        else {
                nthreads = 2;
                max = 100000;
        } 

        threads = (pthread_t *)malloc(sizeof(pthread_t) * nthreads);
        ranges = (struct search_range *)malloc(sizeof(struct search_range) * nthreads);
        for (i = 0; i < nthreads; ++i) {
                ranges[i].start = i * (max / nthreads);
                ranges[i].end = (i + 1) * (max / nthreads);
                ranges[i].count = 0;
                ranges[i].mutex = &mutex;
                pthread_create(&threads[i], NULL, primes_thread, &ranges[i]);
        }

        for (i = 0; i < nthreads; ++i) {
                pthread_join(threads[i], NULL);
                printf("thread %d found %d primes\n", i, ranges[i].count);
        }

        free(ranges);
        free(threads);

        return 0;
}

