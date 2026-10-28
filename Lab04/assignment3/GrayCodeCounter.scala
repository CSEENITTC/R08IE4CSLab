import chisel3._

// 発展問題: 4ビット・グレイコード・カウンタ
class GrayCodeCounter(val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en    = Input(Bool())
    val clear = Input(Bool())
    val runLED = Output(Bool())
    val gray  = Output(UInt(4.W))
    val bin   = Output(UInt(4.W))
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth = log2Ceil(clkFreq)
  val cntReg = RegInit(0.U(cntWidth.W))
  val binReg = RegInit(0.U(width.W))
  val ledReg = RegInit(false.B)

  val enGrayCodeCounter = WireDefault(false.B)

  // TODO: バイナリカウンタからグレイコードへの変換論理 (gray = bin ^ (bin >> 1)) を実装してください
  io.gray := 0.U
  io.bin  := binReg
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new GrayCodeCounter,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
