#include <cstdint>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include "fifos.h"

const char *in_fifo_path = "/home/raghavgupta/hybrid-sim/chipyard/sims/verilator/hacking-add/in_fifo";
int in_fd; 

const char *out_fifo_path = "/home/raghavgupta/hybrid-sim/chipyard/sims/verilator/hacking-add/out_fifo";
int out_fd;

const char *ctrl_fifo_path = "/home/raghavgupta/hybrid-sim/chipyard/sims/verilator/hacking-add/ctrl_fifo";
int ctrl_fd;


int setup_fifos() {
    // Create FIFOs
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

int delete_fifos(){
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

int control_to_int(Control ctrl) {
    return static_cast<int>(ctrl);
}

Control int_to_control(int ctrl) {
    switch (ctrl) {
        case 0: return Control::START;
        case 1: return Control::CONTINUE;
        case 2: return Control::STOP;
        default: 
            perror("Unknown enum value");
            return Control::INVALID; 
    }
}
