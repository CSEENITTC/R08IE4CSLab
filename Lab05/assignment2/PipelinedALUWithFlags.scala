import chisel3._
import chisel3.util._

// 発展問題: フラグ生成 & パイプライン化ALU
class PipelinedALUWithFlags extends Module {
  val io = IO(new Bundle {
    val a        = Input(UInt(8.W))
    val b        = Input(UInt(8.W))
    val aluSel   = Input(UInt(3.W))
    val validIn  = Input(Bool())
    val out      = Output(UInt(8.W))
    val zero     = Output(Bool())
    val negative = Output(Bool())
    val carry    = Output(Bool())
    val validOut = Output(Bool())
  })

  // サブモジュール1: 算術ユニット
  val arithUnit = Module(new Module {
    val io = IO(new Bundle {
      val a = Input(UInt(8.W)); val b = Input(UInt(8.W)); val op = Input(Bool())
      val res = Output(UInt(8.W)); val cout = Output(Bool())
    })
    val ext = Wire(UInt(9.W))
    ext := Mux(!io.op, io.a +& io.b, io.a -& io.b)
    io.res := ext(7, 0)
    io.cout := ext(8)
  })

  // サブモジュール2: 論理ユニット
  val logicUnit = Module(new Module {
    val io = IO(new Bundle {
      val a = Input(UInt(8.W)); val b = Input(UInt(8.W)); val op = Input(UInt(2.W))
      val res = Output(UInt(8.W))
    })
    val outW = WireDefault(0.U(8.W))
    switch(io.op) {
      is(0.U) { outW := io.a & io.b }
      is(1.U) { outW := io.a | io.b }
      is(2.U) { outW := io.a ^ io.b }
      is(3.U) { outW := ~io.a }
    }
    io.res := outW
  })

  arithUnit.io.a := io.a; arithUnit.io.b := io.b; arithUnit.io.op := io.aluSel(0)
  logicUnit.io.a := io.a; logicUnit.io.b := io.b; logicUnit.io.op := io.aluSel(1, 0)

  val rawOut = Mux(io.aluSel(2) === 0.U, arithUnit.io.res, logicUnit.io.res)
  val rawCout = Mux(io.aluSel(2) === 0.U, arithUnit.io.cout, false.B)

  // TODO: 出力段にパイプラインレジスタと各種ステータスフラグ (zero, negative, carry, validOut) を実装してください
  io.out      := 0.U
  io.zero     := false.B
  io.negative := false.B
  io.carry    := false.B
  io.validOut := false.B
}

object PipelinedALUWithFlagsGenerator extends App {
  emitVerilog(new PipelinedALUWithFlags, Array("--target-dir", "generated"))
}
