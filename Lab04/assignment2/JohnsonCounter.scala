import chisel3._
import chisel3.util._

// 基本問題: 4ビットJohnsonカウンタ
class JohnsonCounter(val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
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
  val state = RegInit(0.U(width.W))
  val ledReg = RegInit(false.B)

  val enJohnsonCounter = WireDefault(false.B)
  // TODO: 最上位ビットの反転を最下位ビットへ帰還する2N状態巡回を実装してください
  io.out := state
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new JohnsonCounter,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
