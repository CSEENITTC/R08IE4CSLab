import chisel3._
import chisel3.util._

class TeCIO extends Module {
  val io = IO(new Bundle {
    val wd  = Input(UInt(8.W))
    val op  = Input(Bool()) // 0 (false.B): 論理左シフト (<<), 1 (true.B): 論理右シフト (>>)
    val weA = Input(Bool())
    val weB = Input(Bool())
    val rd  = Output(UInt(8.W))
  })

  val regA = RegInit(0.U(8.W))
  val regB = RegInit(0.U(3.W)) // シフト量 shamt は 3ビットで宣言

  // TODO: regA (io.wd) と regB (io.wd(2,0)) への書き込み、および ShiftUnit のインスタンス化と負論理出力 (~su.io.res) を実装してください
  io.rd := 0.U
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new TeCIO,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
