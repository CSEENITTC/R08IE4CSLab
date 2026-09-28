import chisel3._

// ============================================================================
// 第1回: 組合せ回路の基礎 (Combinational Logic)
// ============================================================================

// 基本問題1: 1ビット半加算器 (Half Adder)
class FullAdder extends Module {
  val io = IO(new Bundle {
    val a    = Input(Bool())
    val b    = Input(Bool())
    val cin  = Input(Bool())
    val sum  = Output(Bool())
    val cout = Output(Bool())
  })

  // TODO: sum と cout の論理式を記述してください
  io.sum  := io.a ^ io.b ^ io.cin
  io.cout := io.a & io.b | (io.a | io.b) & io.cin
}
object VerilogGenerator extends App {
    emitVerilog(new FullAdder, Array("--target-dir", "generated"))
}
