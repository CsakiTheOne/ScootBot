package extra

import java.util.*
import kotlin.collections.HashMap

class Brainfuck {
    companion object {
        fun run(program: String): String {
            var output = ""

            val jumps = matchJumps(program) ?: return ""
            val mem = MutableList(30000, init = { 0 })
            var pc = 0
            var cursor = 0

            while (pc < program.length) {
                when (program[pc]) {
                    '+' -> mem[cursor] = (mem[cursor] + 1) % 255
                    '-' -> mem[cursor] = (mem[cursor] - 1) % 255
                    '>' -> cursor++
                    '<' -> cursor--
                    '[' -> {
                        if (mem[cursor] == 0) {
                            pc = jumps[pc] as Int
                        }
                    }
                    ']' -> {
                        pc = jumps[pc] as Int
                        pc--
                    }
                    '.' -> output += "${mem[cursor].toChar()}"
                    ',' -> mem[cursor] = System.`in`.read()
                }

                pc++
            }
            return output
        }

        private fun matchJumps(program: String): Map<Int, Int>? {
            val stack = Stack<Int>()
            val jumpsMap = HashMap<Int, Int>()
            for ((i, c) in program.withIndex()) {
                when (c) {
                    '[' -> stack.push(i)
                    ']' -> {
                        try {
                            val other = stack.pop()
                            jumpsMap.put(other, i)
                            jumpsMap.put(i, other)
                        } catch (e: EmptyStackException) {
                            println("Program has unmatched brackets.")
                            return null
                        }
                    }
                }
            }

            if (!stack.isEmpty()) {
                println("Program has unmatched brackets.")
                return null
            }

            return jumpsMap
        }
    }
}