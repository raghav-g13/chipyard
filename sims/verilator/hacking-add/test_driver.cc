#include "fifos.h"
#include "driver.h"
#include <iostream>
#include <fcntl.h>

int main(int argc, char** argv) {
    // This is a more complicated example, please also see the simpler examples/make_hello_c.

    if (open_fifos()) {
        return 1;
    }

    for (int i = 0; i < 5; i += 1) {
        if (!i) {
            write_to_ctrl_fifo(Control::START);
        } else {
            write_to_ctrl_fifo(Control::CONTINUE);
        }

        ins inputs{5 + i, 1 + i, 2 + i};
        write_to_in_fifo(inputs);
        outs outputs = read_from_out_fifo();

        // ins inputs{5, 1, 2}; 

        // auto [io_resp_bits_rd, io_resp_bits_data] = step_to_output(ptrs, inputs);

        std::cout<< "Dest Reg = " << outputs.io_resp_bits_rd << std::endl;
        std::cout<< "Data = " << outputs.io_resp_bits_data << std::endl;

    }

    write_to_ctrl_fifo(Control::STOP);
    close_fifos();

    return 0; 

}
