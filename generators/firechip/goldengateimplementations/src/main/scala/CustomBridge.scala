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
    val ccb = key.ccb // TODO: EDIT
    // This creates the interfaces for all of the host-side transport
    // AXI4-lite for the simulation control bus, =
    // AXI4 for DMA
    val io = IO(new WidgetIO())

    // This creates the host-side interface of your TargetIO
    val hPort = IO(HostPort(new CustomBridgeTargetIO(ccb)))

    // Out signals - calculate using chisel3.experimental.DataMirror
    // val outputWidth = w // hPort.hBits.gcdio.out.elements.map { case (_, data) => data.getWidth }.sum

    // Generate some FIFOs to capture tokens...
    val rxfifo = Module(new Queue(new Prefetch(ccb), 10))

    // In signals - calculate using chisel3.experimental.DataMirror
    // val inputWidth = 2 * w // hPort.hBits.gcdio.in.elements.map { case (_, data) => data.getWidth }.sum
    
    // Generate Input FIFO
    val txfifo = Module(new Queue(new Snoop(ccb), 100))

    val target = hPort.hBits.customio
    // In general, your BridgeModule will not need to do work every host-cycle. In simple Bridges,
    // we can do everything in a single host-cycle -- fire captures all of the
    // conditions under which we can consume and input token and produce a new
    // output token
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

    txfifo.io.enq.bits  := target.snoop.bits
    txfifo.io.enq.valid := target.snoop.valid
    temp := target.snoop.valid

    target.request.valid := rxfifo.io.deq.valid
    target.request.bits := rxfifo.io.deq.bits
    rxfifo.io.deq.ready := fire && target.request.ready  // TODO: HUHHHH? target.output_ready?

    target.hit := false.B // TODO: SOME MMIO REG

    // DOC include start: UART Bridge Footer
    // Exposed the head of the queue and the valid bit as a read-only registers
    // with name "out_bits" and out_valid respectively
    // genROReg(txfifo.io.deq.bits.input_valid, "input_valid")
    // genROReg(txfifo.io.deq.bits.blockBytes, "snoop_blockBytes")
    genROReg(txfifo.io.deq.bits.write, "snoop_write")
    genROReg(txfifo.io.deq.bits.address, "snoop_address")
    genROReg(txfifo.io.count, "snoop_fifo_count")
    // genROReg(txfifo.io.deq.bits.block, "snoop_block")
    // genROReg(txfifo.io.deq.bits.block_address, "snoop_block_address")
    // genROReg(txfifo.io.deq.bits.output_ready, "output_ready")
    genROReg(txfifo.io.deq.valid, "in_valid")

    // Generate a writeable register, "out_ready", that when written to dequeues
    // a single element in the tx_fifo. Pulsify derives the register back to false
    // after pulseLength cycles to prevent multiple dequeues
    // INITIALIZE IN_READY TO TRUE
    Pulsify(genWORegInit(txfifo.io.deq.ready, "in_ready", false.B), pulseLength = 1)

    // Generate registers for the rx-side of the UART; this is eseentially the reverse of the above
    // INIT => INPUT READY MUST BE HIGH, OUTPUT VALID MUST BE LOW, BUSY MUST BE LOW
    // genWOReg(rxfifo.io.enq.bits.gcd, "gcd")
    // genWOReg(rxfifo.io.enq.bits.blockBytes, "request_blockBytes")
    genWOReg(rxfifo.io.enq.bits.write, "request_write")
    genWOReg(rxfifo.io.enq.bits.address, "request_address")
    // genWOReg(rxfifo.io.enq.bits.block, "request_block")
    // genWOReg(rxfifo.io.enq.bits.block_address, "request_block_address")

    // rxfifo.io.enq.bits.write := false.B
    // rxfifo.io.enq.bits.address:= 0.U

    
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
