import chisel3._
import chisel3.util._

// 課題2: 論理ユニット (AND, OR, XOR, NOT)
class LogicUnit extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(8.W))
    val b   = Input(UInt(8.W))
    val op  = Input(UInt(2.W)) // 0: AND, 1: OR, 2: XOR, 3: NOT a
    val res = Output(UInt(8.W))
  })

  // TODO: WireDefault と switch/is を用いて4種類のビット論理演算を実装してください
  io.res := 0.U
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new LogicUnit,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
