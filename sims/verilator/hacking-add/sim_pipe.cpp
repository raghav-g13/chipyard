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
#include "fifos.h"
#include <iostream>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

// Legacy function required only so linking works on Cygwin and MSVC++
double sc_time_stamp() { return 0; }

struct pointers {
    std::unique_ptr<VerilatedContext> contextp;
    VAdd* top;
};

pointers ptrs;

void init(int argc, char** argv);
outs step_to_output(ins inputs);
int finish();

ins read_from_in_fifo();
int read_from_ctrl_fifo();
int write_to_out_fifo(outs outputs);

int open_fifos() {
    // Create FDs
    in_fd = open(in_fifo_path, O_RDONLY);
    if (in_fd == -1) {
        perror("open");
        return 1;
    }

    out_fd = open(out_fifo_path, O_WRONLY);
    if (out_fd == -1) {
        perror("open");
        return 1;
    }
    
    ctrl_fd = open(ctrl_fifo_path, O_RDONLY);
    if (ctrl_fd == -1) {
        perror("open");
        return 1;
    }
    return 0;
}

int finish() {

    // Final model cleanup
    ptrs.top->final();

    // Coverage analysis (calling write only after the test is known to pass)
#if VM_COVERAGE
    Verilated::mkdir("logs");
    ptrs.contextp->coveragep()->write("logs/coverage.dat");
#endif

    // Final simulation summary
    // ptrs.contextp->statsPrintSummary();

    // Return good completion status
    // Don't use exit() or destructor won't get called

    close_fifos();
    delete_fifos();

    return 0;

}

void run_n_steps(int n) {
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
        VL_PRINTF("[%" PRId64 "] clock=%x rst=%x cmd_rd=%x cmd_val=%x rs1=%" PRIx64 " rs2=%" PRIx64
                  " -> data=%" PRIx64 "\n",
                  ptrs.contextp->time(), ptrs.top->clock, ptrs.top->reset, ptrs.top->io_cmd_ready, ptrs.top->io_cmd_valid, ptrs.top->io_cmd_bits_rs1, ptrs.top->io_cmd_bits_rs2, ptrs.top->io_resp_bits_data
                );
    }
}

void init(int argc, char** argv) {

    // Create logs/ directory in case we have traces to put under it
    Verilated::mkdir("logs");

    // Construct a VerilatedContext to hold simulation time, etc.
    // Multiple modules (made later below with Vtop) may share the same
    // context to share time, or modules may have different contexts if
    // they should be independent from each other.


    // Using unique_ptr is similar to
    // "VerilatedContext* contextp = new VerilatedContext" then deleting at end.
    // VerilatedContext* contextp = new VerilatedContext;
    // Do not instead make Vtop as a file-scope static variable, as the
    // "C++ static initialization order fiasco" may cause a crash

    // Set debug level, 0 is off, 9 is highest presently used
    // May be overridden by commandArgs argument parsing
    ptrs.contextp->debug(0);

    // Randomization reset policy
    // May be overridden by commandArgs argument parsing
    ptrs.contextp->randReset(2);

    // Verilator must compute traced signals
    ptrs.contextp->traceEverOn(true);

    // Pass arguments so Verilated code can see them, e.g. $value$plusargs
    // This needs to be called before you create any model
    ptrs.contextp->commandArgs(argc, argv);

    // Construct the Verilated model, from Vtop.h generated from Verilating "top.v".
    // Using unique_ptr is similar to "Vtop* top = new Vtop" then deleting at end.
    // "TOP" will be the hierarchical name of the module.
    // VAdd* top = new VAdd{contextp, "TOP"};

    // Set Vtop's input signals
    ptrs.top->reset = 1;
    ptrs.top->clock = 0;
    ptrs.top->io_cmd_valid = 0;
    ptrs.top->io_resp_ready = 1; 

    // Simulate until $finish
    while (!ptrs.contextp->gotFinish() && !(ptrs.top->io_cmd_ready && !ptrs.top->reset)) {
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
        if (!ptrs.top->clock) {
            
            if (ptrs.contextp->time() < 10) {
                ptrs.top->reset = 1;  // Assert reset
            } else if (ptrs.top->reset) {
                ptrs.top->reset = 0;  // Deassert reset
            }             
            // if (ptrs.top->io_cmd_ready && ptrs.top->io_cmd_valid) {
            //     ptrs.top->io_cmd_valid = 0;
            // }
            // if (contextp->time() == 10) {
            //     ptrs.top->io_cmd_valid = 1;
            // }
            // if (ptrs.top->io_resp_ready && ptrs.top->io_resp_valid && !ptrs.top->reset) {
            //     flag = 1;
            // }
        }

        // Evaluate model
        // (If you have multiple models being simulated in the same
        // timestep then instead of eval(), call eval_step() on each, then
        // eval_end_step() on each. See the manual.)
        ptrs.top->eval();

        // Read outputs
        VL_PRINTF("[%" PRId64 "] clock=%x rst=%x cmd_rd=%x cmd_val=%x rs1=%" PRIx64 " rs2=%" PRIx64
                  " -> data=%" PRIx64 "\n",
                  ptrs.contextp->time(), ptrs.top->clock, ptrs.top->reset, ptrs.top->io_cmd_ready, ptrs.top->io_cmd_valid, ptrs.top->io_cmd_bits_rs1, ptrs.top->io_cmd_bits_rs2, ptrs.top->io_resp_bits_data
                );
    }

    // pointers ptrs{contextp, top};
    // run_n_steps(ptrs, 2);

    // return ptrs;

}

outs step_to_output(ins inputs) {

    int drive_ins_flag = 1;
    int return_outs_flag = 0;
    int return_outs_flag_delayed = 0;

    outs outputs; 

    // Simulate until $finish
    while (!ptrs.contextp->gotFinish() && !(return_outs_flag && return_outs_flag_delayed)) {
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
            return_outs_flag_delayed = return_outs_flag;
            if (drive_ins_flag) {
                ptrs.top->io_cmd_valid = 1;
                ptrs.top->io_resp_ready= 1;
                ptrs.top->io_cmd_bits_inst_rd = inputs.io_cmd_bits_inst_rd;
                ptrs.top->io_cmd_bits_rs1 = inputs.io_cmd_bits_rs1;
                ptrs.top->io_cmd_bits_rs2 = inputs.io_cmd_bits_rs2;
                drive_ins_flag = 0;
            } else {
                if (ptrs.top->io_cmd_ready && ptrs.top->io_cmd_valid && !drive_ins_flag) {
                    ptrs.top->io_cmd_valid = 0;
                }
                if (ptrs.top->io_resp_ready && ptrs.top->io_resp_valid && return_outs_flag) {
                    ptrs.top->io_resp_ready = 0;
                }
                if (ptrs.top->io_resp_ready && ptrs.top->io_resp_valid) {
                    outputs.io_resp_bits_data = ptrs.top->io_resp_bits_data;
                    outputs.io_resp_bits_rd = ptrs.top->io_resp_bits_rd;
                    return_outs_flag = 1;
                }
            }
        }

        // Evaluate model
        // (If you have multiple models being simulated in the same
        // timestep then instead of eval(), call eval_step() on each, then
        // eval_end_step() on each. See the manual.)
        ptrs.top->eval();

        // Read outputs
        VL_PRINTF("[%" PRId64 "] clock=%x rst=%x cmd_rd=%x cmd_val=%x rs1=%" PRIx64 " rs2=%" PRIx64
                  " -> data=%" PRIx64 "\n",
                  ptrs.contextp->time(), ptrs.top->clock, ptrs.top->reset, ptrs.top->io_cmd_ready, ptrs.top->io_cmd_valid, ptrs.top->io_cmd_bits_rs1, ptrs.top->io_cmd_bits_rs2, ptrs.top->io_resp_bits_data
                );
    }

    return outputs;
}

ins read_from_in_fifo() {
    ins inputs;
    ssize_t bytes_read = read(in_fd, &inputs, sizeof(inputs));
    if (bytes_read == -1) {
        perror("read_from_in_fifo -> -1");
    }
    if (bytes_read != sizeof(inputs)) {
        perror("read_from_in_fifo -> too small");
    }
    return inputs;
}

int read_from_ctrl_fifo() {
    int ctrl;
    ssize_t bytes_read = read(ctrl_fd, &ctrl, sizeof(ctrl));
    if (bytes_read == -1) {
        perror("read_from_ctrl_fifo -> -1");
    }
    if (bytes_read != sizeof(ctrl)) {
        perror("read_from_ctrl_fifo -> too small");
    }
    return ctrl;
}

int write_to_out_fifo(outs outputs) {
    ssize_t bytes_written = write(out_fd, &outputs, sizeof(outputs));
    if (bytes_written == -1) {
        perror("write_to_fifo -> -1");
        return bytes_written;
    }
    if (bytes_written != sizeof(outputs)) {
        perror("write_to_fifo -> too small");
        return bytes_written;
    }
    return 0;
}

int main(int argc, char** argv) {
    // This is a more complicated example, please also see the simpler examples/make_hello_c.

    if (setup_fifos()) {
        return 1;
    }

    if (open_fifos()) {
        return 1;
    }
    
    auto ptr1 = std::make_unique<VerilatedContext>();
    auto ptr2 = new VAdd{ptr1.get(), "TOP"};

    ptrs.contextp = std::move(ptr1);
    ptrs.top = ptr2;
    
    init(argc, argv);

    while(int_to_control(read_from_ctrl_fifo()) != Control::STOP) {
        ins inputs = read_from_in_fifo();
        outs outputs = step_to_output(inputs);

        write_to_out_fifo(outputs);
    }    

    // ins inputs{5, 1, 2}; 

    // auto [io_resp_bits_rd, io_resp_bits_data] = step_to_output(ptrs, inputs);

    // std::cout<< "Data = " << io_resp_bits_data << std::endl;

    finish();
    
    delete ptrs.top;

}
