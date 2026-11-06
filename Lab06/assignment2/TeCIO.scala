import chisel3._
import chisel3.util._

class TeCIO extends Module {
  val io = IO(new Bundle {
    val wd  = Input(UInt(8.W))
    val op  = Input(UInt(2.W))
    val weA = Input(Bool())
    val weB = Input(Bool())
    val res = Output(UInt(8.W))
  })

  // TODO: regA, regB への書き込みと、LogicUnit のインスタンス化および負論理出力 (~lu.io.res) を実装してください
  io.res := 0.U
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new TeCIO,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
