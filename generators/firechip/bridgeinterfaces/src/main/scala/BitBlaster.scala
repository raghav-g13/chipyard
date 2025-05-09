// See LICENSE for license details.

package firechip.bridgeinterfaces

import chisel3._
import chisel3.util._

case class BitBlasterConfig(
    numBits: Int = 256
)

trait HasBitBlasterParameters {
    val bbParams: BitBlasterConfig
}

abstract class BitBlasterBundle extends Bundle with HasBitBlasterParameters

// Defined from the perspective of the target - going out from Target to Host
class BitBlasterOut(val bbParams: BitBlasterConfig) extends BitBlasterBundle { 
    val out = UInt(bbParams.numBits.W)
}

class BitBlasterOutDecoupled(val bbParams: BitBlasterConfig) extends BitBlasterBundle { 
    val out = Decoupled(UInt(bbParams.numBits.W))
}

// Defined from the perspective of the target - coming in from Host to Target
class BitBlasterIn(val bbParams: BitBlasterConfig) extends BitBlasterBundle {
    val in = UInt(bbParams.numBits.W)
}

class BitBlasterInDecoupled(val bbParams: BitBlasterConfig) extends BitBlasterBundle {
    val in = Decoupled(UInt(bbParams.numBits.W))
}

class BitBlasterBridgeDecoupledIO(outParams: BitBlasterConfig, inParams: BitBlasterConfig) extends Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    
    val out = new BitBlasterOutDecoupled(outParams)
    val in = Flipped(new BitBlasterIn(inParams))
}

class BitBlasterBridgeIO(outParams: BitBlasterConfig, inParams: BitBlasterConfig) extends Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    
    val out = Input(new BitBlasterOut(outParams))
    val in = Output(new BitBlasterIn(inParams))
}

case class BitBlasterBridgeKey(inParams: BitBlasterConfig, outParams: BitBlasterConfig)