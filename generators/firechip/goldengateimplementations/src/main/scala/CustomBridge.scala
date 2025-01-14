// See LICENSE for license details

package firechip.goldengateimplementations

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters

import midas.widgets._
import firesim.lib.bridgeutils._

import firechip.bridgeinterfaces._

//Note: This file is heavily commented as it serves as a bridge walkthrough
//example in the FireSim docs

// DOC include start: UART Bridge Header
// Our UARTBridgeModule definition, note:
// 1) it takes one parameter, key, of type UARTKey --> the same case class we captured from the target-side
// 2) It accepts one implicit parameter of type Parameters
// 3) It extends BridgeModule passing the type of the HostInterface
//
// While the Scala type system will check if you parameterized BridgeModule
// correctly, the types of the constructor arugument (in this case UARTKey),
// don't match, you'll only find out later when Golden Gate attempts to generate your module.
class CustomBridgeModule(key: CustomKey)(implicit p: Parameters) extends BridgeModule[HostPortIO[CustomBridgeTargetIO]]()(p) {
  lazy val module = new BridgeModuleImp(this) {
    val params = key.params
    // This creates the interfaces for all of the host-side transport
    // AXI4-lite for the simulation control bus, =
    // AXI4 for DMA
    val io = IO(new WidgetIO())

    // This creates the host-side interface of your TargetIO
    val hPort = IO(HostPort(new CustomBridgeTargetIO(params)))

    // Out signals - calculate using chisel3.experimental.DataMirror
    // val outputWidth = w // hPort.hBits.gcdio.out.elements.map { case (_, data) => data.getWidth }.sum

    // Generate some FIFOs to capture tokens...
    val rxfifo = Module(new Queue(new RoCCResp(params), 1))

    // In signals - calculate using chisel3.experimental.DataMirror
    // val inputWidth = 2 * w // hPort.hBits.gcdio.in.elements.map { case (_, data) => data.getWidth }.sum
    
    // Generate Input FIFO
    val txfifo = Module(new Queue(new RoCCCmd(params), 1))

    val target = hPort.hBits.roccio
    // In general, your BridgeModule will not need to do work every host-cycle. In simple Bridges,
    // we can do everything in a single host-cycle -- fire captures all of the
    // conditions under which we can consume and input token and produce a new
    // output token
    // TODO: Consider receive-side logic as well
    val fire = hPort.toHost.hValid && // We have a valid input token: toHost ~= leaving the transformed RTL
               hPort.fromHost.hReady && // We have space to enqueue a new output token
               txfifo.io.enq.ready      // We have space to capture new TX data
    val targetReset = fire & hPort.hBits.reset
    rxfifo.reset := reset.asBool || targetReset
    txfifo.reset := reset.asBool || targetReset

    hPort.toHost.hReady := fire
    hPort.fromHost.hValid := fire
    // DOC include end: UART Bridge Header

    // Set up connections

    val temp = RegInit(false.B)

    txfifo.io.enq.bits := target.rocccmd.bits 
    
    txfifo.io.enq.valid := target.rocccmd.valid
    temp := target.rocccmd.valid
    target.rocccmd.ready := txfifo.io.enq.ready && ~temp

    target.roccresp.valid := rxfifo.io.deq.valid
    target.roccresp.bits := rxfifo.io.deq.bits
    rxfifo.io.deq.ready := fire && target.roccresp.ready  // TODO: HUHHHH? target.output_ready?

    // DOC include start: UART Bridge Footer
    // Exposed the head of the queue and the valid bit as a read-only registers
    // with name "out_bits" and out_valid respectively
    // genROReg(txfifo.io.deq.bits.input_valid, "input_valid")
    genROReg(txfifo.io.deq.bits.inst.asUInt, "inst")
    genROReg(txfifo.io.deq.bits.rs1(31, 0), "rs1_0")
    genROReg(txfifo.io.deq.bits.rs1(63, 32), "rs1_1")
    genROReg(txfifo.io.deq.bits.rs2(31, 0), "rs2_0")
    genROReg(txfifo.io.deq.bits.rs2(63, 32), "rs2_1")
    // genROReg(txfifo.io.deq.bits.output_ready, "output_ready")
    genROReg(txfifo.io.deq.valid, "in_valid")

    // Generate a writeable register, "out_ready", that when written to dequeues
    // a single element in the tx_fifo. Pulsify derives the register back to false
    // after pulseLength cycles to prevent multiple dequeues
    // INITIALIZE IN_READY TO TRUE
    Pulsify(genWORegInit(txfifo.io.deq.ready, "in_ready", false.B), pulseLength = 1)

    // Generate registers for the rx-side of the UART; this is eseentially the reverse of the above
    // INIT => INPUT READY MUST BE HIGH, OUTPUT VALID MUST BE LOW, BUSY MUST BE LOW
    //genWOReg(rxfifo.io.enq.bits.input_ready, "input_ready")     
    //genWOReg(rxfifo.io.enq.bits.output_valid, "output_valid")
    genWOReg(rxfifo.io.enq.bits.rd, "rd")

    val data_lower = Wire(UInt(32.W))
    val data_upper = Wire(UInt(32.W))
    rxfifo.io.enq.bits.data := Cat(data_upper, data_lower)

    genWOReg(data_lower, "data_0")
    genWOReg(data_upper, "data_1")
    //genWOReg(rxfifo.io.enq.bits.busy, "busy")
    
    Pulsify(genWORegInit(rxfifo.io.enq.valid, "out_valid", false.B), pulseLength = 1)
    genROReg(rxfifo.io.enq.ready, "out_ready") // TODO: USE!

    // This method invocation is required to wire up all of the MMIO registers to
    // the simulation control bus (AXI4-lite)
    genCRFile()
    // DOC include end: UART Bridge Footer

    override def genHeader(base: BigInt, memoryRegions: Map[String, BigInt], sb: StringBuilder): Unit = {
      genConstructor(base, sb, "custombridge_t", "custombridge")
    }
  }
}
