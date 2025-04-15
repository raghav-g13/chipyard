#ifndef __VERILATOR_MODEL_DRIVER_H
#define __VERILATOR_MODEL_DRIVER_H

#include "fifos.h"
#include <iostream>
#include <fcntl.h>

outs read_from_out_fifo();
int write_to_in_fifo(ins inputs);
int write_to_ctrl_fifo(Control ctrl);
int open_fifos();
int close_fifos();

#endif //__VERILATOR_MODEL_DRIVER_H 