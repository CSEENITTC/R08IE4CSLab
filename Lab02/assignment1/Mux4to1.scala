import chisel3._

// 基本問題1: 4-to-1 マルチプレクサ
class Mux4to1 extends Module {
  val io = IO(new Bundle {
    val in0 = Input(UInt(8.W))
    val in1 = Input(UInt(8.W))
    val in2 = Input(UInt(8.W))
    val in3 = Input(UInt(8.W))
    val sel = Input(UInt(2.W))
    val out = Output(UInt(8.W))
  })

  // TODO: sel (0〜3) の値に応じて in0〜in3 を選択出力してください
  io.out := 0.U
}

object Mux4to1Generator extends App {
  emitVerilog(new Mux4to1, Array("--target-dir", "generated"))
}
