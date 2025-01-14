#include "rocc.h"
#include <stdint.h>

int main(void) {
  uint64_t a = 5;
  uint64_t b = 10;
  uint64_t result;
  asm volatile("fence");
  ROCC_INSTRUCTION_DSS(0, result, a, b, 0);
  asm volatile("fence");
  if (result != a+b) 
    return result + 1;
  return 0;
}