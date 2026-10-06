import chisel3._
import chisel3.util._

// 基本問題: 4ビットJohnsonカウンタ
class JohnsonCounter(val width: Int = 4) extends Module {
  val io = IO(new Bundle {
    val en  = Input(Bool())
    val out = Output(UInt(width.W))
  })

  val state = RegInit(0.U(width.W))

  // TODO: 最上位ビットの反転を最下位ビットへ帰還する2N状態巡回を実装してください
  // state := Cat(state(width - 2, 0), ~state(width - 1))
  io.out := state
}

object JohnsonCounterGenerator extends App {
  emitVerilog(new JohnsonCounter(4), Array("--target-dir", "generated"))
}
