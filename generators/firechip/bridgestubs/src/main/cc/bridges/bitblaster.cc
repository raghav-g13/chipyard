// See LICENSE for license details

#include "bitblaster.h"

#include <cassert>
#include <cinttypes>
#include <climits>
#include <cstdio>
#include <cstring>

#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

#include <sys/mman.h>

char bitblaster_t::KIND;

// put FIREPERF in a mode that writes a simple log for processing later.
// useful for iterating on software side only without re-running on FPGA.
// #define FIREPERF_LOGGER

bitblaster_t::bitblaster_t(simif_t &sim,
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
          const ClockInfo &clock_info)
    : streaming_bridge_driver_t(sim, stream, &KIND), mmio_addrs(mmio_addrs),
      stream_to_cpu_idx(stream_to_cpu_idx), stream_to_cpu_depth(stream_to_cpu_depth),
      steam_to_cpu_num_bits(stream_to_cpu_num_bits),
      stream_from_cpu_idx(stream_from_cpu_idx), stream_from_cpu_depth(stream_from_cpu_depth),
      steam_from_cpu_num_bits(stream_from_cpu_num_bits),
      clock_info(clock_info) {

      return;
}

bitblaster_t::~bitblaster_t() {
  // if (this->tracefile) {
  //   fclose(this->tracefile);
  // }
  return;
}

void bitblaster_t::init() {
  if (!this->trace_enabled) {
    // Explicitly disable token collection in the bridge if no tracefile was
    // provided to improve FMR
    write(this->mmio_addrs.traceEnable, 0);
  } else {
    write(this->mmio_addrs.traceEnable, 1);
  }

  // Configure the trigger even if tracing is disabled, as other
  // instrumentation, like autocounter, may use tracerv-hosted trigger sources.
  // if (this->trigger_selector == 1) {
  //   write(mmio_addrs.triggerSelector, this->trigger_selector);
  //   write(mmio_addrs.hostTriggerCycleCountStartHigh,
  //         this->trace_trigger_start >> 32);
  //   write(mmio_addrs.hostTriggerCycleCountStartLow,
  //         this->trace_trigger_start & ((1ULL << 32) - 1));
  //   write(mmio_addrs.hostTriggerCycleCountEndHigh,
  //         this->trace_trigger_end >> 32);
  //   write(mmio_addrs.hostTriggerCycleCountEndLow,
  //         this->trace_trigger_end & ((1ULL << 32) - 1));
  //   printf("TracerV: Trigger enabled from %lu to %lu cycles\n",
  //          trace_trigger_start,
  //          trace_trigger_end);
  // } else if (this->trigger_selector == 2) {
  //   write(mmio_addrs.triggerSelector, this->trigger_selector);
  //   write(mmio_addrs.hostTriggerPCStartHigh, this->trigger_start_pc >> 32);
  //   write(mmio_addrs.hostTriggerPCStartLow,
  //         this->trigger_start_pc & ((1ULL << 32) - 1));
  //   write(mmio_addrs.hostTriggerPCEndHigh, this->trigger_stop_pc >> 32);
  //   write(mmio_addrs.hostTriggerPCEndLow,
  //         this->trigger_stop_pc & ((1ULL << 32) - 1));
  //   printf("TracerV: Trigger enabled from instruction address %lx to %lx\n",
  //          trigger_start_pc,
  //          trigger_stop_pc);
  // } else if (this->trigger_selector == 3) {
  //   write(mmio_addrs.triggerSelector, this->trigger_selector);
  //   write(mmio_addrs.hostTriggerStartInst, this->trigger_start_insn);
  //   write(mmio_addrs.hostTriggerStartInstMask, this->trigger_start_insn_mask);
  //   write(mmio_addrs.hostTriggerEndInst, this->trigger_stop_insn);
  //   write(mmio_addrs.hostTriggerEndInstMask, this->trigger_stop_insn_mask);
  //   printf("TracerV: Trigger enabled from start trigger instruction %x masked "
  //          "with %x, to end trigger instruction %x masked with %x\n",
  //          this->trigger_start_insn,
  //          this->trigger_start_insn_mask,
  //          this->trigger_stop_insn,
  //          this->trigger_stop_insn_mask);
  // } else {
  //   // Writing 0 to triggerSelector permanently enables the trigger
  //   write(mmio_addrs.triggerSelector, this->trigger_selector);
  //   printf("TracerV: No trigger selected. Trigger enabled from %lu to %lu "
  //          "cycles\n",
  //          0ul,
  //          ULONG_MAX);
  // }
  write(mmio_addrs.triggerSelector, 0);
  write(mmio_addrs.initDone, true);
}

size_t bitblaster_t::process_tokens(int num_beats, int minimum_batch_beats) {
  size_t maximum_batch_bytes = num_beats * STREAM_WIDTH_BYTES;
  size_t minimum_batch_bytes = minimum_batch_beats * STREAM_WIDTH_BYTES;
  // TODO. as opt can mmap file and just load directly into it.
  page_aligned_sized_array(OUTBUF, this->stream_to_cpu_depth * STREAM_WIDTH_BYTES);
  auto bytes_received =
      pull(this->stream_to_cpu_idx, OUTBUF, maximum_batch_bytes, minimum_batch_bytes);
  // check that a tracefile exists (one is enough) since the manager
  // does not create a tracefile when trace_enable is disabled, but the
  // TracerV bridge still exists, and no tracefile is created by default.
  printf("bytes_received %zu\n", bytes_received);
  for (size_t i = 0; i < (bytes_received / sizeof(uint64_t)); i += 8) {
    printf("%016lx", OUTBUF[i + 0]);
    printf("%016lx", OUTBUF[i + 1]);
    printf("%016lx", OUTBUF[i + 2]);
    printf("%016lx", OUTBUF[i + 3]);
    printf("%016lx", OUTBUF[i + 4]);
    printf("%016lx", OUTBUF[i + 5]);
    printf("%016lx", OUTBUF[i + 6]);
    printf("%016lx\n", OUTBUF[i + 7]);
  }
  return bytes_received;
}

void bitblaster_t::tick() {
  if (this->trace_enabled) {
    process_tokens(this->stream_to_cpu_depth, this->stream_to_cpu_depth);
  }
}

// Pull in any remaining tokens and flush them to file
void bitblaster_t::flush() {
  pull_flush(stream_to_cpu_idx);
  while (this->trace_enabled && (process_tokens(this->stream_to_cpu_depth, 0) > 0))
    ;
}
