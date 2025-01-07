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
  // uint32_t in_valid = read(mmio_addrs.in_valid);
  
  uint64_t snoop_fifo_count = read(mmio_addrs.snoop_fifo_count);
  // printf("custombridge_t::tick() in_valid = %d \n", in_valid);
  // printf("entering custombridge_t::tick() w snoop_fifo_count = %d \n", snoop_fifo_count);
  while (read(mmio_addrs.in_valid)) {
    
    // uint64_t snoop_blockBytes = read(mmio_addrs.snoop_blockBytes);
    uint64_t snoop_write = read(mmio_addrs.snoop_write);
    uint64_t snoop_address = read(mmio_addrs.snoop_address);
    // uint64_t snoop_block = read(mmio_addrs.snoop_block);
    // uint64_t snoop_block_address = read(mmio_addrs.snoop_block_address);
    
    write(mmio_addrs.in_ready, true);
    
    // printf("custombridge_t::tick() snoop_blockBytes = %d \n", snoop_blockBytes);
    // printf("custombridge_t::tick() snoop_write = %d \n", snoop_write);
    // printf("custombridge_t::tick() snoop_address = %d \n", snoop_address);
    // printf("custombridge_t::tick() snoop_block = %d \n", snoop_block);
    // printf("custombridge_t::tick() snoop_block_address = %d \n", snoop_block_address);

    write(mmio_addrs.out_valid, false);
    
    // in_valid = read(mmio_addrs.in_valid);
    uint64_t snoop_fifo_count = read(mmio_addrs.snoop_fifo_count);
    // printf("inside loop in custombridge_t::tick() snoop_fifo_count = %d \n", snoop_fifo_count);

    if (snoop_fifo_count == 100) {
      printf("hit limit \n");
      return;
    }
  
  }
  // printf("exiting custombridge_t::tick()");
}
