import chisel3._
import chisel3.util._

// 課題1: 算術ユニット (加算・減算・キャリー出力)
class ArithmeticUnit extends Module {
  val io = IO(new Bundle {
    val a    = Input(UInt(8.W))
    val b    = Input(UInt(8.W))
    val op   = Input(Bool()) // 0 (false.B): ADD, 1 (true.B): SUB
    val res  = Output(UInt(8.W))
    val cout = Output(Bool())
  })

  // TODO: 9ビット幅拡張演算 (+& / -&) を用いて加算・減算とキャリー出力を実装してください
  io.res  := 0.U
  io.cout := false.B
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new ArithmeticUnit,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
