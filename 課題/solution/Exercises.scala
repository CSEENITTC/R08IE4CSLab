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

  io.sum  := io.a ^ io.b
  io.cout := io.a & io.b
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

  io.sum  := io.a ^ io.b ^ io.cin
  io.cout := (io.a & io.b) | (io.cin & (io.a ^ io.b))
}

// 発展問題: 4ビット加算・パリティ検出器 (Adder with Parity)
// ※ サブモジュールは使わず、単一モジュール内で拡張加算 (+&) とビットスライス、縮約XOR (xorR) を用いて完結
class AdderWithParity4 extends Module {
  val io = IO(new Bundle {
    val a      = Input(UInt(4.W))
    val b      = Input(UInt(4.W))
    val cin    = Input(Bool())
    val sum    = Output(UInt(4.W))
    val cout   = Output(Bool())
    val parity = Output(Bool()) // sum の奇数パリティ (1の数が奇数ならtrue)
  })

  // +& 演算子により最上位キャリーを失わない 5ビットの加算結果を生成
  val sumExt = io.a +& io.b + io.cin.asUInt
  io.sum    := sumExt(3, 0)
  io.cout   := sumExt(4)
  io.parity := io.sum.xorR
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

  // 2-to-1 MUXの木構造による合成
  val muxLow  = Mux(io.sel(0), io.in1, io.in0)
  val muxHigh = Mux(io.sel(0), io.in3, io.in2)
  io.out := Mux(io.sel(1), muxHigh, muxLow)
}

// 基本問題: 7セグメントLEDデコーダ
class SevenSegDecoder extends Module {
  val io = IO(new Bundle {
    val bcd = Input(UInt(4.W))
    val seg = Output(UInt(7.W)) // bit[6]=a, ..., bit[0]=g
  })

  // WireDefault で透過ラッチの発生を完全防止
  val segPattern = WireDefault(0.U(7.W))

  switch(io.bcd) {
    is(0.U) { segPattern := "b1111110".U } // 0
    is(1.U) { segPattern := "b0110000".U } // 1
    is(2.U) { segPattern := "b1101101".U } // 2
    is(3.U) { segPattern := "b1111001".U } // 3
    is(4.U) { segPattern := "b0110011".U } // 4
    is(5.U) { segPattern := "b1011011".U } // 5
    is(6.U) { segPattern := "b1011111".U } // 6
    is(7.U) { segPattern := "b1110000".U } // 7
    is(8.U) { segPattern := "b1111111".U } // 8
    is(9.U) { segPattern := "b1111011".U } // 9
  }

  io.seg := segPattern
}

// 発展問題: 4入力優先度付きエンコーダ
class PriorityEncoder4 extends Module {
  val io = IO(new Bundle {
    val in    = Input(UInt(4.W)) // in(3) が最高優先度
    val pos   = Output(UInt(2.W))
    val valid = Output(Bool())
  })

  io.valid := io.in.orR

  val posWire = WireDefault(0.U(2.W))
  when(io.in(3)) {
    posWire := 3.U
  }.elsewhen(io.in(2)) {
    posWire := 2.U
  }.elsewhen(io.in(1)) {
    posWire := 1.U
  }.otherwise {
    posWire := 0.U
  }

  io.pos := posWire
}

// ============================================================================
// 第3回: 順序回路と基本カウンタ (Sequential Logic & Counters)
// ============================================================================

// 基本問題: ロード・イネーブル・同期クリア付き 任意進数カウンタ
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
    when(cntReg === (n - 1).U) {
      cntReg := 0.U
    }.otherwise {
      cntReg := cntReg + 1.U
    }
  }

  io.count    := cntReg
  io.rollover := io.en && (cntReg === (n - 1).U)
}

// 発展問題: プログラマブルPWMパルス発生器
class PwmGenerator(val periodMax: Int = 255) extends Module {
  val io = IO(new Bundle {
    val period = Input(UInt(8.W))
    val duty   = Input(UInt(8.W))
    val en     = Input(Bool())
    val pwmOut = Output(Bool())
  })

  val cntReg = RegInit(0.U(8.W))

  when(io.en) {
    when(cntReg >= io.period || cntReg >= periodMax.U) {
      cntReg := 0.U
    }.otherwise {
      cntReg := cntReg + 1.U
    }
  }.otherwise {
    cntReg := 0.U
  }

  io.pwmOut := io.en && (cntReg < io.duty) && (io.duty > 0.U)
}

// ============================================================================
// 第4回: RingカウンタとJohnsonカウンタ (Ring & Johnson Counters)
// ============================================================================

// 基本問題: 自己復帰回路付き 4ビットRingカウンタ
class SelfCorrectingRingCounter(val width: Int = 4) extends Module {
  val io = IO(new Bundle {
    val en  = Input(Bool())
    val out = Output(UInt(width.W))
  })

  val state = RegInit(1.U(width.W))

  when(io.en) {
    // 正常な1-hot状態か判定 (PopCount === 1)
    val isOneHot = PopCount(state) === 1.U
    when(!isOneHot) {
      // 不正状態からの自己自動復帰
      state := 1.U(width.W)
    }.otherwise {
      // 左循環シフト
      state := Cat(state(width - 2, 0), state(width - 1))
    }
  }

  io.out := state
}

// 基本問題: 4ビットJohnsonカウンタ
class JohnsonCounter(val width: Int = 4) extends Module {
  val io = IO(new Bundle {
    val en  = Input(Bool())
    val out = Output(UInt(width.W))
  })

  val state = RegInit(0.U(width.W))

  when(io.en) {
    // 最上位ビットの反転を下位へ結合
    state := Cat(state(width - 2, 0), ~state(width - 1))
  }

  io.out := state
}

// 発展問題: 4ビット・グレイコード・カウンタ
class GrayCodeCounter extends Module {
  val io = IO(new Bundle {
    val en    = Input(Bool())
    val clear = Input(Bool())
    val gray  = Output(UInt(4.W))
    val bin   = Output(UInt(4.W))
  })

  val binReg = RegInit(0.U(4.W))

  when(io.clear) {
    binReg := 0.U
  }.elsewhen(io.en) {
    binReg := binReg + 1.U
  }

  io.bin  := binReg
  // バイナリからグレイコードへの変換: G = B ^ (B >> 1)
  io.gray := binReg ^ (binReg >> 1)
}

// ============================================================================
// 第5回: 構造化設計 (Structured Hierarchical Design)
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

  val sumExtended = Wire(UInt(9.W))
  when(!io.op) {
    sumExtended := io.a +& io.b
  }.otherwise {
    sumExtended := io.a -& io.b
  }

  io.res  := sumExtended(7, 0)
  io.cout := sumExtended(8)
}

// 基本問題 サブモジュール2: 論理ユニット
class LogicUnit extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(8.W))
    val b   = Input(UInt(8.W))
    val op  = Input(UInt(2.W)) // 0: AND, 1: OR, 2: XOR, 3: NOT a
    val res = Output(UInt(8.W))
  })

  val outWire = WireDefault(0.U(8.W))
  switch(io.op) {
    is(0.U) { outWire := io.a & io.b }
    is(1.U) { outWire := io.a | io.b }
    is(2.U) { outWire := io.a ^ io.b }
    is(3.U) { outWire := ~io.a }
  }
  io.res := outWire
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

  val arith = Module(new ArithmeticUnit)
  val logic = Module(new LogicUnit)

  arith.io.a  := io.a
  arith.io.b  := io.b
  arith.io.op := io.aluSel(0)

  logic.io.a  := io.a
  logic.io.b  := io.b
  logic.io.op := io.aluSel(1, 0)

  // bit[2] によるセレクタ
  when(io.aluSel(2) === 0.U) {
    io.out      := arith.io.res
    io.carryOut := arith.io.cout
  }.otherwise {
    io.out      := logic.io.res
    io.carryOut := false.B
  }
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

  val alu = Module(new StructuredALU)
  alu.io.a      := io.a
  alu.io.b      := io.b
  alu.io.aluSel := io.aluSel

  // パイプライン段 (1サイクル同期遅延)
  val outReg   = RegNext(alu.io.out, 0.U)
  val carryReg = RegNext(alu.io.carryOut, false.B)
  val validReg = RegNext(io.validIn, false.B)

  io.out      := outReg
  io.carry    := carryReg
  io.zero     := outReg === 0.U
  io.negative := outReg(7) === 1.B
  io.validOut := validReg
}

// ============================================================================
// 第6回: 60進カウンタの構造化設計 (Hierarchical 60-Counter)
// ============================================================================

// 基本問題: 同期カスケード 60進カウンタ
class Counter60 extends Module {
  val io = IO(new Bundle {
    val en       = Input(Bool())
    val clear    = Input(Bool())
    val secUnits = Output(UInt(4.W)) // 1の位 (0〜9)
    val secTens  = Output(UInt(3.W)) // 10の位 (0〜5)
    val cout     = Output(Bool())    // 59でアサート
  })

  val u10 = Module(new ModuloNCounter(10, 4))
  val u6  = Module(new ModuloNCounter(6, 3))

  u10.io.clear    := io.clear
  u10.io.load     := false.B
  u10.io.loadData := 0.U
  u10.io.en       := io.en

  // 同期イネーブル接続 (cout -> en): リップルクロックは完全排除
  u6.io.clear    := io.clear
  u6.io.load     := false.B
  u6.io.loadData := 0.U
  u6.io.en       := io.en && u10.io.rollover

  io.secUnits := u10.io.count
  io.secTens  := u6.io.count
  io.cout     := io.en && u10.io.rollover && u6.io.rollover
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

  val secCounter = Module(new ModuloNCounter(60, 6))
  val minCounter = Module(new ModuloNCounter(60, 6))
  val hrsCounter = Module(new ModuloNCounter(24, 5))

  secCounter.io.clear    := io.clear
  secCounter.io.load     := false.B
  secCounter.io.loadData := 0.U
  secCounter.io.en       := io.enSec

  minCounter.io.clear    := io.clear
  minCounter.io.load     := false.B
  minCounter.io.loadData := 0.U
  minCounter.io.en       := io.enSec && secCounter.io.rollover

  hrsCounter.io.clear    := io.clear
  hrsCounter.io.load     := false.B
  hrsCounter.io.loadData := 0.U
  hrsCounter.io.en       := io.enSec && secCounter.io.rollover && minCounter.io.rollover

  io.sec  := secCounter.io.count
  io.min  := minCounter.io.count
  io.hour := hrsCounter.io.count
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

  object State extends ChiselEnum {
    val sIDLE, s1, s10, s101 = Value
  }

  val stateReg = RegInit(State.sIDLE)

  // 次状態遷移論理
  switch(stateReg) {
    is(State.sIDLE) {
      when(io.in) { stateReg := State.s1 }
    }
    is(State.s1) {
      when(!io.in) { stateReg := State.s10 }
    }
    is(State.s10) {
      when(io.in) { stateReg := State.s101 }.otherwise { stateReg := State.sIDLE }
    }
    is(State.s101) {
      // 101 の後 1 が来たら直前の 1 を使って s1 へ、0 なら s10 へ
      when(io.in) { stateReg := State.s1 }.otherwise { stateReg := State.s10 }
    }
  }

  // Moore型出力論理 (状態レジスタのみから生成)
  io.detected := stateReg === State.s101
}

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

  val stateReg   = RegInit(LightState.sVehGreen)
  val timerReg   = RegInit(0.U(4.W))
  val pedReqReg  = RegInit(false.B)

  // ボタン押下のラッチ
  when(io.pedestrianButton) {
    pedReqReg := true.B
  }

  timerReg := timerReg + 1.U

  switch(stateReg) {
    is(LightState.sVehGreen) {
      when(pedReqReg && timerReg >= 10.U) {
        stateReg := LightState.sVehYellow
        timerReg := 0.U
      }
    }
    is(LightState.sVehYellow) {
      when(timerReg >= 3.U) {
        stateReg  := LightState.sVehRedPedGreen
        timerReg  := 0.U
        pedReqReg := false.B // リクエスト消化
      }
    }
    is(LightState.sVehRedPedGreen) {
      when(timerReg >= 8.U) {
        stateReg := LightState.sVehRedPedYellow
        timerReg := 0.U
      }
    }
    is(LightState.sVehRedPedYellow) {
      when(timerReg >= 3.U) {
        stateReg := LightState.sVehGreen
        timerReg := 0.U
      }
    }
  }

  // デコード出力 (Moore型)
  val mLight = WireDefault(0.U(2.W))
  val pLight = WireDefault(false.B)

  switch(stateReg) {
    is(LightState.sVehGreen) {
      mLight := 0.U; pLight := false.B
    }
    is(LightState.sVehYellow) {
      mLight := 1.U; pLight := false.B
    }
    is(LightState.sVehRedPedGreen) {
      mLight := 2.U; pLight := true.B
    }
    is(LightState.sVehRedPedYellow) {
      mLight := 2.U; pLight := false.B
    }
  }

  io.mainLight := mLight
  io.pedLight  := pLight
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

  val mem = Mem(8, UInt(8.W))

  // 同期書き込み
  when(io.wen) {
    mem(io.waddr) := io.wdata
  }

  // 非同期読み出し (組み合わせ回路としての即時アクセス)
  io.rdata1 := mem(io.raddr1)
  io.rdata2 := mem(io.raddr2)
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

  val rf = Module(new RegisterFile8x8)
  rf.io.raddr1 := io.raddr1
  rf.io.raddr2 := io.raddr2
  rf.io.wen    := io.wen
  rf.io.waddr  := io.waddr
  rf.io.wdata  := io.wdata

  // RAW競合の検出と同一サイクルフォワーディング
  val bypass1 = io.wen && (io.waddr === io.raddr1)
  val bypass2 = io.wen && (io.waddr === io.raddr2)

  io.rdata1 := Mux(bypass1, io.wdata, rf.io.rdata1)
  io.rdata2 := Mux(bypass2, io.wdata, rf.io.rdata2)
}
