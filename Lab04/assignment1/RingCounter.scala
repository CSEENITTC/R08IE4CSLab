import chisel3._
import chisel3.util._

// 基本問題: 4ビットRingカウンタ
class RingCounter(val width: Int = 4) extends Module {
  val io = IO(new Bundle {
    val en  = Input(Bool())
    val out = Output(UInt(width.W))
  })

  val state = RegInit(1.U(width.W))

  // TODO: 下位ビットから上位ビットへ移動し、最上位ビットを最下位ビットへ帰還する1-hot循環シフトを実装してください
  io.out := state
}

object VerilogGenerator extends App {
  _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
    new RingCounter,
    Array("--target-dir", "generated"),
    Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
}
