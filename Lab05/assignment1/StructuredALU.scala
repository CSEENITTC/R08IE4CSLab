import chisel3._
import chisel3.util._

// サブモジュール1: 算術ユニット
class ArithmeticUnit extends Module {
  val io = IO(new Bundle {
    val a    = Input(UInt(8.W))
    val b    = Input(UInt(8.W))
    val op   = Input(Bool()) // 0: ADD, 1: SUB
    val res  = Output(UInt(8.W))
    val cout = Output(Bool())
  })

  // TODO: 加算・減算を実装してください
  io.res  := 0.U
  io.cout := false.B
}

// サブモジュール2: 論理ユニット
class LogicUnit extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(8.W))
    val b   = Input(UInt(8.W))
    val op  = Input(UInt(2.W)) // 0: AND, 1: OR, 2: XOR, 3: NOT a
    val res = Output(UInt(8.W))
  })

  // TODO: 4種類のビット論理演算を実装してください
  io.res := 0.U
}

// トップモジュール: 構造化ALU
class StructuredALU extends Module {
  val io = IO(new Bundle {
    val a        = Input(UInt(8.W))
    val b        = Input(UInt(8.W))
    val aluSel   = Input(UInt(3.W)) // bit[2]: 0=算術, 1=論理
    val out      = Output(UInt(8.W))
    val carryOut = Output(Bool())
  })

  // TODO: ArithmeticUnit と LogicUnit をインスタンス化し、階層接続してください
  io.out      := 0.U
  io.carryOut := false.B
}

object StructuredALUGenerator extends App {
  emitVerilog(new StructuredALU, Array("--target-dir", "generated"))
}
