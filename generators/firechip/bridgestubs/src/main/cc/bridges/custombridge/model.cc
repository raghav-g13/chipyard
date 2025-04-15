#include "model.h"

// FIFO file paths
const char *FifoHandler::in_fifo_path = "/home/raghavgupta/hybrid-sim/chipyard/sims/verilator/hacking-add/in_fifo";
const char *FifoHandler::out_fifo_path = "/home/raghavgupta/hybrid-sim/chipyard/sims/verilator/hacking-add/out_fifo";
const char *FifoHandler::ctrl_fifo_path = "/home/raghavgupta/hybrid-sim/chipyard/sims/verilator/hacking-add/ctrl_fifo";

// Constructor
FifoHandler::FifoHandler() : in_fd(-1), out_fd(-1), ctrl_fd(-1) {}

// Destructor
FifoHandler::~FifoHandler() {
    closeFifos();
}

// Function to setup FIFOs
int FifoHandler::setupFifos() {
    if (mkfifo(in_fifo_path, 0666) == -1) {
        perror("mkfifo");
        return 1;
    }
    if (mkfifo(out_fifo_path, 0666) == -1) {
        perror("mkfifo");
        return 1;
    }
    if (mkfifo(ctrl_fifo_path, 0666) == -1) {
        perror("mkfifo");
        return 1;
    }
    return 0;
}

// Function to delete FIFOs
int FifoHandler::deleteFifos() {
    if (unlink(in_fifo_path) == -1) {
        perror("unlink failed");
        return 1;
    }
    if (unlink(out_fifo_path) == -1) {
        perror("unlink failed");
        return 1;
    }
    if (unlink(ctrl_fifo_path) == -1) {
        perror("unlink failed");
        return 1;
    }
    return 0;
}

// Open FIFOs
int FifoHandler::openFifos() {
    in_fd = open(in_fifo_path, O_WRONLY);
    if (in_fd == -1) {
        perror("open");
        return 1;
    }

    out_fd = open(out_fifo_path, O_RDONLY);
    if (out_fd == -1) {
        perror("open");
        return 1;
    }
    
    ctrl_fd = open(ctrl_fifo_path, O_WRONLY);
    if (ctrl_fd == -1) {
        perror("open");
        return 1;
    }

    return 0;
}

// Close FIFOs
int FifoHandler::closeFifos() {
    if (in_fd != -1) close(in_fd);
    if (out_fd != -1) close(out_fd);
    if (ctrl_fd != -1) close(ctrl_fd);
    return 0;
}

// Read from the output FIFO
outs FifoHandler::readFromOutFifo() {
    outs outputs;
    ssize_t bytes_read = read(out_fd, &outputs, sizeof(outputs));
    if (bytes_read == -1) {
        perror("read_from_fifo -> -1");
    }
    if (bytes_read != sizeof(outputs)) {
        perror("read_from_fifo -> too small");
    }
    return outputs;
}

// Write to the input FIFO
int FifoHandler::writeToInFifo(ins inputs) {
    ssize_t bytes_written = write(in_fd, &inputs, sizeof(inputs));
    if (bytes_written == -1) {
        perror("write_to_fifo -> -1");
        return bytes_written;
    }
    if (bytes_written != sizeof(inputs)) {
        perror("write_to_fifo -> too small");
        return bytes_written;
    }
    return 0;
}

// Write to the control FIFO
int FifoHandler::writeToCtrlFifo(Control ctrl) {
    int ctrl_int = controlToInt(ctrl);
    ssize_t bytes_written = write(ctrl_fd, &ctrl_int, sizeof(ctrl_int));
    if (bytes_written == -1) {
        perror("write_to_fifo -> -1");
        return bytes_written;
    }
    if (bytes_written != sizeof(ctrl_int)) {
        perror("write_to_fifo -> too small");
        return bytes_written;
    }
    return 0;
}

// Convert control enum to int
int FifoHandler::controlToInt(Control ctrl) {
    return static_cast<int>(ctrl);
}

// Convert int to control enum
Control FifoHandler::intToControl(int ctrl) {
    switch (ctrl) {
        case 0: return Control::START;
        case 1: return Control::CONTINUE;
        case 2: return Control::STOP;
        default: 
            perror("Unknown enum value");
            return Control::INVALID;
    }
}
