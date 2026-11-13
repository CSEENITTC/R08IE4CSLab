import chisel3._
import chisel3.util._

// 課題3: 歩行者割り込み付き 交通信号機コントローラ
class TrafficLightController extends Module {
  val io = IO(new Bundle {
    val en               = Input(Bool())     // イネーブル信号 (OneHzGeneratorからの1秒パルス)
    val clear            = Input(Bool())     // 同期クリア (初期状態 sVehGreen へ復帰)
    val pedestrianButton = Input(Bool())     // 歩行者ボタン入力
    val mainLight        = Output(UInt(2.W)) // 0: Green, 1: Yellow, 2: Red
    val pedLight         = Output(Bool())    // 0: Red, 1: Green
  })

  object LightState extends ChiselEnum {
    val sVehGreen, sVehYellow, sVehRedPedGreen, sVehRedPedYellow = Value
  }

  // TODO:
  // 1. stateReg (初期値 LightState.sVehGreen)、timerReg (4ビット、初期値 0.U)、pedReqReg (初期値 false.B) を生成
  // 2. io.clear のとき初期状態に復帰、それ以外は io.pedestrianButton で pedReqReg をラッチし、
  //    io.en (1秒パルス) のとき timerReg と状態遷移 (sVehGreen -> sVehYellow -> sVehRedPedGreen -> sVehRedPedYellow -> sVehGreen) を更新
  // 3. Moore型出力として io.mainLight と io.pedLight を駆動
  io.mainLight := 0.U
  io.pedLight  := false.B
}

// object VerilogGenerator extends App {
//   _root_.circt.stage.ChiselStage.emitSystemVerilogFile(
//     new TrafficLightController,
//     Array("--target-dir", "generated"),
//     Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
//   )
// }
