import chisel3._

// 発展問題: プログラマブルPWMパルス発生器
class PwmGenerator(val periodMax: Int = 255) extends Module {
  val io = IO(new Bundle {
    val period = Input(UInt(8.W))
    val duty   = Input(UInt(8.W))
    val en     = Input(Bool())
    val pwmOut = Output(Bool())
  })

  val cntReg = RegInit(0.U(8.W))

  // TODO: カウンタと比較器を組み合わせ、デューティ比に応じたPWMパルスを生成してください
  // en 有効時: cntReg は 0 から period までカウントアップし、満了時に 0 へ復帰
  // pwmOut: en && (cntReg < duty) && (duty > 0.U)
  // en 無効時: カウンタ停止、pwmOut は false.B
  io.pwmOut := false.B
}

object PwmGeneratorGenerator extends App {
  emitVerilog(new PwmGenerator(255), Array("--target-dir", "generated"))
}
