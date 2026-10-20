import chisel3._

// 基本問題: ロード・イネーブル・同期クリア付き 任意進数カウンタ
class ModuloNCounter(val n: Int = 10, val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val count    = Output(UInt(width.W))
    val runLED = Output(Bool())
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq

  val cntReg = RegInit(0.U(width.W))

  // TODO: clear -> en の優先度でカウント動作
  // clear: 次クロックで 0 にリセット
  // modEn: 通常カウント (count == n-1 のとき0に復帰)
  io.runLED   := ~ledReg
  io.count    := ~modReg
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
  new ModuloNCounter,
  Array("--target-dir", "generated"),
  Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
)
}