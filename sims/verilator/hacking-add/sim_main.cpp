// DESCRIPTION: Verilator: Verilog example module
//
// This file ONLY is placed under the Creative Commons Public Domain, for
// any use, without warranty, 2017 by Wilson Snyder.
// SPDX-License-Identifier: CC0-1.0
//======================================================================

// For std::unique_ptr
#include <memory>

// Include common routines
#include <verilated.h>

// Include model header, generated from Verilating "top.v"
#include "VAdd.h"
#include <iostream>

// Legacy function required only so linking works on Cygwin and MSVC++
double sc_time_stamp() { return 0; }

struct pointers {
    VerilatedContext* contextp;
    VAdd* top;
};

struct ins {
    uint64_t io_cmd_bits_inst_rd;
    uint64_t io_cmd_bits_rs1;
    uint64_t io_cmd_bits_rs2;
};

struct outs {
    uint64_t io_resp_bits_rd;
    uint64_t io_resp_bits_data;
};

pointers init();
outs step_to_output(pointers ptrs, ins inputs);
int finish();

int finish(pointers ptrs) {

    // Final model cleanup
    ptrs.top->final();

    // Coverage analysis (calling write only after the test is known to pass)
#if VM_COVERAGE
    Verilated::mkdir("logs");
    contextp->coveragep()->write("logs/coverage.dat");
#endif

    // Final simulation summary
    // contextp->statsPrintSummary();

    // Return good completion status
    // Don't use exit() or destructor won't get called
    delete ptrs.contextp;
    delete ptrs.top;
    return 0;

}

void run_n_steps(pointers ptrs, int n) {
    // Simulate until $finish
    while (!ptrs.contextp->gotFinish() && n > 0) {
        n -= 1;
        // Historical note, before Verilator 4.200 Verilated::gotFinish()
        // was used above in place of contextp->gotFinish().
        // Most of the contextp-> calls can use Verilated:: calls instead;
        // the Verilated:: versions just assume there's a single context
        // being used (per thread).  It's faster and clearer to use the
        // newer contextp-> versions.

        ptrs.contextp->timeInc(1);  // 1 timeprecision period passes...
        // Historical note, before Verilator 4.200 a sc_time_stamp()
        // function was required instead of using timeInc.  Once timeInc()
        // is called (with non-zero), the Verilated libraries assume the
        // new API, and sc_time_stamp() will no longer work.

        // Toggle a fast (time/2 period) clock
        ptrs.top->clock = !ptrs.top->clock;

        // Toggle control signals on an edge that doesn't correspond
        // to where the controls are sampled; in this example we do
        // this only on a negedge of clock, because we know
        // reset is not sampled there.

        // Evaluate model
        // (If you have multiple models being simulated in the same
        // timestep then instead of eval(), call eval_step() on each, then
        // eval_end_step() on each. See the manual.)
        ptrs.top->eval();

        // Read outputs
        VL_PRINTF("[%" PRId64 "] clock=%x rst=%x cmd_rd=%x cmd_val=%x rs1=%" PRIx64 " -> rs2=%" PRIx64
                  " data=%" PRIx64 "\n",
                  ptrs.contextp->time(), ptrs.top->clock, ptrs.top->reset, ptrs.top->io_cmd_ready, ptrs.top->io_cmd_valid, ptrs.top->io_cmd_bits_rs1, ptrs.top->io_cmd_bits_rs2, ptrs.top->io_resp_bits_data
                );
    }
}

pointers init() {

    // Create logs/ directory in case we have traces to put under it
    Verilated::mkdir("logs");

    // Construct a VerilatedContext to hold simulation time, etc.
    // Multiple modules (made later below with Vtop) may share the same
    // context to share time, or modules may have different contexts if
    // they should be independent from each other.

    // Using unique_ptr is similar to
    // "VerilatedContext* contextp = new VerilatedContext" then deleting at end.
    VerilatedContext* contextp = new VerilatedContext;
    // Do not instead make Vtop as a file-scope static variable, as the
    // "C++ static initialization order fiasco" may cause a crash

    // Set debug level, 0 is off, 9 is highest presently used
    // May be overridden by commandArgs argument parsing
    contextp->debug(0);

    // Randomization reset policy
    // May be overridden by commandArgs argument parsing
    contextp->randReset(2);

    // Verilator must compute traced signals
    contextp->traceEverOn(true);

    // Pass arguments so Verilated code can see them, e.g. $value$plusargs
    // This needs to be called before you create any model
    // contextp->commandArgs(argc, argv);

    // Construct the Verilated model, from Vtop.h generated from Verilating "top.v".
    // Using unique_ptr is similar to "Vtop* top = new Vtop" then deleting at end.
    // "TOP" will be the hierarchical name of the module.
    VAdd* top = new VAdd{contextp, "VAdd"};

    // Set Vtop's input signals
    top->reset = 1;
    top->clock = 0;
    top->io_cmd_valid = 0;
    top->io_resp_ready = 1; 

    // Simulate until $finish
    while (!contextp->gotFinish() && !(top->io_cmd_ready && !top->reset)) {
        // Historical note, before Verilator 4.200 Verilated::gotFinish()
        // was used above in place of contextp->gotFinish().
        // Most of the contextp-> calls can use Verilated:: calls instead;
        // the Verilated:: versions just assume there's a single context
        // being used (per thread).  It's faster and clearer to use the
        // newer contextp-> versions.

        contextp->timeInc(1);  // 1 timeprecision period passes...
        // Historical note, before Verilator 4.200 a sc_time_stamp()
        // function was required instead of using timeInc.  Once timeInc()
        // is called (with non-zero), the Verilated libraries assume the
        // new API, and sc_time_stamp() will no longer work.

        // Toggle a fast (time/2 period) clock
        top->clock = !top->clock;

        // Toggle control signals on an edge that doesn't correspond
        // to where the controls are sampled; in this example we do
        // this only on a negedge of clock, because we know
        // reset is not sampled there.
        if (!top->clock) {
            
            if (contextp->time() < 10) {
                top->reset = 1;  // Assert reset
            } else if (top->reset) {
                top->reset = 0;  // Deassert reset
            }             
            // if (top->io_cmd_ready && top->io_cmd_valid) {
            //     top->io_cmd_valid = 0;
            // }
            // if (contextp->time() == 10) {
            //     top->io_cmd_valid = 1;
            // }
            // if (top->io_resp_ready && top->io_resp_valid && !top->reset) {
            //     flag = 1;
            // }
        }

        // Evaluate model
        // (If you have multiple models being simulated in the same
        // timestep then instead of eval(), call eval_step() on each, then
        // eval_end_step() on each. See the manual.)
        top->eval();

        // Read outputs
        VL_PRINTF("[%" PRId64 "] clock=%x rst=%x cmd_rd=%x cmd_val=%x rs1=%" PRIx64 " -> rs2=%" PRIx64
                  " data=%" PRIx64 "\n",
                  contextp->time(), top->clock, top->reset, top->io_cmd_ready, top->io_cmd_valid, top->io_cmd_bits_rs1, top->io_cmd_bits_rs2, top->io_resp_bits_data
                );
    }

    pointers ptrs{contextp, top};
    // run_n_steps(ptrs, 2);

    return ptrs;

}

outs step_to_output(pointers ptrs, ins inputs) {

    int drive_ins_flag = 1;
    int return_outs_flag = 0;

    outs outputs; 

    // Simulate until $finish
    while (!ptrs.contextp->gotFinish() && !return_outs_flag) {
        // Historical note, before Verilator 4.200 Verilated::gotFinish()
        // was used above in place of contextp->gotFinish().
        // Most of the contextp-> calls can use Verilated:: calls instead;
        // the Verilated:: versions just assume there's a single context
        // being used (per thread).  It's faster and clearer to use the
        // newer contextp-> versions.

        ptrs.contextp->timeInc(1);  // 1 timeprecision period passes...
        // Historical note, before Verilator 4.200 a sc_time_stamp()
        // function was required instead of using timeInc.  Once timeInc()
        // is called (with non-zero), the Verilated libraries assume the
        // new API, and sc_time_stamp() will no longer work.

        // Toggle a fast (time/2 period) clock
        ptrs.top->clock = !ptrs.top->clock;

        assert(!ptrs.top->reset);

        // Toggle control signals on an edge that doesn't correspond
        // to where the controls are sampled; in this example we do
        // this only on a negedge of clock, because we know
        // reset is not sampled there.
        if (!ptrs.top->clock) {
            if (drive_ins_flag) {
                ptrs.top->io_cmd_valid = 1;
                ptrs.top->io_resp_ready=1;
                ptrs.top->io_cmd_bits_inst_rd = inputs.io_cmd_bits_inst_rd;
                ptrs.top->io_cmd_bits_rs1 = inputs.io_cmd_bits_rs1;
                ptrs.top->io_cmd_bits_rs2 = inputs.io_cmd_bits_rs2;
                drive_ins_flag = 0;
            } else if (ptrs.top->io_cmd_ready && ptrs.top->io_cmd_valid && !drive_ins_flag) {
                ptrs.top->io_cmd_valid = 0;
            }
            if (ptrs.top->io_resp_ready && ptrs.top->io_resp_valid) {
                ptrs.top->io_resp_ready = 0;
                outputs.io_resp_bits_data = ptrs.top->io_resp_bits_data;
                outputs.io_resp_bits_rd = ptrs.top->io_resp_bits_rd;
                return_outs_flag = 1;
            }
        }

        // Evaluate model
        // (If you have multiple models being simulated in the same
        // timestep then instead of eval(), call eval_step() on each, then
        // eval_end_step() on each. See the manual.)
        ptrs.top->eval();

        // Read outputs
        VL_PRINTF("[%" PRId64 "] clock=%x rst=%x cmd_rd=%x cmd_val=%x rs1=%" PRIx64 " -> rs2=%" PRIx64
                  " data=%" PRIx64 "\n",
                  ptrs.contextp->time(), ptrs.top->clock, ptrs.top->reset, ptrs.top->io_cmd_ready, ptrs.top->io_cmd_valid, ptrs.top->io_cmd_bits_rs1, ptrs.top->io_cmd_bits_rs2, ptrs.top->io_resp_bits_data
                );
    }

    return outputs;
}

int main(int argc, char** argv) {
    // This is a more complicated example, please also see the simpler examples/make_hello_c.

    pointers ptrs = init();

    ins inputs{5, 1, 2}; 

    auto [io_resp_bits_rd, io_resp_bits_data] = step_to_output(ptrs, inputs);

    std::cout<< "Data = " << io_resp_bits_data << std::endl;

    return finish(ptrs); 

}
