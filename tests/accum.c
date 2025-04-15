#include "rocc.h"
#include <stdio.h>

static inline void accum_write(int idx, unsigned long data)
{
	asm volatile ("fence");
	ROCC_INSTRUCTION_SS(0, data, idx, 0);
	asm volatile ("fence");
}

static inline unsigned long accum_read(int idx)
{
	unsigned long value;
	asm volatile ("fence");
	ROCC_INSTRUCTION_DSS(0, value, 0, idx, 1);
	asm volatile ("fence");
	return value;
}

static inline void accum_load(int idx, void *ptr)
{
	asm volatile ("fence");
	ROCC_INSTRUCTION_SS(0, (uintptr_t) ptr, idx, 2);
	asm volatile ("fence");
}

static inline void accum_add(int idx, unsigned long addend)
{
	asm volatile ("fence");
	ROCC_INSTRUCTION_SS(0, addend, idx, 3);
	asm volatile ("fence");
}

unsigned long data = 0x3421L;

int main(void)
{
	unsigned long result;

	printf("\nCalling accum_load\n");
    accum_load(0, &data);
	printf("\nCalling accum_add\n");
	accum_add(0, 2);
    printf("\nCalling accum_read\n");
	result = accum_read(0);

	if (result != data + 2)
		return 1;

	printf("\nCalling accum_write\n");
	accum_write(0, 3);
	printf("\nCalling accum_add\n");
	accum_add(0, 1);
    printf("\nCalling accum_read\n");
	result = accum_read(0);

	if (result != 4)
		return 2;

	return 0;
}
