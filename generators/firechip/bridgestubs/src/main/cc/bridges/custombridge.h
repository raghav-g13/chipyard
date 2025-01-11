// See LICENSE for license details

#ifndef __CUSTOM_H
#define __CUSTOM_H

// TODO: NEED THIS?
#include "bridges/serial_data.h"
#include "core/bridge_driver.h"

#include <cstdint>
#include <memory>
#include <optional>
#include <signal.h>
#include <string>
#include <vector>

/**
 * Structure carrying the addresses of all fixed MMIO ports.
 *
 * This structure is instantiated when all bridges are populated based on
 * the target configuration.
 */
struct CUSTOMBRIDGEMODULE_struct {

  // TODO: CLEAN UP WHICH SIGNALS GET PASSED ON TO HOST MODULES - do we need to pass all ready/valids 
  // TXFIFO - INPUTS

  // uint64_t snoop_blockBytes;
  uint64_t snoop_write;
  uint64_t snoop_address;
  // uint64_t snoop_fifo_count;
  // uint64_t snoop_block;
  // uint64_t snoop_block_address;
  uint64_t in_valid;
  uint64_t in_ready;

  // RXFIFO - OUTS

  // uint64_t request_blockBytes;
  uint64_t request_write;
  uint64_t request_address;
  // uint64_t request_block;
  // uint64_t request_block_address;

  uint64_t out_valid; 
  uint64_t out_ready;
};

class custombridge_t final : public bridge_driver_t {
public:
  /// The identifier for the bridge type used for casts.
  static char KIND;

  /// Creates a bridge which interacts with standard streams or PTY.
  custombridge_t(simif_t &simif,
         const CUSTOMBRIDGEMODULE_struct &mmio_addrs,
         int customno, // TODO: OPTIONAL FOR NOW
         const std::vector<std::string> &args); // TODO: OPTIONAL FOR NOW

  ~custombridge_t() override;
  
  void init() override;
  void tick() override;
  void finish() override;
private:
  const CUSTOMBRIDGEMODULE_struct mmio_addrs;
  uint64_t num_requests;
};

#endif // __CUSTOM_H
