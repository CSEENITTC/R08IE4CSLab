import chisel3._
import chisel3.util._

class TeCIO extends Module {
  val io = IO(new Bundle {
    val wd   = Input(UInt(8.W))
    val op   = Input(Bool()) // 0 (false.B): ADD, 1 (true.B): SUB
    val weA  = Input(Bool())
    val weB  = Input(Bool())
    val rd   = Output(UInt(8.W))
    val cout = Output(Bool())
  })

  val regA = RegInit(0.U(8.W))
  val regB = RegInit(0.U(8.W))

  // TODO: regA, regB への書き込みと、ArithmeticUnit のインスタンス化および負論理出力 (~au.io.res, ~au.io.cout) を実装してください
  io.rd   := 0.U
  io.cout := false.B
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new TeCIO,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
