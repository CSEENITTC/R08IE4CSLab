import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class FourStateFSMtb extends AnyFlatSpec with ChiselScalatestTester {
  "FourStateFSM" should "stay in initial state when en=false, cycle one-hot states when en=true, and reset on clear" in {
    test(new FourStateFSM).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // 1. 初期状態の確認 (en = false.B の間は初期状態 s0 = "b0001" を維持)
      dut.io.clear.poke(false.B)
      dut.io.en.poke(false.B)
      dut.io.out.expect("b0001".U(4.W))
      dut.clock.step(2)
      dut.io.out.expect("b0001".U(4.W))

      // 2. イネーブル (en = true.B) で 4状態をワンホットで巡回し続けることを確認
      dut.io.en.poke(true.B)
      val expectedOneHot = Seq("b0001", "b0010", "b0100", "b1000")
      for (step <- 0 until 8) {
        val exp = expectedOneHot(step % 4)
        println(f"step=$step => out=${dut.io.out.peekInt().toInt.toBinaryString}%4s (expected: $exp)")
        dut.io.out.expect(exp.U(4.W))
        dut.clock.step(1)
      }

      // 3. リセットスイッチ (clear = true.B) を押したら初期状態 ("b0001") に戻ることを確認
      dut.io.clear.poke(true.B)
      dut.clock.step(1)
      dut.io.out.expect("b0001".U(4.W))
    }
  }
}
