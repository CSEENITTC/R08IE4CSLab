import chisel3._
import chisel3.util._

// 基本問題: 4ビットRingカウンタ
class RingCounter(val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en  = Input(Bool())
    val clear  = Input(Bool())
    val runLED = Output(Bool())
    val out = Output(UInt(width.W))
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth = log2Ceil(clkFreq)
  val cntReg = RegInit(0.U(cntWidth.W))
  val state = RegInit(1.U(width.W))
  val ledReg = RegInit(false.B)

  val enRingCounter = WireDefault(false.B)

  io.out := state
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new RingCounter,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
