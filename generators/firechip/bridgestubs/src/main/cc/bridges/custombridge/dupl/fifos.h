#ifndef __FIFOS_H
#define __FIFOS_H

#include <cstdint>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>

struct ins {
    uint64_t io_cmd_bits_inst_rd;
    uint64_t io_cmd_bits_rs1;
    uint64_t io_cmd_bits_rs2;
};

struct outs {
    uint64_t io_resp_bits_rd;
    uint64_t io_resp_bits_data;
};

enum class Control {
    START,
    CONTINUE,
    STOP,
    INVALID = -1
};

Control int_to_control(int ctrl);
int control_to_int(Control ctrl);

int setup_fifos();
extern int open_fifos();
extern int close_fifos();
int delete_fifos();

#endif // __FIFOS_H