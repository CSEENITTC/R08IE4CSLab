import chisel3._
import chisel3.util._

class TeCIO extends Module {
  val io = IO(new Bundle {
    val wd  = Input(UInt(8.W))
    val op  = Input(Bool())
    val weA = Input(Bool())
    val weB = Input(Bool())
    val rd  = Output(UInt(8.W))
  })

  // TODO: regA, regB を定義し、weA / weB による書き込みと op による選択出力を実装してください
  io.rd := 0.U
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new TeCIO,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
