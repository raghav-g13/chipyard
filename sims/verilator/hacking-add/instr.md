## driver.cpp

Basically how you want to write the FireSim bridgestub.

To compile,
`gcc -lstdc++ -o driver -g driver.cpp`

## sim_pipe.cpp

This hosts the sim driver for the Verilated model - handwritten - likely can be generalized to RoCC interface. 

Think about mem model.

To compile,
`verilator --cc --exe --build -j --trace -Wall Add.sv sim_pipe.cpp`