// See LICENSE for license details
#ifndef __BITBLASTER_H
#define __BITBLASTER_H

#include "core/bridge_driver.h"
#include "core/clock_info.h"
#include <functional>
#include <vector>

// TODO: FIX
struct BITBLASTERBRIDGEMODULE_struct {
  uint64_t initDone;
  uint64_t traceEnable;
  uint64_t hostTriggerCycleCountStartHigh;
  uint64_t hostTriggerCycleCountStartLow;
  uint64_t hostTriggerCycleCountEndHigh;
  uint64_t hostTriggerCycleCountEndLow;
  uint64_t triggerSelector;
};

class bitblaster_t final : public streaming_bridge_driver_t {
public:
  /// The identifier for the bridge type used for casts.
  static char KIND;

  bitblaster_t(simif_t &sim,
            StreamEngine &stream,
            const BITBLASTERBRIDGEMODULE_struct &mmio_addrs,
            int blasterno,
            const std::vector<std::string> &args,
            int stream_to_cpu_idx,
            int stream_to_cpu_depth,
            int stream_to_cpu_num_bits,
            int stream_from_cpu_idx,
            int stream_from_cpu_depth,
            int stream_from_cpu_num_bits,
            const ClockInfo &clock_info);
  ~bitblaster_t();

  virtual void init();
  virtual void tick();
  virtual void finish() { flush(); };

private:
  const BITBLASTERBRIDGEMODULE_struct mmio_addrs;
  const int stream_to_cpu_idx;
  const int stream_to_cpu_depth;
  const int steam_to_cpu_num_bits;
  const int stream_from_cpu_idx;
  const int stream_from_cpu_depth;
  const int steam_from_cpu_num_bits;

public:

private:
  ClockInfo clock_info;
  bool trace_enabled = true;

public:

private:
  long dma_addr;
  size_t process_tokens(int num_beats, int minium_batch_beats);

public:
  void flush();
};

#endif // __BITBLASTER_H
