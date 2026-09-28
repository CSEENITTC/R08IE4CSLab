import chisel3._
import chisel3.util._

class AdderWithParity4 extends Module {
  val io = IO(new Bundle {
    val a      = Input(UInt(4.W))
    val b      = Input(UInt(4.W))
    val cin    = Input(UInt(1.W))
    val sum    = Output(UInt(4.W))
    val cout   = Output(Bool())
    val parity = Output(Bool()) // sum の奇数パリティ (1の数が奇数ならtrue)
  })

  // TODO: +& 演算子による拡張加算、ビットスライス、縮約XOR (xorR) を用いて実装してください
  // ※ サブモジュールは使用せず、単一モジュール内で直接記述してください
  val result = io.a +& io.b + io.cin
  io.sum    := result(3,0)
  io.cout   := result(4)
  io.parity := result(3,0).xorR
}
object VerilogGenerator extends App {
    emitVerilog(new AdderWithParity4, Array("--target-dir", "generated"))
}
