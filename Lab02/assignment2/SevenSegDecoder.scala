import chisel3._
import chisel3.util._

// 基本問題2: 7セグメントLEDデコーダ
class SevenSegDecoder extends Module {
  val io = IO(new Bundle {
    val bcd = Input(UInt(4.W))
    val seg = Output(UInt(7.W)) // bit[6]=a, bit[5]=b, bit[4]=c, bit[3]=d, bit[2]=e, bit[1]=f, bit[0]=g
  })

  // TODO: switch / is と WireDefault を用いて透過ラッチのないデコーダを実装してください
  // 未定義値 (10〜15) は全消灯 (0.U(7.W)) としてください
  //
  // 7セグメント対応 (点灯=1, 消灯=0):
  // 0: "b1111110".U (abcdef)
  // 1: "b0110000".U (bc)
  // 2: "b1101101".U (abdeg)
  // 3: "b1111001".U (abcdg)
  // 4: "b0110011".U (bcfg)
  // 5: "b1011011".U (acdfg)
  // 6: "b1011111".U (acdefg)
  // 7: "b1110000".U (abc)
  // 8: "b1111111".U (abcdefg)
  // 9: "b1111011".U (abcdfg)
  io.seg := 0.U
}

object SevenSegDecoderGenerator extends App {
  val verilog = _root_.circt.stage.ChiselStage.emitSystemVerilog(
    new SevenSegDecoder,
    firtoolOpts = Array("--lowering-options=noAlwaysComb,disallowPackedArrays,disallowLocalVariables")
  )
  val output = Paths.get("generated", "SevenSegDecoder.v")
  Files.createDirectories(output.getParent)
  Files.write(output, verilog.getBytes(StandardCharsets.UTF_8))
}