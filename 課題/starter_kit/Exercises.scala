package chisel_course.exercises

import chisel3._
import chisel3.util._

// ============================================================================
// 第1回: 組合せ回路の基礎 (Combinational Logic)
// ============================================================================

// 基本問題1: 1ビット半加算器 (Half Adder)
class HalfAdder extends Module {
  val io = IO(new Bundle {
    val a    = Input(Bool())
    val b    = Input(Bool())
    val sum  = Output(Bool())
    val cout = Output(Bool())
  })

  // TODO: sum と cout の論理式を記述してください
  io.sum  := false.B
  io.cout := false.B
}

// 基本問題2: 1ビット全加算器 (Full Adder)
class FullAdder extends Module {
  val io = IO(new Bundle {
    val a    = Input(Bool())
    val b    = Input(Bool())
    val cin  = Input(Bool())
    val sum  = Output(Bool())
    val cout = Output(Bool())
  })

  // TODO: sum と cout の論理式を記述してください
  io.sum  := false.B
  io.cout := false.B
}

// 発展問題: 4ビット加算・パリティ検出器 (Adder with Parity)
class AdderWithParity4 extends Module {
  val io = IO(new Bundle {
    val a      = Input(UInt(4.W))
    val b      = Input(UInt(4.W))
    val cin    = Input(Bool())
    val sum    = Output(UInt(4.W))
    val cout   = Output(Bool())
    val parity = Output(Bool()) // sum の奇数パリティ (1の数が奇数ならtrue)
  })

  // TODO: +& 演算子による拡張加算、ビットスライス、縮約XOR (xorR) を用いて実装してください
  // ※ サブモジュールは使用せず、単一モジュール内で直接記述してください
  io.sum    := 0.U
  io.cout   := false.B
  io.parity := false.B
}

// ============================================================================
// 第2回: Muxとデコーダの回路 (Mux & Decoders)
// ============================================================================

// 基本問題: 4-to-1 マルチプレクサ
class Mux4to1 extends Module {
  val io = IO(new Bundle {
    val in0 = Input(UInt(8.W))
    val in1 = Input(UInt(8.W))
    val in2 = Input(UInt(8.W))
    val in3 = Input(UInt(8.W))
    val sel = Input(UInt(2.W))
    val out = Output(UInt(8.W))
  })

  // TODO: sel に応じて対応する入力を選択出力してください
  io.out := 0.U
}

// 基本問題: 7セグメントLEDデコーダ
class SevenSegDecoder extends Module {
  val io = IO(new Bundle {
    val bcd = Input(UInt(4.W))
    val seg = Output(UInt(7.W)) // bit[6]=a, bit[5]=b, ..., bit[0]=g
  })

  // TODO: switch/is と WireDefault を用いて、0〜9のフォントを出力してください（未定義値は0）
  io.seg := 0.U
}

// 発展問題: 4入力優先度付きエンコーダ
class PriorityEncoder4 extends Module {
  val io = IO(new Bundle {
    val in    = Input(UInt(4.W)) // in(3) が最高優先度
    val pos   = Output(UInt(2.W))
    val valid = Output(Bool())
  })

  // TODO: 最も上位で立っているビットの位置(0〜3)と有効フラグを出力してください
  io.pos   := 0.U
  io.valid := false.B
}

// ============================================================================
// 第3回: 順序回路と基本カウンタ (Sequential Logic & Counters)
// ============================================================================

// 基本問題1: 1Hz信号生成・0.5秒LED点滅カウンタ (9MHzクロック対応)
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

  // TODO: clear -> en の優先度でカウント動作と 0.5秒LED点滅、1秒ストローブパルスを実装してください
  io.ledBlink   := ledReg
  io.oneHzPulse := false.B
}

// 基本問題2: 1秒ごとにカウントアップする同期クリア付きモジュロNカウンタ
// 9MHzシステムクロックから1秒パルスを生成し、1秒ごとに 0 〜 n-1 をインクリメント
// FPGA実機に合わせて負論理 (0で点灯) で LED に出力
class ModuloNCounter(val n: Int = 10, val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())         // カウントイネーブル
    val clear    = Input(Bool())         // 同期クリア (最優先で 0 にリセット)
    val count    = Output(UInt(width.W)) // カウント出力 (実機LED向け負論理)
    val runLED   = Output(Bool())        // 動作確認用LED (0.5秒ごとに反転、負論理)
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq

  val modReg   = RegInit(0.U(width.W))
  val cntWidth = log2Ceil(clkFreq)
  val cntReg   = RegInit(0.U(cntWidth.W))
  val ledReg   = RegInit(false.B)
  val modEn    = WireDefault(false.B)

  // TODO: clear -> en の優先度でカウント動作とLED制御を実装してください
  // ※ カウンタ更新論理と出力論理を明確に分離して記述すること
  io.runLED := ~ledReg
  io.count  := ~modReg
}

// 発展問題: プログラマブルPWMパルス発生器
// - io.period: 周期カウンタの上限値 (cntReg は 0 〜 period を巡回するため、1周期は period + 1 クロック)
// - io.duty  : 1周期のうち High (true.B) を出力するクロック数 (cntReg < duty の期間だけ High)
// - io.en    : 動作イネーブル (true.B でカウント＆PWM出力、false.B でカウンタ0リセット＆出力 false.B)
class PwmGenerator(val periodMax: Int = 255) extends Module {
  val io = IO(new Bundle {
    val period = Input(UInt(8.W)) // カウンタ上限値 (1周期 = period + 1 クロック)
    val duty   = Input(UInt(8.W)) // High出力クロック数 (cntReg < duty の間 High)
    val en     = Input(Bool())    // 動作イネーブル
    val pwmOut = Output(Bool())   // PWM出力
  })

  val cntReg = RegInit(0.U(8.W))

  // TODO: カウンタと比較器を組み合わせ、デューティ比に応じたPWMパルスを生成してください
  io.pwmOut := false.B
}

// ============================================================================
// 第4回: RingカウンタとJohnsonカウンタ (Ring & Johnson Counters)
// ============================================================================

// 基本問題: 4ビットRingカウンタ (9MHz分周・1秒イネーブル連携)
class RingCounter(val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en     = Input(Bool())
    val clear  = Input(Bool())
    val runLED = Output(Bool())
    val out    = Output(UInt(width.W))
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth   = log2Ceil(clkFreq)
  val cntReg     = RegInit(0.U(cntWidth.W))
  val state      = RegInit(1.U(width.W))
  val ledReg     = RegInit(false.B)
  val enRingCounter = WireDefault(false.B)

  // TODO: 1秒ストローブ enRingCounter を生成し、下位ビットから上位ビットへ移動・MSBをLSBへ帰還する1-hot循環シフトを実装してください
  io.out    := state
  io.runLED := ~ledReg
}

// 基本問題: 4ビットJohnsonカウンタ (9MHz分周・1秒イネーブル連携)
class JohnsonCounter(val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en     = Input(Bool())
    val clear  = Input(Bool())
    val runLED = Output(Bool())
    val out    = Output(UInt(width.W))
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth   = log2Ceil(clkFreq)
  val cntReg     = RegInit(0.U(cntWidth.W))
  val state      = RegInit(0.U(width.W))
  val ledReg     = RegInit(false.B)
  val enJohnsonCounter = WireDefault(false.B)

  // TODO: 1秒ストローブ enJohnsonCounter を生成し、最上位ビットの反転を最下位ビットへ帰還する2N状態巡回を実装してください
  io.out    := state
  io.runLED := ~ledReg
}

// 発展問題: 4ビット・グレイコード・カウンタ (9MHz分周・1秒イネーブル連携)
class GrayCodeCounter(val width: Int = 4, val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en     = Input(Bool())
    val clear  = Input(Bool())
    val runLED = Output(Bool())
    val gray   = Output(UInt(4.W))
    val bin    = Output(UInt(4.W))
  })

  val halfPeriod = clkFreq / 2
  val fullPeriod = clkFreq
  val cntWidth   = log2Ceil(clkFreq)
  val cntReg     = RegInit(0.U(cntWidth.W))
  val binReg     = RegInit(0.U(width.W))
  val ledReg     = RegInit(false.B)
  val enGrayCodeCounter = WireDefault(false.B)

  // TODO: 1秒ストローブ enGrayCodeCounter で binReg を進め、グレイコード変換論理 (gray = binReg ^ (binReg >> 1)) を実装してください
  io.gray   := 0.U
  io.bin    := ~binReg
  io.runLED := ~ledReg
}

// ============================================================================
// 第5回: 60進カウンタの構造化設計 (Hierarchical 60-Counter)
// ============================================================================

// 基本問題: 1Hz生成モジュール + 10進カウンタ + 6進カウンタによる階層化 60進カウンタ
class Counter60(val clkFreq: Int = 9000000) extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val secUnits = Output(UInt(4.W)) // 1の位 (0〜9)
    val secTens  = Output(UInt(3.W)) // 10の位 (0〜5)
    val runLED   = Output(Bool())    // 0.5秒反転LED出力
    val cout     = Output(Bool())    // 59秒の最後の1クロックでアサート
  })

  // TODO: OneHzGenerator(clkFreq)、10進カウンタ、6進カウンタの3つのサブモジュールをインスタンス化し、
  //       同期イネーブル接続 (oneHzPulse -> 10進en, 10進rollover -> 6進en) で階層化設計してください
  // ※ 非同期クロックの使用は厳禁です
  io.secUnits := 0.U
  io.secTens  := 0.U
  io.cout     := false.B
  io.runLED   := false.B
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

// ============================================================================
// 第6回: 構造化設計とALU (Structured Hierarchical Design & ALU)
// ============================================================================

// 基本問題 サブモジュール1: 算術ユニット
class ArithmeticUnit extends Module {
  val io = IO(new Bundle {
    val a    = Input(UInt(8.W))
    val b    = Input(UInt(8.W))
    val op   = Input(Bool()) // 0: ADD, 1: SUB
    val res  = Output(UInt(8.W))
    val cout = Output(Bool())
  })

  // TODO: 加算・減算を実装してください
  io.res  := 0.U
  io.cout := false.B
}

// 基本問題 サブモジュール2: 論理ユニット
class LogicUnit extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(8.W))
    val b   = Input(UInt(8.W))
    val op  = Input(UInt(2.W)) // 0: AND, 1: OR, 2: XOR, 3: NOT a
    val res = Output(UInt(8.W))
  })

  // TODO: 4種類のビット論理演算を実装してください
  io.res := 0.U
}

// 基本問題 トップモジュール: 構造化ALU
class StructuredALU extends Module {
  val io = IO(new Bundle {
    val a        = Input(UInt(8.W))
    val b        = Input(UInt(8.W))
    val aluSel   = Input(UInt(3.W)) // bit[2]: 0=算術, 1=論理
    val out      = Output(UInt(8.W))
    val carryOut = Output(Bool())
  })

  // TODO: ArithmeticUnit と LogicUnit をインスタンス化し、階層接続してください
  io.out      := 0.U
  io.carryOut := false.B
}

// 発展問題: フラグ生成 & パイプライン化ALU
class PipelinedALUWithFlags extends Module {
  val io = IO(new Bundle {
    val a        = Input(UInt(8.W))
    val b        = Input(UInt(8.W))
    val aluSel   = Input(UInt(3.W))
    val validIn  = Input(Bool())
    val out      = Output(UInt(8.W))
    val zero     = Output(Bool())
    val negative = Output(Bool())
    val carry    = Output(Bool())
    val validOut = Output(Bool())
  })

  // TODO: StructuredALU にパイプラインレジスタと各種ステータスフラグを追加してください
  io.out      := 0.U
  io.zero     := false.B
  io.negative := false.B
  io.carry    := false.B
  io.validOut := false.B
}

// ============================================================================
// 第7回: 有限状態機械 (Finite State Machines: FSM)
// ============================================================================

// 基本問題: Moore型 "101" パターン検出器
class SequenceDetector101 extends Module {
  val io = IO(new Bundle {
    val in       = Input(Bool())
    val detected = Output(Bool())
  })

  // TODO: ChiselEnum を用いて状態を定義し、グリッチフリーなMoore型FSMを実装してください
  io.detected := false.B
}

// 発展問題: 歩行者割り込み付き 交通信号機コントローラ
class TrafficLightController extends Module {
  val io = IO(new Bundle {
    val pedestrianButton = Input(Bool())
    val mainLight        = Output(UInt(2.W)) // 0: Green, 1: Yellow, 2: Red
    val pedLight         = Output(Bool())    // 0: Red, 1: Green
  })

  // TODO: タイマレジスタと割り込みラッチを併用した安全な状態遷移を実装してください
  io.mainLight := 0.U
  io.pedLight  := false.B
}

// ============================================================================
// 第8回: メモリ回路と非同期読み出し (Memory Circuits & Asynchronous Read)
// ============================================================================

// 基本問題: 2R1W 非同期読み出しレジスタファイル (8ワード×8ビット)
class RegisterFile8x8 extends Module {
  val io = IO(new Bundle {
    val raddr1 = Input(UInt(3.W))
    val raddr2 = Input(UInt(3.W))
    val rdata1 = Output(UInt(8.W))
    val rdata2 = Output(UInt(8.W))
    val wen    = Input(Bool())
    val waddr  = Input(UInt(3.W))
    val wdata  = Input(UInt(8.W))
  })

  // TODO: Mem を用いて同期書き込み・非同期読み出し回路を実装してください
  io.rdata1 := 0.U
  io.rdata2 := 0.U
}

// 発展問題: RAWバイパス回路付き レジスタファイル
class BypassedRegisterFile extends Module {
  val io = IO(new Bundle {
    val raddr1 = Input(UInt(3.W))
    val raddr2 = Input(UInt(3.W))
    val rdata1 = Output(UInt(8.W))
    val rdata2 = Output(UInt(8.W))
    val wen    = Input(Bool())
    val waddr  = Input(UInt(3.W))
    val wdata  = Input(UInt(8.W))
  })

  // TODO: 同一サイクル書き込みアドレスと読み出しアドレスの一致を検出し、フォワーディングしてください
  io.rdata1 := 0.U
  io.rdata2 := 0.U
}
