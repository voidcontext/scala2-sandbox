package com.gaborpihaj

import org.jline.terminal.TerminalBuilder
import org.jline.keymap.BindingReader
import java.io.PrintWriter
import scala.collection.mutable
// import org.jline.reader.LineReaderBuilder
// import org.jline.reader.impl.completer.StringsCompleter
// import org.jline.reader.impl.DefaultParser
// import org.jline.builtins.Widgets.AutosuggestionWidgets

object TerminalControl {
  def up(n: Int = 1) = esc(s"${n}A")

  def down(n: Int = 1) = esc(s"${n}B")

  def forward(n: Int = 1) = esc(s"${n}C")

  def back(n: Int = 1) = esc(s"${n}D")

  def clearScreen() = esc("2J")

  def clearLine() = esc("0K")

  def move(row: Int, column: Int) = esc(s"${row};${column}H")

  def esc(s: String) = s"\u001b[$s"
}


object Main extends App {
  val terminal = TerminalBuilder.builder()
    .system(true)
    .jansi(true)
    .build()

  // val completer = new StringsCompleter("foo", "bar", "baz")

  // val reader = LineReaderBuilder.builder()
  //   .terminal(terminal)
  //   .completer(completer)
  //   .build()

  // val autoSuggestion = new AutosuggestionWidgets(reader)
  // autoSuggestion.enable()

  // val prompt = "jline2.prompt > "

  // val str = reader.readLine(prompt)

  // println(str)
  // scala.io.StdIn.readLine()

  // ---------------------

  import TerminalControl._

  println(clearScreen() + move(1, 1))
  println("start...")
  println(move(terminal.getHeight(), 1))
  print("prompt here > ")

  terminal.enterRawMode()
  terminal.echo(false)

  val writer = terminal.writer()

  val reader = new BindingReader(terminal.reader())


  def printList[A](ls: List[A], startLine: Int, max: Int, writer: PrintWriter) = 
    ls.take(max).zipWithIndex.foreach { 
      case (a, index) =>
        writer.print(move(startLine - index, 1)  + clearLine() + s"char: $a")
    }

  def readSequence(s: List[Int], reader: BindingReader): List[Int] = s match {
    case List(27) => readSequence(s :+ reader.readCharacter(), reader)
    case List(27, 91) => readSequence(s :+ reader.readCharacter(), reader)
    case l => l
  }


  var chars: mutable.ListBuffer[Int] = scala.collection.mutable.ListBuffer()
  LazyList.continually(reader.readCharacter()).takeWhile(_ != 13)
    .map(chr => List(chr))
    .map(readSequence(_, reader))
    .map { seq =>
      val cursor = terminal.getCursorPosition(_ => ())
      val row = cursor.getY()
      val col = cursor.getX()
      chars.addAll(seq)
      printList(chars.toList.reverse, row - 2, terminal.getHeight() - 4, writer)
      writer.print(move(row + 1, col + 1));
      seq
    }
    .map {
      case List(27, 91, 68) => writer.print(back())
      case List(27, 91, 67) => writer.print(forward())
      case List(c) if 32 <= c && c <= 126 => writer.print(c.toChar)
      case _ => ()
    }
    .foreach(_ => terminal.flush())

  terminal.close()

  // var ch: Byte = scala.io.StdIn.
  // while(ch !=  13.toByte) {
  //   ch.toInt match {
  //     case 37 => print(back())
  //     case 39 => print(forward())
  //     case c if 32 <= c && c <= 126 => print(c.toChar)
  //   }
  //   ch = scala.io.StdIn.readByte()
  // }


}
