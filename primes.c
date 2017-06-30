#include <stdio.h>
#include <omp.h>

int is_trivial_prime(int n)
{
	return n <= 2;
}

int is_divisible(int n, int d)
{
	return n % d == 0;
}

int is_prime(int n)
{
	if (is_trivial_prime(n))
		return 1;
	if (is_divisible(n, 2))
		return 0;
	for (int d = 3; d < n; d += 2)
		if (is_divisible(n, d))
			return 0;
	return 1;
}

int main()
{
	int count = 0;
#pragma omp parallel for reduction(+:count)
	for (int n = 2; n < 200000; ++n)
		if (is_prime(n))
			++count;
	printf("Primes found: %d\n", count);
	return 0;
}
