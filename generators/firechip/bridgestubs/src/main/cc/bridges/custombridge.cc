// See LICENSE for license details

#include "custombridge.h"
#include "core/simif.h"

#include <stdio.h>
#include <stdlib.h>
#include <numeric>

char custombridge_t::KIND;

custombridge_t::custombridge_t(simif_t &simif,
               const CUSTOMBRIDGEMODULE_struct &mmio_addrs,
               int customno,
               const std::vector<std::string> &args)
    : bridge_driver_t(simif, &KIND), mmio_addrs(mmio_addrs) {}

custombridge_t::~custombridge_t() = default;

void custombridge_t::init() {
  // NOTHING FOR NOW
}

void custombridge_t::tick() {
  // printf("entering custombridge_t::tick()");
  uint32_t in_valid = read(mmio_addrs.in_valid);
  // printf("custombridge_t::tick() in_valid = %d \n", in_valid);
  while (in_valid) {
    uint32_t x = read(mmio_addrs.x);
    uint32_t y = read(mmio_addrs.y);
    write(mmio_addrs.in_ready, true);
    printf("custombridge_t::tick() x = %d \n", x);
    printf("custombridge_t::tick() y = %d \n", y);
    
    uint32_t gcd;
    if (x && y) {
      gcd = std::gcd(x, y);
    } else {
      gcd = 0;
    } 
    printf("custombridge_t::tick() gcd = %d \n", gcd);
    write(mmio_addrs.gcd, gcd);
    write(mmio_addrs.out_valid, true);
    
    in_valid = read(mmio_addrs.in_valid);
  }
  // printf("exiting custombridge_t::tick()");
}
