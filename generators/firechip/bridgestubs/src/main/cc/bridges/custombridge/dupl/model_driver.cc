#include "model_driver.h"
#include <iostream>
#include <fcntl.h>

const char *in_fifo_path = "/home/raghavgupta/hybrid-sim/chipyard/sims/verilator/hacking-add/in_fifo";
int in_fd; 

const char *out_fifo_path = "/home/raghavgupta/hybrid-sim/chipyard/sims/verilator/hacking-add/out_fifo";
int out_fd;

const char *ctrl_fifo_path = "/home/raghavgupta/hybrid-sim/chipyard/sims/verilator/hacking-add/ctrl_fifo";
int ctrl_fd;

int open_fifos() {
    // Create FDs
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

outs read_from_out_fifo() {
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

int write_to_ctrl_fifo(Control ctrl) {
    int ctrl_int = control_to_int(ctrl);
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

int write_to_in_fifo(ins inputs) {
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

int close_fifos() {
    // Close FDs
    close(in_fd);
    close(out_fd);
    close(ctrl_fd);
    return 0;
}
