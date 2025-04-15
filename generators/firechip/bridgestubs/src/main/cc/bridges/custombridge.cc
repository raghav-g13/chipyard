// See LICENSE for license details

#include "custombridge.h"
#include "core/simif.h"

#include <stdio.h>
#include <stdlib.h>
#include <numeric>
#include "bridges/custombridge/model.h"

char custombridge_t::KIND;

custombridge_t::custombridge_t(simif_t &simif,
               const CUSTOMBRIDGEMODULE_struct &mmio_addrs,
               int customno,
               const std::vector<std::string> &args)
    : bridge_driver_t(simif, &KIND), mmio_addrs(mmio_addrs) {}

custombridge_t::~custombridge_t() = default;

void custombridge_t::init() {
  // NOTHING FOR NOW
  if (fifoHandler.openFifos()) {
    printf("ERROR opening fifos for custom bridge");
  }
  num_transactions = 0;
}

uint32_t getRd(uint32_t inst) {
    return (inst >> 7) & 0x1F; // Extract bits 11–7 and return
}

void custombridge_t::finish() { 
  fifoHandler.writeToCtrlFifo(Control::STOP);
  fifoHandler.closeFifos();
};

void custombridge_t::tick() {
  // printf("entering custombridge_t::tick()");
  uint32_t in_valid = read(mmio_addrs.in_valid);
  // printf("custombridge_t::tick() in_valid = %d \n", in_valid);
  while (in_valid) {
    uint32_t inst = read(mmio_addrs.inst);
    
    uint32_t rs1_0 = read(mmio_addrs.rs1_0);
    uint32_t rs1_1 = read(mmio_addrs.rs1_1);
    uint64_t rs1 = (static_cast<uint64_t>(rs1_1) << 32) | rs1_0;
    
    uint32_t rs2_0 = read(mmio_addrs.rs2_0);
    uint32_t rs2_1 = read(mmio_addrs.rs2_1);
    uint64_t rs2 = (static_cast<uint64_t>(rs2_1) << 32) | rs2_0;


    write(mmio_addrs.in_ready, true);
    printf("custombridge_t::tick() rs1 = %d \n", rs1);
    printf("custombridge_t::tick() rs2 = %d \n", rs2);

    if (!num_transactions) {
      fifoHandler.writeToCtrlFifo(Control::START);
    } else {
      fifoHandler.writeToCtrlFifo(Control::CONTINUE);
    }

    ins inputs{getRd(inst), rs1, rs2};
    fifoHandler.writeToInFifo(inputs);
    outs outputs = fifoHandler.readFromOutFifo(); 
    // uint32_t rd = getRd(inst); 
    // uint64_t data = rs1 + rs2;
    uint32_t rd = outputs.io_resp_bits_rd; 
    uint64_t data = outputs.io_resp_bits_data;
    printf("custombridge_t::tick() rd = %d \n", rd);
    printf("custombridge_t::tick() data = %d \n", data);

    // Extract the lower 32 bits
    uint32_t data_0 = static_cast<uint32_t>(data & 0xFFFFFFFF);
    // Extract the upper 32 bits
    uint32_t data_1 = static_cast<uint32_t>((data >> 32) & 0xFFFFFFFF);


    write(mmio_addrs.rd, rd);
    write(mmio_addrs.data_0, data_0);
    write(mmio_addrs.data_1, data_1);
    write(mmio_addrs.out_valid, true);
    
    in_valid = read(mmio_addrs.in_valid);
  }
  // printf("exiting custombridge_t::tick()");
}
