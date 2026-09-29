# Computer Systems Laboratory
Computer System Laboratory

## Prerequisites

### Java Development Kit (JDK)

Scala runs on the Java Virtual Machine (JVM), so a Java Development Kit (JDK) is required to use Chisel. Note that Chisel requires Java 17 or later.

#### Install OpenJDK

```bash
brew install openjdk
```

#### Set the PATH

```bash
echo 'export PATH="/opt/homebrew/opt/openjdk/bin:$PATH"' >> ~/.zshrc
echo 'export CPPFLAGS="-I/opt/homebrew/opt/openjdk/include"' >> ~/.zshrc
```

### Build Tool: sbt

```bash
brew install sbt
```

### Surfer

```bash
brew install surfer
```

### Verilator

```bash
brew install verilator
```

## Optional Tools

### Local LLM

#### Ollama

```bash
brew install ollama
```

- Install the Ollama VS Code Extension.
- Press `Cmd + Shift + P` and select **Chat: Manage Language Models**.
- Create an `AGENTS.md` file in your project folder.

## Execution and Test
### Generate Verilog HDL
```
sbt run
```

### Run Tests
```
sbt test
```

### View Simulation Results as Waveforms
```
surfer &
```
Open your .vcd file in surfer.
Drag the desired module name and drop it into the simulation window.
Note: You can locate .vcd files using the following command:
```
find . -name "*.vcd"
```

## References

### Development Tools

1. **Chisel Official Website**  
   https://www.chisel-lang.org/

2. **sbt**  
   https://www.scala-sbt.org/

3. **Surfer**  
   https://surfer-project.org/

4. **Verilator**  
   https://www.veripool.org/verilator/

5. **Ollama**  
   https://ollama.com/

### Learning Resources

6. **Digital Design with Chisel**  
   https://www.imm.dtu.dk/~masca/chisel-book.pdf

7. **Digital Design with Chisel (Japanese Edition)**  
   https://www.imm.dtu.dk/~masca/chisel_book_jp.pdf

8. **Chisel Cheatsheet**
https://github.com/freechipsproject/chisel-cheatsheet/releases/latest/download/chisel_cheatsheet.pdf

9. **An Educational Open-Source CPU Implemented with RISC-V and Chisel**  
   https://github.com/chadyuu/riscv-chisel-book

10. **Construct a Single-Cycle RISC-V CPU with Chisel**  
    https://hackmd.io/@sysprog/r1mlr3I7p