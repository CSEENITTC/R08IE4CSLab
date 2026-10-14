import chisel3._

// 任意進数カウンタ (ヘルパーモジュール)
class ModuloNCounter(val n: Int, val width: Int) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val load     = Input(Bool())
    val loadData = Input(UInt(width.W))
    val count    = Output(UInt(width.W))
    val rollover = Output(Bool())
  })
  val cntReg = RegInit(0.U(width.W))
  when(io.clear) {
    cntReg := 0.U
  }.elsewhen(io.load) {
    cntReg := Mux(io.loadData < n.U, io.loadData, 0.U)
  }.elsewhen(io.en) {
    when(cntReg === (n - 1).U) { cntReg := 0.U }.otherwise { cntReg := cntReg + 1.U }
  }
  io.count    := cntReg
  io.rollover := io.en && (cntReg === (n - 1).U)
}

// 発展問題: 24時間デジタル時計コア
class DigitalClockCore extends Module {
  val io = IO(new Bundle {
    val enSec = Input(Bool()) // 1秒パルス
    val clear = Input(Bool())
    val sec   = Output(UInt(6.W)) // 0〜59
    val min   = Output(UInt(6.W)) // 0〜59
    val hour  = Output(UInt(5.W)) // 0〜23
  })

  // TODO: 秒(60進) -> 分(60進) -> 時(24進) を同期カスケードで結合してください
  io.sec  := 0.U
  io.min  := 0.U
  io.hour := 0.U
}

object DigitalClockCoreGenerator extends App {
  emitVerilog(new DigitalClockCore, Array("--target-dir", "generated"))
}
