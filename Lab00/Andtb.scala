import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Andtb extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "And"

  it should "correctly implement an And gate for all 4 input patterns" in {
    test(new And).withAnnotations(Seq(WriteVcdAnnotation)) { dut => 
      println("===== And Gate Simulation Start =====")

      // ヘルパーメソッドを用いて各パターンをテスト
      testStep(dut, a = 0, b = 0, expected = 0)
      testStep(dut, a = 0, b = 1, expected = 0)
      testStep(dut, a = 1, b = 0, expected = 0)
      testStep(dut, a = 1, b = 1, expected = 1)

      println("===== All Tests Completed Successfully =====")
    }
  }

  /**
    * 1ステップ分の信号印加・時間前進・期待値検証・ログ表示を行うヘルパーメソッド
    */
  def testStep(dut: And, a: Int, b: Int, expected: Int): Unit = {
    // 1. 入力信号の印加 (poke)
    dut.io.a.poke(a.U(1.W))
    dut.io.b.poke(b.U(1.W))

    // 2. クロックを1ステップ進める (step)
    dut.clock.step(1)

    // 3. 実際の出力値を取得 (peek)
    val actual = dut.io.out.peekInt().toInt

    // 4. 期待値との比較検証 (expect)
    dut.io.out.expect(expected.U(1.W))

    // 5. 結果の整形表示
    val tag = if (actual == expected) "[PASS]" else "[FAIL]"
    println(f"$tag a=$a, b=$b => out=$actual (expected=$expected)")
  }
}
