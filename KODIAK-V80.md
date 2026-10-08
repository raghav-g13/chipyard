# Running Kodiak on the Alveo V80 with FireSim

This guide gets you from a fresh machine to running the Kodiak test workloads
on a Xilinx Alveo V80 FPGA. Steps the upstream FireSim V80 guide already
covers are linked.

Upstream V80 guide, referred to below as **[V80 guide]**:
https://docs.fires.im/en/main/Getting-Started-Guides/On-Premises-FPGA-Getting-Started/Xilinx-Alveo-V80-FPGAs.html
(source: `sims/firesim/docs/Getting-Started-Guides/On-Premises-FPGA-Getting-Started/`)

## What's on this branch

| Path | What it is |
|---|---|
| `generators/chipyard/src/main/scala/config/KodiakConfigs.scala` | `KodiakFireSimConfig`: the Kodiak SoC |
| `generators/firechip/chip/src/main/scala/TargetConfigs.scala` | `FireSimKodiakConfig`: Kodiak plus FireSim bridges, 500 MHz buses |
| `sims/firesim-staging/kodiak_config_build_recipes.yaml` | V80 build recipe (`xilinx_alveo_v80_firesim_kodiak_no_nic_l2_llc4mb_ddr3`, 10 MHz, BASIC) |
| `sims/firesim-staging/kodiak_config_hwdb.yaml` | HWDB entry pointing at the prebuilt bitstream |
| `software/kodiak-workloads/*.riscv` | 20 prebuilt bare-metal test binaries |
| `sims/firesim/deploy/workloads/kodiak-*.json` | FireSim workload definitions for those binaries |
| `sims/firesim/deploy/workloads/run-kodiak-workloads.sh` | Runs any subset of the workloads and reports PASS/FAIL |

## 1. One-time FPGA and host setup

Follow these sections of the [V80 guide] as written:

1. **Initial Setup**: installing the V80, writing the FireSim image to flash,
   and machines with other FireSim FPGAs.
2. **Repo Setup**: everything up to "Setting up the FireSim Repo".

## 2. Repo setup (differs from the V80 guide)

In "Setting up the FireSim Repo", use the Kodiak branch instead of upstream
Chipyard:

```bash
git clone https://github.com/raghav-g13/chipyard
cd chipyard
git checkout v80-kodiak-handoff
./build-setup.sh -s 9
```

`-s 9` skips precompiling the default buildroot Linux, which the bare-metal
Kodiak workloads don't need. Don't skip step 10 (CIRCT): `firesim infrasetup`
builds the simulation driver locally, and that needs `firtool`.

**Reinstall the FireSim sudo scripts**, even if this machine already has them.
They changed for V80 support, so copies from an older FireSim install won't
work. Follow step 1 of the `sudo` setup in
[FireSim's local FPGA setup](https://docs.fires.im/en/main/Local-FPGA-Initial-Setup.html),
but copy the scripts from this branch's FireSim instead of a temporary clone:

```bash
cd sims/firesim
sudo cp deploy/sudo-scripts/* /usr/local/bin
sudo cp platforms/xilinx_alveo_u250/scripts/* /usr/local/bin
sudo cp platforms/xilinx_alveo_v80/scripts/firesim-v80-change-pcie-perms /usr/local/bin
sudo chmod 755 /usr/local/bin/firesim*
sudo chgrp firesim /usr/local/bin/firesim*
cd ../..
```

Then rerun `firesim enumeratefpgas` once the manager is configured (below), so
`/opt/firesim-db.json` lists the V80.

This branch fetches FireSim from `raghav-g13/firesim` (branch
`v80-kodiak-handoff`), which adds the Kodiak workloads and runner script.

Then continue with "Initializing FireSim Config Files" and "Configuring the
FireSim manager" from the [V80 guide] unchanged.

## 3. Get the Kodiak bitstream

`sims/firesim-staging/kodiak_config_hwdb.yaml` points at a prebuilt bitstream
(10 MHz FPGA clock, BASIC build strategy) that passes all 20 workloads below:

https://bitstreams.ucb.bar/xilinx_alveo_v80/xilinx_alveo_v80_firesim_kodiak_no_nic_l2_llc4mb_ddr3.tar.gz

FireSim downloads it during `firesim infrasetup`, so there's nothing to fetch
by hand.

In `sims/firesim/deploy/config_runtime.yaml`, set:

```yaml
target_config:
    default_hw_config: xilinx_alveo_v80_firesim_kodiak_no_nic_l2_llc4mb_ddr3
```

Pass the Kodiak HWDB to every `firesim` command with
`-a ${CY_DIR}/sims/firesim-staging/kodiak_config_hwdb.yaml`.

## 4. Run one workload

Follow "Running a Single Node Simulation" in the [V80 guide] with two changes:

- Use the `default_hw_config` above instead of the Rocket one.
- Set `workload_name: kodiak-hello.json` instead of `br-base-uniform.json`.

A passing run ends with `*** PASSED ***` in the uartlog under
`deploy/results-workload/<timestamp>-kodiak-hello/`.

## 5. Run the test set

```bash
cd ${CY_DIR}/sims/firesim/deploy
./workloads/run-kodiak-workloads.sh -- -a ${CY_DIR}/sims/firesim-staging/kodiak_config_hwdb.yaml
```

- Pass workload names (without the `kodiak-` prefix) to run a subset, e.g.
  `./workloads/run-kodiak-workloads.sh hello vec-sgemm -- -a ...`.
- `-t SECONDS` sets the per-step timeout. The default is 1200.
- Results go to `deploy/results-kodiak/<timestamp>/`: `summary.csv` plus each
  workload's uartlog and manager logs.
- The workload is chosen with `-x`, so `config_runtime.yaml` is never edited.
  Everything else (run farm, hardware config) comes from that file.

### Workloads

All 20 pass on the prebuilt bitstream. Expect these numbers: the runner
reports the simulator's total emulated target cycles, which are the same on
every run of the same bitstream. Wallclock excludes the few minutes each
workload spends programming the FPGA.

| Workload | What it exercises | Cycles | Wallclock (s) |
|---|---|---|---|
| `hello` | Boot and UART printf on one core | 78,185,837 | 7.8 |
| `mt-hello` | Boot on all harts | 112,266,842 | 11.2 |
| `hart1-add` | Compute on a secondary hart | 78,185,837 | 7.8 |
| `tcm` | Shuttle tightly-coupled memory reads and writes | 76,181,072 | 7.6 |
| `scalar-mp-add` | Scalar integer add kernel | 76,181,072 | 7.6 |
| `fp32_macc` | Scalar FP32 multiply-accumulate | 148,352,612 | 14.8 |
| `scalar-gemm-cos` | Scalar FP GEMM | 116,276,372 | 11.7 |
| `vec-sgemm` | Saturn FP32 GEMM (71x71x71) | 78,185,837 | 7.8 |
| `vec-sgemv` | Saturn FP32 GEMV (128x128) | 78,185,837 | 7.8 |
| `vec-igemm-utilization` | Saturn int8 to int32 GEMM | 144,343,082 | 14.4 |
| `vec-daxpy` | Saturn FP64 AXPY | 112,266,842 | 11.4 |
| `vec-mt-sgemm-v3` | Multi-hart vector GEMM | 78,185,837 | 7.8 |
| `vec-sgemm-tcm` | Vector GEMM with operands in TCM | 180,428,852 | 18.0 |
| `vec-sep-conv-3` | Separable 3x3 2D convolution | 112,266,842 | 11.3 |
| `vec-spmv` | Sparse matrix-vector product (indexed loads) | 340,810,052 | 34.1 |
| `vec-fft` | FFT | 46,109,597 | 4.6 |
| `vec-softmax` | Softmax (exp and reductions) | 310,738,577 | 31.1 |
| `vec-strlen` | strlen (fault-only-first loads) | 112,266,842 | 11.2 |
| `vec-transpose-load` | Segmented loads | 44,104,832 | 4.4 |
| `vec-mixed_width_mask` | Mixed-width masked operations | 44,104,832 | 4.4 |

A full run of all 20 takes about an hour, almost all of it FPGA programming.

## 6. Building the bitstream yourself (optional)

Follow "Building a FireSim Bitstream" in the [V80 guide], using the Kodiak
recipe file:

```bash
firesim buildbitstream -r ${CY_DIR}/sims/firesim-staging/kodiak_config_build_recipes.yaml
```

with `builds_to_run` in `config_build.yaml` set to
`xilinx_alveo_v80_firesim_kodiak_no_nic_l2_llc4mb_ddr3`. The recipe uses the
BASIC strategy and a 10 MHz FPGA clock; a build takes about 2.5 hours.

Keep those settings. We are still debugging Kodiak bitstreams built at 50 MHz (BASIC or
RUNTIME_OPTIMIZED) or at 10 MHz with RUNTIME_OPTIMIZED which meet timing but show erroneous behavior.

## 7. Adding workloads

The binaries in `software/kodiak-workloads/` are prebuilt, and their sources
aren't on this branch, so they can't be rebuilt from here.

To add your own workload, put the binary in `software/kodiak-workloads/` and
copy any `deploy/workloads/kodiak-*.json`, changing `benchmark_name` and
`common_bootbinary`.

## 8. Troubleshooting

- **Only one core runs my program.** Harts 0 and 1 are the Shuttle+Saturn
  cores, and the Rocket core is hart 2. A bare-metal startup that parks every
  hart with `mhartid >= 1` runs the whole program on Shuttle 0, and the Rocket
  core never runs.
- **A workload times out.** Larger kernels can take much longer than the 20 mins
  here; some have run for over 3 hours at 10 MHz. Raise the runner's timeout
  with `-t SECONDS`.
- **infrasetup fails with "01 not visible" or "Unable to obtain Extended
  Device BDF".** After many reprograms in a row (it happened twice in about 40
  programs), the V80 can fail to come back on PCIe after programming. Check
  with `lspci -nn -d 10ee:`. A V80 running a FireSim bitstream shows class
  `[0580]`; other FireSim FPGAs in the same host can share the `10ee:903f` ID.
  To recover, reprogram the V80 over JTAG (as `firesim infrasetup` does, or
  with Vivado's hardware manager), then run
  `echo 1 | sudo tee /sys/bus/pci/rescan`. If it still doesn't appear, warm reboot
  the host. Then rerun the failed workloads by name.
- **A simulation hangs.** Run `firesim kill` to free the FPGA. The runner
  script does this automatically after any non-PASS result.
- **infrasetup fails with "No matching hw_devices were found".** The FPGA
  database was generated before the V80 support was merged, so it doesn't name
  the V80's JTAG device (`xcv80_1`). Reinstall the scripts from
  `sims/firesim/platforms/xilinx_alveo_v80/scripts/` into `/usr/local/bin` and
  rerun `firesim enumeratefpgas`, as in the [V80 guide]'s Repo Setup.
