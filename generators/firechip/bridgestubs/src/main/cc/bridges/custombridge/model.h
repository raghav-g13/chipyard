#ifndef __MODEL_SDFGH_H
#define __MODEL_SDFGH_H

#include <cstdint>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <iostream>
#include <fcntl.h>

// Structure for input data
struct ins {
    uint64_t io_cmd_bits_inst_rd;
    uint64_t io_cmd_bits_rs1;
    uint64_t io_cmd_bits_rs2;
};

// Structure for output data
struct outs {
    uint64_t io_resp_bits_rd;
    uint64_t io_resp_bits_data;
};

// Enum class for controlling the state
enum class Control {
    START,
    CONTINUE,
    STOP,
    INVALID = -1
};

// Class to handle FIFO operations
class FifoHandler {
public:
    // FIFO file paths
    static const char *in_fifo_path;
    static const char *out_fifo_path;
    static const char *ctrl_fifo_path;

    // File descriptors for FIFOs
    int in_fd;
    int out_fd;
    int ctrl_fd;

    // Constructor and Destructor
    FifoHandler();
    ~FifoHandler();

    // Function to setup FIFOs
    int setupFifos();

    // Function to delete FIFOs
    int deleteFifos();

    // Open FIFOs
    int openFifos();

    // Close FIFOs
    int closeFifos();

    // Read from the output FIFO
    outs readFromOutFifo();

    // Write to the input FIFO
    int writeToInFifo(ins inputs);

    // Write to the control FIFO
    int writeToCtrlFifo(Control ctrl);

    // Convert control enum to int
    static int controlToInt(Control ctrl);

    // Convert int to control enum
    static Control intToControl(int ctrl);
};

#endif // __MODEL_H
