// 1秒パルスを生成するサブモジュール
import chisel3._
import chisel3.util.log2Ceil

class OneHzGenerator(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en         = Input(Bool())
    val clear      = Input(Bool())
    val ledBlink   = Output(Bool())
    val oneHzPulse = Output(Bool())
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth   = log2Ceil(fullPeriod)

  val cntReg = RegInit(0.U(cntWidth.W))
  val ledReg = RegInit(false.B)

  when(io.clear) {
    cntReg := 0.U
    ledReg := false.B
  }.elsewhen(io.en) {
    when(cntReg === (fullPeriod - 1).U) {
      cntReg := 0.U
      ledReg := !ledReg
    }.elsewhen(cntReg === (halfPeriod - 1).U) {
      cntReg := cntReg + 1.U
      ledReg := !ledReg
    }.otherwise {
      cntReg := cntReg + 1.U
    }
  }

  io.ledBlink   := ledReg
  io.oneHzPulse := io.en && (cntReg === (fullPeriod - 1).U)
}
