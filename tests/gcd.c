#include "mmio.h"

#define GCD_STATUS 0x4000
#define GCD_X 0x4004
#define GCD_Y 0x4008
#define GCD_GCD 0x400C

#define NUM_TRANSFERS 1

const uint32_t X[100] = {
    672, 345, 831, 527, 121, 887, 954, 248, 439, 930,
    349, 502, 765, 393, 215, 973, 454, 632, 811, 241,
    168, 578, 490, 712, 118, 837, 263, 579, 654, 392,
    750, 621, 187, 843, 999, 157, 470, 568, 231, 645,
    808, 510, 388, 729, 503, 682, 371, 823, 289, 514,
    156, 609, 283, 764, 496, 807, 370, 281, 711, 945,
    158, 459, 627, 742, 501, 693, 213, 573, 811, 263,
    840, 331, 782, 495, 907, 612, 366, 483, 297, 901,
    688, 546, 731, 125, 574, 268, 427, 932, 183, 789,
    524, 617, 433, 938, 171, 781, 269, 854, 602, 329
};

const uint32_t Y[100] = {
    285, 821, 460, 154, 992, 521, 384, 762, 437, 120,
    814, 765, 502, 138, 909, 487, 676, 295, 454, 732,
    507, 202, 473, 600, 285, 831, 406, 599, 230, 776,
    591, 810, 693, 410, 162, 421, 265, 105, 871, 597,
    314, 427, 590, 205, 807, 354, 289, 156, 390, 640,
    495, 523, 477, 106, 784, 729, 148, 516, 233, 386,
    485, 372, 816, 514, 755, 168, 413, 567, 339, 757,
    140, 904, 630, 191, 568, 305, 894, 470, 126, 583,
    421, 832, 310, 578, 209, 635, 478, 259, 572, 615,
    313, 457, 531, 496, 387, 515, 702, 382, 145, 873
};



unsigned int gcd_ref(unsigned int x, unsigned int y) {
  while (y != 0) {
    if (x > y)
      x = x - y;
    else
      y = y - x;
  }
  return x;
}

// DOC include start: GCD test
int main(void)
{
  uint32_t result, ref, x, y;

  for (int i = 0; i < NUM_TRANSFERS; i += 1) {
      
      x = X[i];
      y = Y[i];
      
      // wait for peripheral to be ready
      while ((reg_read8(GCD_STATUS) & 0x2) == 0) ;

      reg_write32(GCD_X, x);
      reg_write32(GCD_Y, y);


      // wait for peripheral to complete
      while ((reg_read8(GCD_STATUS) & 0x1) == 0) ;

      result = reg_read32(GCD_GCD);
      /* 
      ref = gcd_ref(x, y);

      if (result != ref) {
        printf("Hardware result %d does not match reference value %d\n", result, ref);
        return 1;
      }
      printf("Hardware result %d is correct for GCD iter %d\n", result, i);
      */  
      printf("Hardware result is %d for GCD iter %d\n", result, i);
  }

  return 0;
}
// DOC include end: GCD test
