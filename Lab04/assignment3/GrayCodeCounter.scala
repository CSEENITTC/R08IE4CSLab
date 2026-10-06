import chisel3._

// 発展問題: 4ビット・グレイコード・カウンタ
class GrayCodeCounter extends Module {
  val io = IO(new Bundle {
    val en    = Input(Bool())
    val clear = Input(Bool())
    val gray  = Output(UInt(4.W))
    val bin   = Output(UInt(4.W))
  })

  val binReg = RegInit(0.U(4.W))

  // TODO: バイナリカウンタからグレイコードへの変換論理 (gray = bin ^ (bin >> 1)) を実装してください
  io.gray := 0.U
  io.bin  := binReg
}

object GrayCodeCounterGenerator extends App {
  emitVerilog(new GrayCodeCounter, Array("--target-dir", "generated"))
}
