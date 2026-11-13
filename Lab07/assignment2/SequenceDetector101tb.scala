import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class SequenceDetector101tb extends AnyFlatSpec with ChiselScalatestTester {
  it should "detect overlapping 101 sequences with one-hot state output and Moore detected flag" in {
    test(new SequenceDetector101).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.clear.poke(false.B)
      dut.io.en.poke(true.B)

      // 初期状態 sIDLE ("b0001") の確認
      dut.io.out.expect("b0001".U(4.W))
      dut.io.detected.expect(false.B)

      // 入力系列: 1 -> 0 -> 1 -> 0 -> 1 -> 0
      val inputs      = Seq(true,    false,   true,    false,   true,    false)
      val expectedOut = Seq("b0010", "b0100", "b1000", "b0100", "b1000", "b0100")
      val expectedDet = Seq(false,   false,   true,    false,   true,    false)

      for (i <- inputs.indices) {
        dut.io.in.poke(inputs(i).B)
        dut.clock.step(1)
        val outBin = dut.io.out.peekInt().toInt.toBinaryString
        val det    = dut.io.detected.peekBoolean()
        println(f"step $i: in=${inputs(i)}%5s => out=$outBin%4s (exp: ${expectedOut(i)}), detected=$det (exp: ${expectedDet(i)})")
        dut.io.out.expect(expectedOut(i).U(4.W))
        dut.io.detected.expect(expectedDet(i).B)
      }

      // clear で初期状態 sIDLE ("b0001") に戻ることを確認
      dut.io.clear.poke(true.B)
      dut.clock.step(1)
      dut.io.out.expect("b0001".U(4.W))
      dut.io.detected.expect(false.B)
    }
  }
}
