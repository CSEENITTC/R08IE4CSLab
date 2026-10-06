import chisel3._
import chisel3.util._

// 発展問題: 歩行者割り込み付き 交通信号機コントローラ
class TrafficLightController extends Module {
  val io = IO(new Bundle {
    val pedestrianButton = Input(Bool())
    val mainLight        = Output(UInt(2.W)) // 0: Green, 1: Yellow, 2: Red
    val pedLight         = Output(Bool())    // 0: Red, 1: Green
  })

  object LightState extends ChiselEnum {
    val sVehGreen, sVehYellow, sVehRedPedGreen, sVehRedPedYellow = Value
  }

  // TODO: タイマレジスタと割り込みラッチを併用した安全な状態遷移を実装してください
  // mainLight: 0(Green), 1(Yellow), 2(Red)
  // pedLight: false(Red), true(Green)
  io.mainLight := 0.U
  io.pedLight  := false.B
}

object TrafficLightControllerGenerator extends App {
  emitVerilog(new TrafficLightController, Array("--target-dir", "generated"))
}
