#include "rocc.h"
#include <stdint.h>
#include <stdio.h>

int main(void) {
  uint64_t a = 5;
  uint64_t b = 10;
  uint64_t result;
  printf("Sending inputs %d and %d. \n", a, b);
  asm volatile("fence");
  ROCC_INSTRUCTION_DSS(0, result, a, b, 0);
  asm volatile("fence");
  printf("Received result %d. \n", result);
  if (result != a+b) 
    return result + 1;
  return 0;
}
